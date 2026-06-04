package com.cityguard.caseinfo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.auth.entity.SysUser;
import com.cityguard.auth.mapper.SysUserMapper;
import com.cityguard.caseinfo.constant.CaseAdjustmentConstant;
import com.cityguard.caseinfo.dto.CaseAdjustmentApplyRequest;
import com.cityguard.caseinfo.dto.CaseAdjustmentReviewRequest;
import com.cityguard.caseinfo.entity.CaseAdjustmentApply;
import com.cityguard.caseinfo.entity.CaseInfo;
import com.cityguard.caseinfo.mapper.CaseAdjustmentApplyMapper;
import com.cityguard.caseinfo.mapper.CaseInfoMapper;
import com.cityguard.caseinfo.service.CaseAdjustmentService;
import com.cityguard.common.constant.CaseStatusConstant;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.common.spi.UserNotificationSender;
import com.cityguard.timer.service.CaseTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseAdjustmentServiceImpl implements CaseAdjustmentService {

    private static final String ROLE_DEPT = "DEPT";
    private static final String ROLE_HANDLER = "HANDLER";
    private static final String ROLE_DISPATCHER = "DISPATCHER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SUPERVISOR = "SUPERVISOR";
    private static final String BIZ_CASE = "case";

    private final CaseAdjustmentApplyMapper adjustmentApplyMapper;
    private final CaseInfoMapper caseInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final CaseTimerService caseTimerService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private UserNotificationSender userNotificationSender;

    @Override
    @Transactional
    public CaseAdjustmentApply apply(CaseAdjustmentApplyRequest request, Long operatorId, String operatorName,
                                     List<String> operatorRoles) {
        if (request.getCaseId() == null) {
            throw new BusinessException("案件ID不能为空");
        }
        String applyType = normalizeType(request.getApplyType());
        assertAdjustmentApplicantRole(operatorRoles);
        String reason = request.getReason() != null ? request.getReason().trim() : "";
        if (reason.isBlank()) {
            throw new BusinessException("请填写申请原因");
        }

        CaseInfo caseInfo = caseInfoMapper.selectById(request.getCaseId());
        if (caseInfo == null || caseInfo.getIsDeleted() != null && caseInfo.getIsDeleted() == 1) {
            throw new BusinessException("案件不存在");
        }
        assertAdjustmentApplicantScope(caseInfo, operatorId, operatorRoles);
        assertApplicableStatus(caseInfo);
        assertNotHandleOverdue(caseInfo);
        assertCaseOperable(caseInfo);

        if (adjustmentApplyMapper.countPending(caseInfo.getId(), applyType) > 0) {
            throw new BusinessException("已有同类型申请在审批中，请等待处理");
        }

        boolean handlerInitiated = isHandlerApplicant(operatorRoles);

        if (CaseAdjustmentConstant.TYPE_EXTENSION.equals(applyType)) {
            int approved = caseInfo.getExtensionApprovedCount() != null ? caseInfo.getExtensionApprovedCount() : 0;
            if (approved >= CaseAdjustmentConstant.MAX_EXTENSION_APPROVED) {
                throw new BusinessException("该案件延期次数已达上限（" + CaseAdjustmentConstant.MAX_EXTENSION_APPROVED + " 次）");
            }
        } else {
            if (adjustmentApplyMapper.countApproved(caseInfo.getId(), CaseAdjustmentConstant.TYPE_SUSPEND) > 0) {
                throw new BusinessException("该案件已批准过挂账，不可再次申请挂账");
            }
        }

        LocalDateTime suspendUntil = null;
        if (CaseAdjustmentConstant.TYPE_SUSPEND.equals(applyType)) {
            if (request.getSuspendUntil() == null) {
                throw new BusinessException("请选择挂账截止日期");
            }
            LocalDate untilDate = request.getSuspendUntil();
            LocalDate today = LocalDate.now();
            LocalDate maxDate = today.plusDays(CaseAdjustmentConstant.MAX_SUSPEND_DAYS);
            if (untilDate.isBefore(today)) {
                throw new BusinessException("挂账截止日期不能早于今天");
            }
            if (untilDate.isAfter(maxDate)) {
                throw new BusinessException("挂账截止日期不能超过今天起 " + CaseAdjustmentConstant.MAX_SUSPEND_DAYS + " 天（1 年）");
            }
            suspendUntil = untilDate.atTime(LocalTime.of(23, 59, 59));
        }

        SysUser operator = sysUserMapper.selectById(operatorId);
        CaseAdjustmentApply apply = new CaseAdjustmentApply();
        apply.setCaseId(caseInfo.getId());
        apply.setCaseCode(caseInfo.getCaseCode());
        apply.setApplyType(applyType);
        apply.setApplyStatus(handlerInitiated
                ? CaseAdjustmentConstant.STATUS_PENDING_DEPT
                : CaseAdjustmentConstant.STATUS_PENDING);
        apply.setReason(reason);
        apply.setSuspendUntil(suspendUntil);
        apply.setOldDeadlineTime(caseInfo.getDeadlineTime());
        apply.setApplicantId(operatorId);
        apply.setApplicantName(operatorName);
        apply.setApplicantDeptId(operator != null ? operator.getDepartmentId() : caseInfo.getHandleDeptId());
        adjustmentApplyMapper.insert(apply);

        String typeLabel = CaseAdjustmentConstant.TYPE_EXTENSION.equals(applyType) ? "延期" : "挂账";
        if (handlerInitiated) {
            saveFlowRecord(caseInfo.getId(), caseInfo.getCaseCode(), caseInfo.getCaseStatus(),
                    "申请" + typeLabel, "处置人员申请" + typeLabel + "（待部门审核）：" + reason, operatorId, operatorName);
            notifyDeptReviewers(caseInfo, typeLabel);
        } else {
            saveFlowRecord(caseInfo.getId(), caseInfo.getCaseCode(), caseInfo.getCaseStatus(),
                    "申请" + typeLabel, "处置部门申请" + typeLabel + "：" + reason, operatorId, operatorName);
            notifyDispatcherReviewer(caseInfo, typeLabel);
        }
        fillLabels(apply, caseInfo);
        return apply;
    }

    @Override
    public Page<CaseAdjustmentApply> listPending(Integer pageNum, Integer pageSize, Long operatorId, List<String> roles) {
        if (!canReview(roles)) {
            throw new BusinessException("无权查看延期/挂账待审列表");
        }
        Page<CaseAdjustmentApply> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CaseAdjustmentApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseAdjustmentApply::getApplyStatus, CaseAdjustmentConstant.STATUS_PENDING)
                .orderByAsc(CaseAdjustmentApply::getCreateTime);
        if (!canReviewAll(roles)) {
            wrapper.apply("""
                    case_id IN (
                        SELECT id FROM case_info
                        WHERE is_deleted = 0 AND dispatch_operator_id = {0}
                    )
                    """, operatorId);
        }
        Page<CaseAdjustmentApply> result = adjustmentApplyMapper.selectPage(page, wrapper);
        for (CaseAdjustmentApply row : result.getRecords()) {
            CaseInfo c = caseInfoMapper.selectById(row.getCaseId());
            fillLabels(row, c);
        }
        return result;
    }

    @Override
    public Page<CaseAdjustmentApply> listPendingDept(Integer pageNum, Integer pageSize, Long operatorId,
                                                     List<String> roles) {
        if (!canDeptReview(roles)) {
            throw new BusinessException("无权查看部门待审延期/挂账");
        }
        Page<CaseAdjustmentApply> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CaseAdjustmentApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseAdjustmentApply::getApplyStatus, CaseAdjustmentConstant.STATUS_PENDING_DEPT)
                .orderByAsc(CaseAdjustmentApply::getCreateTime);
        if (!canReviewAll(roles)) {
            SysUser op = sysUserMapper.selectById(operatorId);
            if (op == null || op.getDepartmentId() == null) {
                throw new BusinessException("当前账号未绑定处置部门");
            }
            wrapper.apply("""
                    case_id IN (
                        SELECT id FROM case_info
                        WHERE is_deleted = 0 AND handle_dept_id = {0}
                    )
                    """, op.getDepartmentId());
        }
        Page<CaseAdjustmentApply> result = adjustmentApplyMapper.selectPage(page, wrapper);
        for (CaseAdjustmentApply row : result.getRecords()) {
            fillLabels(row, caseInfoMapper.selectById(row.getCaseId()));
        }
        return result;
    }

    @Override
    @Transactional
    public CaseAdjustmentApply deptReview(CaseAdjustmentReviewRequest request, Long operatorId, String operatorName,
                                          List<String> roles) {
        if (!canDeptReview(roles)) {
            throw new BusinessException("无权进行部门初审");
        }
        if (request.getApplyId() == null) {
            throw new BusinessException("申请ID不能为空");
        }
        if (request.getApproved() == null) {
            throw new BusinessException("请选择同意报送或驳回");
        }

        CaseAdjustmentApply apply = adjustmentApplyMapper.selectById(request.getApplyId());
        if (apply == null) {
            throw new BusinessException("申请不存在");
        }
        if (!CaseAdjustmentConstant.STATUS_PENDING_DEPT.equals(apply.getApplyStatus())) {
            throw new BusinessException("该申请不在部门待审状态，请刷新后重试");
        }

        CaseInfo caseInfo = caseInfoMapper.selectById(apply.getCaseId());
        if (caseInfo == null) {
            throw new BusinessException("关联案件不存在");
        }
        assertDeptCaseScope(caseInfo, operatorId, roles);

        String remark = request.getReviewRemark() != null ? request.getReviewRemark().trim() : "";
        apply.setDeptReviewerId(operatorId);
        apply.setDeptReviewerName(operatorName);
        apply.setDeptReviewTime(LocalDateTime.now());
        apply.setDeptReviewRemark(remark);

        String typeLabel = CaseAdjustmentConstant.TYPE_EXTENSION.equals(apply.getApplyType()) ? "延期" : "挂账";
        if (Boolean.TRUE.equals(request.getApproved())) {
            apply.setApplyStatus(CaseAdjustmentConstant.STATUS_PENDING);
            saveFlowRecord(caseInfo.getId(), caseInfo.getCaseCode(), caseInfo.getCaseStatus(),
                    "部门同意报送" + typeLabel,
                    "处置部门同意报送" + typeLabel + (remark.isBlank() ? "" : "：" + remark),
                    operatorId, operatorName);
            notifyDispatcherReviewer(caseInfo, typeLabel);
            notifyUser(apply.getApplicantId(), typeLabel + "部门已通过",
                    "案件 " + caseInfo.getCaseCode() + " 的" + typeLabel + "申请部门已同意报送派遣员",
                    caseInfo.getId(), caseInfo.getCaseCode());
        } else {
            if (remark.isBlank()) {
                throw new BusinessException("驳回时请填写意见（如：可调库存，无需延期）");
            }
            apply.setApplyStatus(CaseAdjustmentConstant.STATUS_REJECTED);
            saveFlowRecord(caseInfo.getId(), caseInfo.getCaseCode(), caseInfo.getCaseStatus(),
                    "部门驳回" + typeLabel,
                    "处置部门驳回" + typeLabel + "：" + remark,
                    operatorId, operatorName);
            notifyUser(apply.getApplicantId(), typeLabel + "部门未通过",
                    "案件 " + caseInfo.getCaseCode() + " 的" + typeLabel + "申请部门未通过：" + remark,
                    caseInfo.getId(), caseInfo.getCaseCode());
        }
        adjustmentApplyMapper.updateById(apply);
        fillLabels(apply, caseInfo);
        return apply;
    }

    @Override
    @Transactional
    public CaseAdjustmentApply review(CaseAdjustmentReviewRequest request, Long operatorId, String operatorName,
                                      List<String> roles) {
        if (!canReview(roles)) {
            throw new BusinessException("无权审批延期/挂账");
        }
        if (request.getApplyId() == null) {
            throw new BusinessException("申请ID不能为空");
        }
        if (request.getApproved() == null) {
            throw new BusinessException("请选择批准或驳回");
        }

        CaseAdjustmentApply apply = adjustmentApplyMapper.selectById(request.getApplyId());
        if (apply == null) {
            throw new BusinessException("申请不存在");
        }
        if (!CaseAdjustmentConstant.STATUS_PENDING.equals(apply.getApplyStatus())) {
            throw new BusinessException("该申请不在派遣待审状态，请刷新后重试");
        }

        CaseInfo caseInfo = caseInfoMapper.selectById(apply.getCaseId());
        if (caseInfo == null) {
            throw new BusinessException("关联案件不存在");
        }
        assertReviewScope(caseInfo, operatorId, roles);

        String remark = request.getReviewRemark() != null ? request.getReviewRemark().trim() : "";
        apply.setReviewerId(operatorId);
        apply.setReviewerName(operatorName);
        apply.setReviewTime(LocalDateTime.now());
        apply.setReviewRemark(remark);

        String typeLabel = CaseAdjustmentConstant.TYPE_EXTENSION.equals(apply.getApplyType()) ? "延期" : "挂账";
        if (Boolean.TRUE.equals(request.getApproved())) {
            apply.setApplyStatus(CaseAdjustmentConstant.STATUS_APPROVED);
            if (CaseAdjustmentConstant.TYPE_EXTENSION.equals(apply.getApplyType())) {
                LocalDateTime newDeadline = caseTimerService.extendHandleDeadline(caseInfo.getId());
                apply.setNewDeadlineTime(newDeadline);
                int count = (caseInfo.getExtensionApprovedCount() != null ? caseInfo.getExtensionApprovedCount() : 0) + 1;
                jdbcTemplate.update(
                        "UPDATE case_info SET extension_approved_count = ?, deadline_time = ? WHERE id = ?",
                        count, newDeadline, caseInfo.getId());
            } else {
                caseTimerService.pauseHandleTimer(caseInfo.getId());
                jdbcTemplate.update(
                        """
                        UPDATE case_info SET is_suspended = 1, suspend_until = ? WHERE id = ?
                        """,
                        apply.getSuspendUntil(), caseInfo.getId());
            }
            saveFlowRecord(caseInfo.getId(), caseInfo.getCaseCode(), caseInfo.getCaseStatus(),
                    "批准" + typeLabel,
                    "派遣员批准" + typeLabel + (remark.isBlank() ? "" : "：" + remark),
                    operatorId, operatorName);
        } else {
            if (remark.isBlank()) {
                throw new BusinessException("驳回时请填写审批意见");
            }
            apply.setApplyStatus(CaseAdjustmentConstant.STATUS_REJECTED);
            saveFlowRecord(caseInfo.getId(), caseInfo.getCaseCode(), caseInfo.getCaseStatus(),
                    "驳回" + typeLabel,
                    "派遣员驳回" + typeLabel + "：" + remark,
                    operatorId, operatorName);
        }
        adjustmentApplyMapper.updateById(apply);
        fillLabels(apply, caseInfo);
        return apply;
    }

    @Override
    public List<CaseAdjustmentApply> listByCaseId(Long caseId) {
        LambdaQueryWrapper<CaseAdjustmentApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseAdjustmentApply::getCaseId, caseId)
                .orderByDesc(CaseAdjustmentApply::getCreateTime);
        List<CaseAdjustmentApply> list = adjustmentApplyMapper.selectList(wrapper);
        CaseInfo caseInfo = caseInfoMapper.selectById(caseId);
        for (CaseAdjustmentApply row : list) {
            fillLabels(row, caseInfo);
        }
        return list;
    }

    @Override
    public void enrichCaseDetail(CaseInfo caseInfo) {
        if (caseInfo == null || caseInfo.getId() == null) {
            return;
        }
        caseInfo.setHasPendingExtension(
                adjustmentApplyMapper.countPending(caseInfo.getId(), CaseAdjustmentConstant.TYPE_EXTENSION) > 0);
        caseInfo.setHasPendingSuspend(
                adjustmentApplyMapper.countPending(caseInfo.getId(), CaseAdjustmentConstant.TYPE_SUSPEND) > 0);
        caseInfo.setPendingDeptExtensionApply(
                findApplyByStatus(caseInfo.getId(), CaseAdjustmentConstant.TYPE_EXTENSION,
                        CaseAdjustmentConstant.STATUS_PENDING_DEPT));
        caseInfo.setPendingDeptSuspendApply(
                findApplyByStatus(caseInfo.getId(), CaseAdjustmentConstant.TYPE_SUSPEND,
                        CaseAdjustmentConstant.STATUS_PENDING_DEPT));
        caseInfo.setPendingExtensionApply(
                findApplyByStatus(caseInfo.getId(), CaseAdjustmentConstant.TYPE_EXTENSION,
                        CaseAdjustmentConstant.STATUS_PENDING));
        caseInfo.setPendingSuspendApply(
                findApplyByStatus(caseInfo.getId(), CaseAdjustmentConstant.TYPE_SUSPEND,
                        CaseAdjustmentConstant.STATUS_PENDING));
        caseInfo.setLastRejectedExtensionApply(
                findLastRejectedApply(caseInfo.getId(), CaseAdjustmentConstant.TYPE_EXTENSION));
        caseInfo.setLastRejectedSuspendApply(
                findLastRejectedApply(caseInfo.getId(), CaseAdjustmentConstant.TYPE_SUSPEND));
        caseInfo.setSuspendEverApproved(
                adjustmentApplyMapper.countApproved(caseInfo.getId(), CaseAdjustmentConstant.TYPE_SUSPEND) > 0);
        if (caseInfo.getExtensionApprovedCount() == null) {
            caseInfo.setExtensionApprovedCount(0);
        }
        if (caseInfo.getIsSuspended() == null) {
            caseInfo.setIsSuspended(0);
        }
    }

    @Override
    public void assertCaseOperable(CaseInfo caseInfo) {
        if (caseInfo != null && caseInfo.getIsSuspended() != null && caseInfo.getIsSuspended() == 1) {
            throw new BusinessException("案件挂账中，暂不可操作；挂账恢复后可继续处置");
        }
    }

    @Override
    @Transactional
    public void resumeExpiredSuspensions() {
        List<Long> caseIds = adjustmentApplyMapper.selectCaseIdsDueForSuspendResume();
        for (Long caseId : caseIds) {
            try {
                resumeSingleCase(caseId);
            } catch (Exception e) {
                log.warn("挂账自动恢复失败 caseId={}: {}", caseId, e.getMessage());
            }
        }
    }

    private void resumeSingleCase(Long caseId) {
        CaseInfo caseInfo = caseInfoMapper.selectById(caseId);
        if (caseInfo == null || caseInfo.getIsSuspended() == null || caseInfo.getIsSuspended() != 1) {
            return;
        }
        caseTimerService.resumeHandleTimer(caseId);
        jdbcTemplate.update(
                "UPDATE case_info SET is_suspended = 0, suspend_until = NULL WHERE id = ?",
                caseId);
        saveFlowRecord(caseId, caseInfo.getCaseCode(), caseInfo.getCaseStatus(),
                "挂账到期恢复", "挂账期届满，处置计时已恢复", 1L, "系统");
        log.info("Case {} suspend auto-resumed", caseId);
    }

    private void assertApplicableStatus(CaseInfo caseInfo) {
        String st = caseInfo.getCaseStatus();
        if (!CaseStatusConstant.PENDING_HANDLE.equals(st) && !CaseStatusConstant.HANDLING.equals(st)) {
            throw new BusinessException("仅「待指派」或「处置中」的案件可申请延期/挂账");
        }
    }

    private void assertNotHandleOverdue(CaseInfo caseInfo) {
        if (caseTimerService.isHandleStageOverdue(caseInfo.getId())) {
            throw new BusinessException("案件处置已超时，不可申请延期/挂账");
        }
    }

    private void assertAdjustmentApplicantRole(List<String> roles) {
        if (roles == null) {
            throw new BusinessException("仅处置部门或处置人员可申请延期/挂账");
        }
        if (roles.contains(ROLE_ADMIN) || roles.contains(ROLE_DEPT) || roles.contains(ROLE_HANDLER)) {
            return;
        }
        throw new BusinessException("仅处置部门或处置人员可申请延期/挂账");
    }

    private void assertAdjustmentApplicantScope(CaseInfo caseInfo, Long operatorId, List<String> roles) {
        if (roles != null && roles.contains(ROLE_ADMIN)) {
            return;
        }
        if (roles != null && roles.contains(ROLE_HANDLER)) {
            assertHandlerAssignedForAdjustment(caseInfo, operatorId);
            return;
        }
        if (roles != null && roles.contains(ROLE_DEPT)) {
            assertDeptCaseScope(caseInfo, operatorId, roles);
            return;
        }
        throw new BusinessException("无权申请延期/挂账");
    }

    private void assertHandlerAssignedForAdjustment(CaseInfo caseInfo, Long operatorId) {
        if (!CaseStatusConstant.HANDLING.equals(caseInfo.getCaseStatus())) {
            throw new BusinessException("仅「处置中」且已指派给您时可申请延期/挂账");
        }
        if (caseInfo.getCurrentHandlerId() == null || !caseInfo.getCurrentHandlerId().equals(operatorId)) {
            throw new BusinessException("该案件未指派给您，无法申请延期/挂账");
        }
    }

    private boolean canDeptReview(List<String> roles) {
        return roles != null && (roles.contains(ROLE_DEPT) || roles.contains(ROLE_ADMIN)
                || roles.contains(ROLE_SUPERVISOR));
    }

    private static boolean isHandlerApplicant(List<String> roles) {
        return roles != null && roles.contains(ROLE_HANDLER)
                && !roles.contains(ROLE_DEPT) && !roles.contains(ROLE_ADMIN);
    }

    private void assertDeptCaseScope(CaseInfo caseInfo, Long operatorId, List<String> roles) {
        if (roles != null && roles.contains(ROLE_ADMIN)) {
            return;
        }
        SysUser op = sysUserMapper.selectById(operatorId);
        if (op == null || op.getDepartmentId() == null
                || !op.getDepartmentId().equals(caseInfo.getHandleDeptId())) {
            throw new BusinessException("您无权为其他处置部门的案件申请延期/挂账");
        }
    }

    private void assertReviewScope(CaseInfo caseInfo, Long operatorId, List<String> roles) {
        if (canReviewAll(roles)) {
            return;
        }
        Long dispatchOp = resolveDispatchOperatorId(caseInfo);
        if (dispatchOp == null || !dispatchOp.equals(operatorId)) {
            throw new BusinessException("该案件不属于您派遣，无权审批此申请");
        }
    }

    private boolean canReview(List<String> roles) {
        return roles != null && (roles.contains(ROLE_DISPATCHER) || roles.contains(ROLE_ADMIN)
                || roles.contains(ROLE_SUPERVISOR));
    }

    private boolean canReviewAll(List<String> roles) {
        return roles != null && (roles.contains(ROLE_ADMIN) || roles.contains(ROLE_SUPERVISOR));
    }

    private CaseAdjustmentApply findApplyByStatus(Long caseId, String applyType, String status) {
        LambdaQueryWrapper<CaseAdjustmentApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseAdjustmentApply::getCaseId, caseId)
                .eq(CaseAdjustmentApply::getApplyType, applyType)
                .eq(CaseAdjustmentApply::getApplyStatus, status)
                .orderByDesc(CaseAdjustmentApply::getCreateTime)
                .last("LIMIT 1");
        CaseAdjustmentApply apply = adjustmentApplyMapper.selectOne(wrapper);
        if (apply != null) {
            fillLabels(apply, caseInfoMapper.selectById(caseId));
        }
        return apply;
    }

    private CaseAdjustmentApply findLastRejectedApply(Long caseId, String applyType) {
        LambdaQueryWrapper<CaseAdjustmentApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseAdjustmentApply::getCaseId, caseId)
                .eq(CaseAdjustmentApply::getApplyType, applyType)
                .eq(CaseAdjustmentApply::getApplyStatus, CaseAdjustmentConstant.STATUS_REJECTED)
                .orderByDesc(CaseAdjustmentApply::getCreateTime)
                .last("LIMIT 1");
        CaseAdjustmentApply apply = adjustmentApplyMapper.selectOne(wrapper);
        if (apply != null) {
            fillLabels(apply, caseInfoMapper.selectById(caseId));
        }
        return apply;
    }

    private void notifyDeptReviewers(CaseInfo caseInfo, String typeLabel) {
        if (caseInfo.getHandleDeptId() == null) {
            return;
        }
        List<Long> userIds = jdbcTemplate.queryForList("""
                SELECT DISTINCT u.id FROM sys_user u
                INNER JOIN sys_role_user ru ON u.id = ru.user_id
                INNER JOIN sys_role r ON r.id = ru.role_id AND r.deleted = 0
                WHERE u.deleted = 0 AND (u.status IS NULL OR u.status = 1)
                  AND u.department_id = ? AND r.role_code = 'DEPT'
                """, Long.class, caseInfo.getHandleDeptId());
        for (Long uid : userIds) {
            notifyUser(uid, "待部门审核" + typeLabel,
                    "案件 " + caseInfo.getCaseCode() + " 处置人员申请" + typeLabel + "，请部门初审",
                    caseInfo.getId(), caseInfo.getCaseCode());
        }
    }

    private void notifyDispatcherReviewer(CaseInfo caseInfo, String typeLabel) {
        Long dispatcherId = resolveDispatchOperatorId(caseInfo);
        if (dispatcherId != null) {
            notifyUser(dispatcherId, "待派遣员审批" + typeLabel,
                    "案件 " + caseInfo.getCaseCode() + " 申请" + typeLabel + "，请及时审批",
                    caseInfo.getId(), caseInfo.getCaseCode());
        }
    }

    private Long resolveDispatchOperatorId(CaseInfo caseInfo) {
        if (caseInfo.getDispatchOperatorId() != null) {
            return caseInfo.getDispatchOperatorId();
        }
        List<Long> ids = jdbcTemplate.queryForList(
                """
                SELECT operator_id FROM case_flow_record
                WHERE case_id = ? AND node_name = '派遣至处置部门'
                ORDER BY id DESC LIMIT 1
                """,
                Long.class, caseInfo.getId());
        return ids.isEmpty() ? null : ids.get(0);
    }

    private static String normalizeType(String raw) {
        if (raw == null) {
            throw new BusinessException("申请类型不能为空");
        }
        String t = raw.trim().toLowerCase();
        if (CaseAdjustmentConstant.TYPE_EXTENSION.equals(t) || CaseAdjustmentConstant.TYPE_SUSPEND.equals(t)) {
            return t;
        }
        throw new BusinessException("申请类型无效");
    }

    private void fillLabels(CaseAdjustmentApply apply, CaseInfo caseInfo) {
        if (CaseAdjustmentConstant.TYPE_EXTENSION.equals(apply.getApplyType())) {
            apply.setApplyTypeLabel("延期");
        } else if (CaseAdjustmentConstant.TYPE_SUSPEND.equals(apply.getApplyType())) {
            apply.setApplyTypeLabel("挂账");
        }
        apply.setApplyStatusLabel(switch (apply.getApplyStatus()) {
            case CaseAdjustmentConstant.STATUS_PENDING_DEPT -> "待部门审核";
            case CaseAdjustmentConstant.STATUS_PENDING -> "待派遣员审批";
            case CaseAdjustmentConstant.STATUS_APPROVED -> "已批准";
            case CaseAdjustmentConstant.STATUS_REJECTED -> "已驳回";
            default -> apply.getApplyStatus();
        });
        if (caseInfo != null) {
            apply.setHandleDeptName(caseInfo.getHandleDeptName());
        }
    }

    private void saveFlowRecord(Long caseId, String caseCode, String nodeCode, String nodeName,
                                String opinion, Long operatorId, String operatorName) {
        jdbcTemplate.update(
                """
                INSERT INTO case_flow_record (
                    case_id, case_code, node_code, node_name,
                    operate_type, operate_result, operate_opinion,
                    operator_id, operator_name, operate_time, create_time
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """,
                caseId, caseCode, nodeCode, nodeName,
                "adjustment", "adjustment", opinion != null ? opinion : "",
                operatorId != null ? operatorId : 1L,
                operatorName != null ? operatorName : "系统",
                LocalDateTime.now(), LocalDateTime.now());
    }

    private void notifyUser(Long userId, String title, String content, Long bizId, String bizCode) {
        if (userNotificationSender == null || userId == null) {
            return;
        }
        try {
            userNotificationSender.notifyUser(userId, title, content, BIZ_CASE, bizId, bizCode);
        } catch (Exception e) {
            log.warn("发送提醒失败 userId={}: {}", userId, e.getMessage());
        }
    }
}

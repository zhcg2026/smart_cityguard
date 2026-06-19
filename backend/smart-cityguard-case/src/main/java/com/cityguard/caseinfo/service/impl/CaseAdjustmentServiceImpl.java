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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        apply.setApplicantName(resolveOperatorName(operatorId));
        apply.setApplicantDeptId(operator != null ? operator.getDepartmentId() : caseInfo.getHandleDeptId());
        adjustmentApplyMapper.insert(apply);

        String typeLabel = CaseAdjustmentConstant.TYPE_EXTENSION.equals(applyType) ? "延期" : "挂账";
        String deptName = caseInfo.getHandleDeptName() != null ? caseInfo.getHandleDeptName() : "处置部门";
        if (handlerInitiated) {
            saveFlowRecord(caseInfo.getId(), caseInfo.getCaseCode(), caseInfo.getCaseStatus(),
                    "申请" + typeLabel, "处置人员申请" + typeLabel + "（待部门审核）：" + reason,
                    operatorId, operatorName, null, deptName);
            notifyDeptReviewers(caseInfo, typeLabel);
        } else {
            saveFlowRecord(caseInfo.getId(), caseInfo.getCaseCode(), caseInfo.getCaseStatus(),
                    "申请" + typeLabel, "处置部门申请" + typeLabel + "：" + reason,
                    operatorId, operatorName, null, "派遣员");
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
                          AND case_status = 'handling' AND current_handler_id IS NOT NULL
                    )
                    """, op.getDepartmentId());
        } else {
            wrapper.apply("""
                    case_id IN (
                        SELECT id FROM case_info
                        WHERE is_deleted = 0 AND case_status = 'handling' AND current_handler_id IS NOT NULL
                    )
                    """);
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
        assertHandlerDeptReviewStillValid(caseInfo, apply);

        String remark = request.getReviewRemark() != null ? request.getReviewRemark().trim() : "";
        apply.setDeptReviewerId(operatorId);
        apply.setDeptReviewerName(resolveOperatorName(operatorId));
        apply.setDeptReviewTime(LocalDateTime.now());
        apply.setDeptReviewRemark(remark);

        String typeLabel = CaseAdjustmentConstant.TYPE_EXTENSION.equals(apply.getApplyType()) ? "延期" : "挂账";
        if (Boolean.TRUE.equals(request.getApproved())) {
            apply.setApplyStatus(CaseAdjustmentConstant.STATUS_PENDING);
            saveFlowRecord(caseInfo.getId(), caseInfo.getCaseCode(), caseInfo.getCaseStatus(),
                    "部门同意报送" + typeLabel,
                    "处置部门同意报送" + typeLabel + (remark.isBlank() ? "" : "：" + remark),
                    operatorId, operatorName, null, "派遣员");
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
                    operatorId, operatorName, apply.getApplicantId(), apply.getApplicantName());
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
        apply.setReviewerName(resolveOperatorName(operatorId));
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
                    operatorId, operatorName, apply.getApplicantId(), apply.getApplicantName());
            notifyUser(apply.getApplicantId(), typeLabel + "审批通过",
                    "案件 " + caseInfo.getCaseCode() + " 的" + typeLabel + "申请已由派遣员审批通过",
                    caseInfo.getId(), caseInfo.getCaseCode());
            notifyDeptHandlerUsers(caseInfo, typeLabel, true);
        } else {
            if (remark.isBlank()) {
                throw new BusinessException("驳回时请填写审批意见");
            }
            apply.setApplyStatus(CaseAdjustmentConstant.STATUS_REJECTED);
            saveFlowRecord(caseInfo.getId(), caseInfo.getCaseCode(), caseInfo.getCaseStatus(),
                    "驳回" + typeLabel,
                    "派遣员驳回" + typeLabel + "：" + remark,
                    operatorId, operatorName, apply.getApplicantId(), apply.getApplicantName());
            notifyUser(apply.getApplicantId(), typeLabel + "审批未通过",
                    "案件 " + caseInfo.getCaseCode() + " 的" + typeLabel + "申请已由派遣员驳回：" + remark,
                    caseInfo.getId(), caseInfo.getCaseCode());
            notifyDeptHandlerUsers(caseInfo, typeLabel, false);
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
    @Transactional
    public void voidPendingAdjustmentsOnHandlerUnassign(Long caseId, Long unassignedHandlerId,
                                                        String voidReason, Long operatorId, String operatorName) {
        if (caseId == null || unassignedHandlerId == null) {
            return;
        }
        LambdaQueryWrapper<CaseAdjustmentApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseAdjustmentApply::getCaseId, caseId)
                .in(CaseAdjustmentApply::getApplyStatus,
                        CaseAdjustmentConstant.STATUS_PENDING_DEPT,
                        CaseAdjustmentConstant.STATUS_PENDING);
        List<CaseAdjustmentApply> applies = adjustmentApplyMapper.selectList(wrapper);
        if (applies.isEmpty()) {
            return;
        }

        CaseInfo caseInfo = caseInfoMapper.selectById(caseId);
        if (caseInfo == null) {
            return;
        }

        String remark = voidReason != null && !voidReason.isBlank()
                ? voidReason.trim()
                : "处置人员已解除指派，申请自动作废";
        LocalDateTime now = LocalDateTime.now();
        String opName = resolveOperatorName(operatorId);
        if ("系统".equals(opName) && operatorName != null && !operatorName.isBlank()) {
            opName = operatorName;
        }

        List<String> voidedLabels = new ArrayList<>();
        for (CaseAdjustmentApply apply : applies) {
            if (!shouldVoidOnHandlerUnassign(apply, unassignedHandlerId)) {
                continue;
            }
            String typeLabel = CaseAdjustmentConstant.TYPE_EXTENSION.equals(apply.getApplyType()) ? "延期" : "挂账";
            voidedLabels.add(typeLabel);

            String originalStatus = apply.getApplyStatus();
            apply.setApplyStatus(CaseAdjustmentConstant.STATUS_CANCELLED);
            if (CaseAdjustmentConstant.STATUS_PENDING_DEPT.equals(originalStatus)) {
                apply.setDeptReviewerId(operatorId);
                apply.setDeptReviewerName(opName);
                apply.setDeptReviewTime(now);
                apply.setDeptReviewRemark(remark);
            } else {
                apply.setReviewerId(operatorId);
                apply.setReviewerName(opName);
                apply.setReviewTime(now);
                apply.setReviewRemark(remark);
            }
            adjustmentApplyMapper.updateById(apply);

            if (apply.getApplicantId() != null) {
                notifyUser(apply.getApplicantId(), typeLabel + "申请已作废",
                        "案件 " + caseInfo.getCaseCode() + " 的" + typeLabel
                                + "申请因解除指派已自动作废：" + remark,
                        caseInfo.getId(), caseInfo.getCaseCode());
            }
        }

        if (!voidedLabels.isEmpty()) {
            saveFlowRecord(caseId, caseInfo.getCaseCode(), caseInfo.getCaseStatus(),
                    "作废延期挂账申请",
                    "因解除指派，作废在审" + String.join("、", voidedLabels) + "申请：" + remark,
                    operatorId, opName, unassignedHandlerId, resolveOperatorName(unassignedHandlerId));
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

    private static boolean shouldVoidOnHandlerUnassign(CaseAdjustmentApply apply, Long unassignedHandlerId) {
        if (apply == null || unassignedHandlerId == null) {
            return false;
        }
        if (CaseAdjustmentConstant.STATUS_PENDING_DEPT.equals(apply.getApplyStatus())) {
            return Objects.equals(apply.getApplicantId(), unassignedHandlerId);
        }
        if (CaseAdjustmentConstant.STATUS_PENDING.equals(apply.getApplyStatus())) {
            return Objects.equals(apply.getApplicantId(), unassignedHandlerId);
        }
        return false;
    }

    private void assertHandlerDeptReviewStillValid(CaseInfo caseInfo, CaseAdjustmentApply apply) {
        if (!CaseAdjustmentConstant.STATUS_PENDING_DEPT.equals(apply.getApplyStatus())) {
            return;
        }
        if (!CaseStatusConstant.HANDLING.equals(caseInfo.getCaseStatus())) {
            throw new BusinessException("案件已非处置中，该申请已失效，请刷新页面");
        }
        if (caseInfo.getCurrentHandlerId() == null) {
            throw new BusinessException("案件已解除指派，该申请已失效，请刷新页面");
        }
        if (apply.getApplicantId() != null
                && !Objects.equals(apply.getApplicantId(), caseInfo.getCurrentHandlerId())) {
            throw new BusinessException("处置人员已变更，该申请已失效，请刷新页面");
        }
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

    private void notifyDeptHandlerUsers(CaseInfo caseInfo, String typeLabel, boolean approved) {
        if (caseInfo.getHandleDeptId() == null) {
            return;
        }
        String title = approved ? typeLabel + "审批通过" : typeLabel + "审批未通过";
        String content = approved
                ? "案件 " + caseInfo.getCaseCode() + " 的" + typeLabel + "申请已由派遣员审批通过"
                : "案件 " + caseInfo.getCaseCode() + " 的" + typeLabel + "申请已由派遣员驳回";
        List<Long> userIds = jdbcTemplate.queryForList("""
                SELECT DISTINCT u.id FROM sys_user u
                INNER JOIN sys_role_user ru ON u.id = ru.user_id
                INNER JOIN sys_role r ON r.id = ru.role_id AND r.deleted = 0
                WHERE u.deleted = 0 AND (u.status IS NULL OR u.status = 1)
                  AND u.department_id = ? AND r.role_code IN ('HANDLER','DEPT')
                """, Long.class, caseInfo.getHandleDeptId());
        log.debug("notifyDeptHandlerUsers: deptId={}, handlerUserIds={}", caseInfo.getHandleDeptId(), userIds);
        for (Long uid : userIds) {
            notifyUser(uid, title, content, caseInfo.getId(), caseInfo.getCaseCode());
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
            case CaseAdjustmentConstant.STATUS_CANCELLED -> "已作废";
            default -> apply.getApplyStatus();
        });
        if (caseInfo != null) {
            apply.setHandleDeptName(caseInfo.getHandleDeptName());
        }
    }

    private void saveFlowRecord(Long caseId, String caseCode, String nodeCode, String nodeName,
                                String opinion, Long operatorId, String operatorName) {
        saveFlowRecord(caseId, caseCode, nodeCode, nodeName, opinion, operatorId, operatorName, null, null);
    }

    private String resolveOperatorName(Long operatorId) {
        if (operatorId == null) {
            return "系统";
        }
        SysUser user = sysUserMapper.selectById(operatorId);
        if (user == null) {
            return "系统";
        }
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        return user.getUsername();
    }

    private void saveFlowRecord(Long caseId, String caseCode, String nodeCode, String nodeName,
                                String opinion, Long operatorId, String operatorName,
                                Long receiverId, String receiverName) {
        String opName = resolveOperatorName(operatorId);
        if ("系统".equals(opName) && operatorName != null && !operatorName.isBlank()) {
            opName = operatorName;
        }
        jdbcTemplate.update(
                """
                INSERT INTO case_flow_record (
                    case_id, case_code, node_code, node_name,
                    operate_type, operate_result, operate_opinion,
                    operator_id, operator_name,
                    receiver_id, receiver_name,
                    operate_time, create_time
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                caseId, caseCode, nodeCode, nodeName,
                "adjustment", "adjustment", opinion != null ? opinion : "",
                operatorId != null ? operatorId : 1L,
                opName,
                receiverId,
                receiverName,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private void notifyUser(Long userId, String title, String content, Long bizId, String bizCode) {
        if (userNotificationSender == null || userId == null) {
            log.debug("通知跳过: sender={}, userId={}", userNotificationSender != null, userId);
            return;
        }
        try {
            userNotificationSender.notifyUser(userId, title, content, BIZ_CASE, bizId, bizCode);
            log.debug("通知发送成功: userId={}, title={}", userId, title);
        } catch (Exception e) {
            log.warn("发送提醒失败 userId={}: {}", userId, e.getMessage());
        }
    }
}

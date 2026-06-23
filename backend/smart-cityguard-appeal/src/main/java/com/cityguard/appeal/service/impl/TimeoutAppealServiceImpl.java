package com.cityguard.appeal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.appeal.constant.TimeoutAppealConstant;
import com.cityguard.appeal.dto.TimeoutAppealDetailVo;
import com.cityguard.appeal.dto.TimeoutAppealReviewRequest;
import com.cityguard.appeal.dto.TimeoutAppealSubmitRequest;
import com.cityguard.appeal.entity.AppealApply;
import com.cityguard.appeal.entity.AppealAttachment;
import com.cityguard.appeal.entity.AppealReview;
import com.cityguard.appeal.mapper.AppealApplyMapper;
import com.cityguard.appeal.mapper.AppealAttachmentMapper;
import com.cityguard.appeal.mapper.AppealReviewMapper;
import com.cityguard.appeal.service.TimeoutAppealService;
import com.cityguard.auth.entity.SysDepartment;
import com.cityguard.auth.entity.SysUser;
import com.cityguard.auth.mapper.SysDepartmentMapper;
import com.cityguard.auth.mapper.SysUserMapper;
import com.cityguard.caseinfo.entity.CaseInfo;
import com.cityguard.caseinfo.mapper.CaseInfoMapper;
import com.cityguard.common.constant.CaseStatusConstant;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.auth.entity.LoginUser;
import com.cityguard.common.spi.UserNotificationSender;
import com.cityguard.timer.constant.TimerStageConstant;
import com.cityguard.timer.entity.CaseTimerRecord;
import com.cityguard.timer.mapper.CaseTimerRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeoutAppealServiceImpl implements TimeoutAppealService {

    private static final String BIZ_APPEAL = "appeal";

    private static final String ROLE_DEPT = "DEPT";
    private static final String ROLE_HANDLER = "HANDLER";
    private static final String ROLE_DISPATCHER = "DISPATCHER";
    private static final String ROLE_ACCEPTOR = "ACCEPTOR";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SUPERVISOR = "SUPERVISOR";

    private static final Set<String> CLOSED_STATUSES = Set.of(
            CaseStatusConstant.CLOSED, CaseStatusConstant.FORCED_CLOSE);

    private final AppealApplyMapper appealApplyMapper;
    private final AppealReviewMapper appealReviewMapper;
    private final AppealAttachmentMapper appealAttachmentMapper;
    private final CaseInfoMapper caseInfoMapper;
    private final CaseTimerRecordMapper caseTimerRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDepartmentMapper sysDepartmentMapper;
    private final JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private UserNotificationSender userNotificationSender;

    @Override
    @Transactional
    public AppealApply submit(TimeoutAppealSubmitRequest request, LoginUser user) {
        assertRole(user, ROLE_DEPT, ROLE_HANDLER);
        if (request == null || request.getCaseId() == null) {
            throw new BusinessException("请选择案件");
        }
        if (!StringUtils.hasText(request.getAppealDesc())) {
            throw new BusinessException("请填写申诉说明");
        }
        Long deptId = requireUserDeptId(user);
        CaseInfo caseInfo = requireCase(request.getCaseId());
        assertDeptCase(caseInfo, deptId);
        assertClosed(caseInfo);
        assertHandleTimedOut(caseInfo.getId());
        if (Integer.valueOf(1).equals(caseInfo.getHandleTimeoutExempt())) {
            throw new BusinessException("该案件已申诉通过，无需重复申请");
        }
        if (existsTimeoutAppeal(caseInfo.getId())) {
            throw new BusinessException("该案件已提起过申诉，不可再次申请");
        }

        boolean isHandler = hasRole(user.getRoles(), ROLE_HANDLER) && !hasRole(user.getRoles(), ROLE_DEPT);

        AppealApply appeal = new AppealApply();
        appeal.setAppealCode(generateAppealCode());
        appeal.setCaseId(caseInfo.getId());
        appeal.setCaseCode(caseInfo.getCaseCode());
        appeal.setApplyType(TimeoutAppealConstant.APPLY_TYPE);
        appeal.setAppealType("处置超时");
        appeal.setAppealDesc(request.getAppealDesc().trim());
        appeal.setApplyDeptId(deptId);
        appeal.setApplyDeptName(caseInfo.getHandleDeptName());
        appeal.setApplyUserId(user.getId());
        appeal.setApplyUserName(displayName(user));
        appeal.setApplyTime(LocalDateTime.now());
        appeal.setAppealStatus(isHandler
                ? TimeoutAppealConstant.STATUS_PENDING_DEPT
                : TimeoutAppealConstant.STATUS_PENDING_DISPATCHER);
        appealApplyMapper.insert(appeal);

        saveAttachments(appeal.getId(), request.getAttachmentPaths(), user);

        caseInfo.setAppealStatus(TimeoutAppealConstant.CASE_APPEAL_PENDING);
        caseInfoMapper.updateById(caseInfo);

        if (isHandler) {
            notifyDeptUsers(deptId, "新申诉待审核",
                    displayName(user) + " 提交了超时申诉，请审核。案件编号：" + caseInfo.getCaseCode());
        } else {
            notifyAllDispatchers("新申诉待审核",
                    displayName(user) + " 提交了超时申诉，请进一步审核。案件编号：" + caseInfo.getCaseCode());
        }

        return appeal;
    }

    @Override
    @Transactional
    public AppealApply deptReview(TimeoutAppealReviewRequest request, LoginUser user) {
        assertRole(user, ROLE_DEPT, ROLE_ADMIN, ROLE_SUPERVISOR);
        AppealApply appeal = requireTimeoutAppeal(request.getAppealId());
        if (!TimeoutAppealConstant.STATUS_PENDING_DEPT.equals(appeal.getAppealStatus())) {
            throw new BusinessException("当前状态不可部门审核");
        }
        boolean approved = Boolean.TRUE.equals(request.getApproved());
        if (!approved && !StringUtils.hasText(request.getOpinion())) {
            throw new BusinessException("打回时请填写意见");
        }
        saveReview(appeal, "dept_review", "处置部门审核",
                approved, request.getOpinion(), user);

        if (approved) {
            appeal.setAppealStatus(TimeoutAppealConstant.STATUS_PENDING_DISPATCHER);
            appealApplyMapper.updateById(appeal);
            notifyAllDispatchers("申诉待派遣员审核",
                    "部门已审核通过超时申诉，请进一步审核。案件编号：" + appeal.getCaseCode());
        } else {
            finalizeRejected(appeal, request.getOpinion());
            notifyUser(appeal.getApplyUserId(), "申诉被驳回",
                    "您的超时申诉（案件 " + appeal.getCaseCode() + "）已被部门驳回");
        }
        return appeal;
    }

    @Override
    @Transactional
    public AppealApply dispatcherReview(TimeoutAppealReviewRequest request, LoginUser user) {
        assertRole(user, ROLE_DISPATCHER, ROLE_ADMIN, ROLE_SUPERVISOR);
        AppealApply appeal = requireTimeoutAppeal(request.getAppealId());
        if (!TimeoutAppealConstant.STATUS_PENDING_DISPATCHER.equals(appeal.getAppealStatus())) {
            throw new BusinessException("当前状态不可派遣员审核");
        }
        boolean approved = Boolean.TRUE.equals(request.getApproved());
        if (!approved && !StringUtils.hasText(request.getOpinion())) {
            throw new BusinessException("打回时请填写意见");
        }
        saveReview(appeal, TimeoutAppealConstant.REVIEW_NODE_DISPATCHER, "派遣员初审",
                approved, request.getOpinion(), user);

        if (approved) {
            appeal.setAppealStatus(TimeoutAppealConstant.STATUS_PENDING_ACCEPTOR);
            appealApplyMapper.updateById(appeal);
            notifyAllAcceptors("申诉待受理员审核",
                    "派遣员已审核通过超时申诉，请进一步审核。案件编号：" + appeal.getCaseCode());
        } else {
            finalizeRejected(appeal, request.getOpinion());
            notifyUser(appeal.getApplyUserId(), "申诉被驳回",
                    "您的超时申诉（案件 " + appeal.getCaseCode() + "）已被派遣员驳回");
        }
        return appeal;
    }

    @Override
    @Transactional
    public AppealApply acceptorReview(TimeoutAppealReviewRequest request, LoginUser user) {
        assertRole(user, ROLE_ACCEPTOR, ROLE_ADMIN, ROLE_SUPERVISOR);
        AppealApply appeal = requireTimeoutAppeal(request.getAppealId());
        if (!TimeoutAppealConstant.STATUS_PENDING_ACCEPTOR.equals(appeal.getAppealStatus())) {
            throw new BusinessException("当前状态不可受理员审核");
        }
        boolean approved = Boolean.TRUE.equals(request.getApproved());
        if (!approved && !StringUtils.hasText(request.getOpinion())) {
            throw new BusinessException("打回时请填写意见");
        }
        saveReview(appeal, TimeoutAppealConstant.REVIEW_NODE_ACCEPTOR, "受理员二审",
                approved, request.getOpinion(), user);

        if (approved) {
            finalizeApproved(appeal, request.getOpinion());
            notifyUser(appeal.getApplyUserId(), "申诉已通过",
                    "您的超时申诉已通过审核，案件 " + appeal.getCaseCode() + " 将免于超时处罚");
        } else {
            finalizeRejected(appeal, request.getOpinion());
            notifyUser(appeal.getApplyUserId(), "申诉被驳回",
                    "您的超时申诉（案件 " + appeal.getCaseCode() + "）已被受理员驳回");
        }
        return appeal;
    }

    @Override
    public TimeoutAppealDetailVo getDetail(Long appealId, LoginUser user) {
        AppealApply appeal = requireTimeoutAppeal(appealId);
        assertCanView(appeal, user);

        TimeoutAppealDetailVo vo = new TimeoutAppealDetailVo();
        vo.setAppeal(appeal);
        vo.setReviews(appealReviewMapper.selectByAppealId(appealId));
        vo.setAttachments(listAttachments(appealId));

        CaseInfo caseInfo = findCase(appeal.getCaseId());
        if (caseInfo != null) {
            fillCaseSummary(vo, caseInfo);
        }
        vo.setCanDeptReview(canDeptReview(appeal, user));
        vo.setCanDispatcherReview(canDispatcherReview(appeal, user));
        vo.setCanAcceptorReview(canAcceptorReview(appeal, user));
        return vo;
    }

    @Override
    public AppealApply getByCaseId(Long caseId) {
        if (caseId == null) {
            return null;
        }
        return appealApplyMapper.selectOne(new LambdaQueryWrapper<AppealApply>()
                .eq(AppealApply::getCaseId, caseId)
                .eq(AppealApply::getApplyType, TimeoutAppealConstant.APPLY_TYPE)
                .eq(AppealApply::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    @Override
    public Page<AppealApply> list(Integer pageNum, Integer pageSize, String tab, String caseCode, LoginUser user) {
        int pn = pageNum != null && pageNum > 0 ? pageNum : 1;
        int ps = pageSize != null && pageSize > 0 ? Math.min(pageSize, 100) : 10;
        Page<AppealApply> page = new Page<>(pn, ps);
        LambdaQueryWrapper<AppealApply> w = new LambdaQueryWrapper<>();
        w.eq(AppealApply::getApplyType, TimeoutAppealConstant.APPLY_TYPE);
        if (StringUtils.hasText(caseCode)) {
            w.like(AppealApply::getCaseCode, caseCode.trim());
        }
        applyListScope(w, tab, user);
        w.orderByDesc(AppealApply::getApplyTime);
        return appealApplyMapper.selectPage(page, w);
    }

    @Override
    public Page<CaseInfo> listAppealableCases(Integer pageNum, Integer pageSize, String caseCode, LoginUser user) {
        int pn = pageNum != null && pageNum > 0 ? pageNum : 1;
        int ps = pageSize != null && pageSize > 0 ? Math.min(pageSize, 100) : 10;
        Page<CaseInfo> page = new Page<>(pn, ps);

        Long deptId = null;
        Long userId = null;
        List<String> roles = user.getRoles();
        if (hasAnyRole(roles, ROLE_ADMIN, ROLE_SUPERVISOR)) {
            // admin sees all
        } else if (hasRole(roles, ROLE_DEPT) || hasRole(roles, ROLE_HANDLER)) {
            deptId = requireUserDeptId(user);
            userId = user.getId();
        } else {
            page.setRecords(List.of());
            return page;
        }

        LambdaQueryWrapper<CaseInfo> w = new LambdaQueryWrapper<>();
        w.eq(CaseInfo::getIsDeleted, 0);
        w.in(CaseInfo::getCaseStatus, CaseStatusConstant.CLOSED, CaseStatusConstant.FORCED_CLOSE);
        w.isNull(CaseInfo::getHandleTimeoutExempt).or().eq(CaseInfo::getHandleTimeoutExempt, 0);
        w.and(q -> q
            .isNull(CaseInfo::getAppealStatus)
            .or()
            .notIn(CaseInfo::getAppealStatus,
                TimeoutAppealConstant.CASE_APPEAL_PENDING,
                TimeoutAppealConstant.CASE_APPEAL_APPROVED)
        );
        // 处置阶段超时的案件
        w.apply("id IN (SELECT DISTINCT case_id FROM case_timer_record WHERE timer_stage = 'handle' AND timer_status = 'timeout')");
        if (deptId != null) {
            w.eq(CaseInfo::getHandleDeptId, deptId);
        }
        if (userId != null && hasRole(roles, ROLE_HANDLER)) {
            // 处置人员只能看到自己经办的
            w.apply("id IN (SELECT DISTINCT case_id FROM case_flow_record WHERE operator_id = {0})", userId);
        }
        if (StringUtils.hasText(caseCode)) {
            w.like(CaseInfo::getCaseCode, caseCode.trim());
        }
        w.orderByDesc(CaseInfo::getCloseTime);
        return caseInfoMapper.selectPage(page, w);
    }

    private void applyListScope(LambdaQueryWrapper<AppealApply> w, String tab, LoginUser user) {
        List<String> roles = user.getRoles();
        if (hasAnyRole(roles, ROLE_ADMIN, ROLE_SUPERVISOR)) {
            applyTabFilter(w, tab, null);
            return;
        }
        boolean isDept = hasRole(roles, ROLE_DEPT);
        boolean isHandler = hasRole(roles, ROLE_HANDLER);
        if (isDept && isHandler) {
            w.and(q -> q
                .eq(AppealApply::getApplyUserId, user.getId())
                .or()
                .eq(AppealApply::getApplyDeptId, requireUserDeptId(user))
            );
            return;
        }
        if (isDept) {
            w.eq(AppealApply::getApplyDeptId, requireUserDeptId(user));
            applyTabFilter(w, tab, null);
            return;
        }
        if (isHandler) {
            w.eq(AppealApply::getApplyUserId, user.getId());
            return;
        }
        if (hasRole(roles, ROLE_DISPATCHER)) {
            if ("pending".equals(tab)) {
                w.eq(AppealApply::getAppealStatus, TimeoutAppealConstant.STATUS_PENDING_DISPATCHER);
            } else if ("done".equals(tab)) {
                w.in(AppealApply::getAppealStatus, TimeoutAppealConstant.STATUS_PENDING_ACCEPTOR,
                        TimeoutAppealConstant.STATUS_APPROVED, TimeoutAppealConstant.STATUS_REJECTED);
            }
            return;
        }
        if (hasRole(roles, ROLE_ACCEPTOR)) {
            if ("pending".equals(tab)) {
                w.eq(AppealApply::getAppealStatus, TimeoutAppealConstant.STATUS_PENDING_ACCEPTOR);
            } else if ("done".equals(tab)) {
                w.in(AppealApply::getAppealStatus, TimeoutAppealConstant.STATUS_APPROVED,
                        TimeoutAppealConstant.STATUS_REJECTED);
            }
            return;
        }
        w.eq(AppealApply::getId, -1L);
    }

    private void applyTabFilter(LambdaQueryWrapper<AppealApply> w, String tab, Long deptId) {
        if (deptId != null) {
            w.eq(AppealApply::getApplyDeptId, deptId);
        }
        if (!StringUtils.hasText(tab) || "all".equals(tab)) {
            return;
        }
        if ("pending".equals(tab)) {
            w.in(AppealApply::getAppealStatus, TimeoutAppealConstant.STATUS_PENDING_DISPATCHER,
                    TimeoutAppealConstant.STATUS_PENDING_ACCEPTOR);
        } else if ("done".equals(tab)) {
            w.in(AppealApply::getAppealStatus, TimeoutAppealConstant.STATUS_APPROVED,
                    TimeoutAppealConstant.STATUS_REJECTED);
        }
    }

    private void notifyUser(Long userId, String title, String content) {
        if (userNotificationSender == null || userId == null) {
            return;
        }
        try {
            userNotificationSender.notifyUser(userId, title, content,
                    BIZ_APPEAL, null, null);
        } catch (Exception e) {
            log.warn("发送申诉提醒失败 userId={}: {}", userId, e.getMessage());
        }
    }

    private void notifyDeptUsers(Long deptId, String title, String content) {
        if (userNotificationSender == null || deptId == null) {
            return;
        }
        try {
            List<Long> deptUserIds = jdbcTemplate.queryForList(
                    """
                    SELECT DISTINCT u.id FROM sys_user u
                    INNER JOIN sys_role_user ru ON ru.user_id = u.id
                    INNER JOIN sys_role r ON r.id = ru.role_id AND r.deleted = 0 AND r.role_code = ?
                    WHERE u.deleted = 0 AND u.status = 1 AND u.department_id = ?
                    """,
                    Long.class, ROLE_DEPT, deptId);
            userNotificationSender.notifyUsers(deptUserIds, title, content,
                    BIZ_APPEAL, null, null);
        } catch (Exception e) {
            log.warn("发送部门申诉提醒失败 deptId={}: {}", deptId, e.getMessage());
        }
    }

    private void notifyAllDispatchers(String title, String content) {
        if (userNotificationSender == null) {
            return;
        }
        try {
            List<Long> dispatcherIds = jdbcTemplate.queryForList(
                    """
                    SELECT DISTINCT ru.user_id FROM sys_role_user ru
                    INNER JOIN sys_role r ON r.id = ru.role_id AND r.deleted = 0 AND r.role_code = ?
                    INNER JOIN sys_user u ON u.id = ru.user_id AND u.deleted = 0 AND u.status = 1
                    """,
                    Long.class, ROLE_DISPATCHER);
            userNotificationSender.notifyUsers(dispatcherIds, title, content,
                    BIZ_APPEAL, null, null);
        } catch (Exception e) {
            log.warn("发送派遣员申诉提醒失败: {}", e.getMessage());
        }
    }

    private void notifyAllAcceptors(String title, String content) {
        if (userNotificationSender == null) {
            return;
        }
        try {
            List<Long> acceptorIds = jdbcTemplate.queryForList(
                    """
                    SELECT DISTINCT ru.user_id FROM sys_role_user ru
                    INNER JOIN sys_role r ON r.id = ru.role_id AND r.deleted = 0 AND r.role_code = ?
                    INNER JOIN sys_user u ON u.id = ru.user_id AND u.deleted = 0 AND u.status = 1
                    """,
                    Long.class, ROLE_ACCEPTOR);
            userNotificationSender.notifyUsers(acceptorIds, title, content,
                    BIZ_APPEAL, null, null);
        } catch (Exception e) {
            log.warn("发送受理员申诉提醒失败: {}", e.getMessage());
        }
    }

    private void finalizeApproved(AppealApply appeal, String opinion) {
        appeal.setAppealStatus(TimeoutAppealConstant.STATUS_APPROVED);
        appeal.setFinalResult(TimeoutAppealConstant.RESULT_APPROVED);
        appeal.setFinalOpinion(opinion);
        appealApplyMapper.updateById(appeal);

        CaseInfo caseInfo = requireCase(appeal.getCaseId());
        caseInfo.setHandleTimeoutExempt(1);
        caseInfo.setHandleTimeoutExemptAppealId(appeal.getId());
        caseInfo.setAppealStatus(TimeoutAppealConstant.CASE_APPEAL_APPROVED);
        caseInfoMapper.updateById(caseInfo);
    }

    private void finalizeRejected(AppealApply appeal, String opinion) {
        appeal.setAppealStatus(TimeoutAppealConstant.STATUS_REJECTED);
        appeal.setFinalResult(TimeoutAppealConstant.RESULT_REJECTED);
        appeal.setFinalOpinion(opinion);
        appealApplyMapper.updateById(appeal);

        CaseInfo caseInfo = requireCase(appeal.getCaseId());
        caseInfo.setAppealStatus(TimeoutAppealConstant.CASE_APPEAL_REJECTED);
        caseInfoMapper.updateById(caseInfo);
    }

    private void saveReview(AppealApply appeal, String node, String nodeName, boolean approved,
                            String opinion, LoginUser user) {
        AppealReview review = new AppealReview();
        review.setAppealId(appeal.getId());
        review.setAppealCode(appeal.getAppealCode());
        review.setReviewNode(node);
        review.setReviewNodeName(nodeName);
        review.setReviewResult(approved ? TimeoutAppealConstant.RESULT_APPROVED
                : TimeoutAppealConstant.RESULT_REJECTED);
        review.setReviewOpinion(opinion);
        review.setReviewerId(user.getId());
        review.setReviewerName(displayName(user));
        Long deptId = user.getDepartmentId();
        if (deptId != null) {
            review.setReviewerDeptId(deptId);
            SysDepartment dept = sysDepartmentMapper.selectById(deptId);
            if (dept != null) {
                review.setReviewerDeptName(dept.getDeptName());
            }
        }
        review.setReviewTime(LocalDateTime.now());
        appealReviewMapper.insert(review);
    }

    private void saveAttachments(Long appealId, List<String> paths, LoginUser user) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        for (String path : paths) {
            if (!StringUtils.hasText(path)) {
                continue;
            }
            AppealAttachment att = new AppealAttachment();
            att.setAppealId(appealId);
            att.setFileType("image");
            att.setFileName(path.substring(path.lastIndexOf('/') + 1));
            att.setFilePath(path.trim());
            att.setUseType("apply");
            att.setUploaderId(user.getId());
            att.setUploaderName(displayName(user));
            appealAttachmentMapper.insert(att);
        }
    }

    private List<AppealAttachment> listAttachments(Long appealId) {
        return appealAttachmentMapper.selectList(new LambdaQueryWrapper<AppealAttachment>()
                .eq(AppealAttachment::getAppealId, appealId)
                .eq(AppealAttachment::getIsDeleted, 0)
                .orderByAsc(AppealAttachment::getCreateTime));
    }

    private void fillCaseSummary(TimeoutAppealDetailVo vo, CaseInfo caseInfo) {
        vo.setCaseId(caseInfo.getId());
        vo.setCaseCode(caseInfo.getCaseCode());
        vo.setCaseStatus(caseInfo.getCaseStatus());
        vo.setHandleDeptName(caseInfo.getHandleDeptName());
        vo.setAddress(caseInfo.getAddress());
        vo.setDescription(caseInfo.getDescription());
        vo.setReportTime(caseInfo.getReportTime());
        vo.setCloseTime(caseInfo.getCloseTime());
        vo.setHandleFinishTime(caseInfo.getHandleFinishTime());
        vo.setHandleTimeoutExempt(Integer.valueOf(1).equals(caseInfo.getHandleTimeoutExempt()));

        CaseTimerRecord timer = caseTimerRecordMapper.selectLatestByCaseAndStage(
                caseInfo.getId(), TimerStageConstant.HANDLE);
        if (timer != null) {
            vo.setHandleDeadlineTime(timer.getDeadlineTime());
            if (timer.getActualFinishTime() != null) {
                vo.setHandleFinishTime(timer.getActualFinishTime());
            }
            vo.setHandleStageTimedOut(timer.getIsTimeout() != null && timer.getIsTimeout() == 1);
            vo.setHandleTimeoutSeconds(timer.getTimeoutSeconds());
        }
    }

    private boolean existsTimeoutAppeal(Long caseId) {
        Long count = appealApplyMapper.selectCount(new LambdaQueryWrapper<AppealApply>()
                .eq(AppealApply::getCaseId, caseId)
                .eq(AppealApply::getApplyType, TimeoutAppealConstant.APPLY_TYPE)
                .eq(AppealApply::getIsDeleted, 0)
                .in(AppealApply::getAppealStatus,
                        TimeoutAppealConstant.STATUS_PENDING_DEPT,
                        TimeoutAppealConstant.STATUS_PENDING_DISPATCHER,
                        TimeoutAppealConstant.STATUS_PENDING_ACCEPTOR));
        return count != null && count > 0;
    }

    private void assertHandleTimedOut(Long caseId) {
        CaseTimerRecord timer = caseTimerRecordMapper.selectLatestByCaseAndStage(caseId, TimerStageConstant.HANDLE);
        if (timer == null || timer.getIsTimeout() == null || timer.getIsTimeout() != 1) {
            throw new BusinessException("该案件处置阶段未超时，不可申诉");
        }
    }

    private void assertClosed(CaseInfo caseInfo) {
        if (caseInfo.getCloseTime() == null || !CLOSED_STATUSES.contains(caseInfo.getCaseStatus())) {
            throw new BusinessException("仅已结案案件可提起申诉");
        }
    }

    private void assertDeptCase(CaseInfo caseInfo, Long deptId) {
        if (caseInfo.getHandleDeptId() == null || !caseInfo.getHandleDeptId().equals(deptId)) {
            throw new BusinessException("仅可对本部门处置案件提起申诉");
        }
    }

    private void assertCanView(AppealApply appeal, LoginUser user) {
        List<String> roles = user.getRoles();
        if (hasAnyRole(roles, ROLE_ADMIN, ROLE_SUPERVISOR)) {
            return;
        }
        if (hasRole(roles, ROLE_DEPT) && appeal.getApplyDeptId().equals(requireUserDeptId(user))) {
            return;
        }
        if (hasAnyRole(roles, ROLE_DISPATCHER, ROLE_ACCEPTOR)) {
            return;
        }
        throw new BusinessException("无权查看该申诉");
    }

    private boolean canDeptReview(AppealApply appeal, LoginUser user) {
        return hasAnyRole(user.getRoles(), ROLE_DEPT, ROLE_ADMIN, ROLE_SUPERVISOR)
                && TimeoutAppealConstant.STATUS_PENDING_DEPT.equals(appeal.getAppealStatus());
    }

    private boolean canDispatcherReview(AppealApply appeal, LoginUser user) {
        return hasAnyRole(user.getRoles(), ROLE_DISPATCHER, ROLE_ADMIN, ROLE_SUPERVISOR)
                && TimeoutAppealConstant.STATUS_PENDING_DISPATCHER.equals(appeal.getAppealStatus());
    }

    private boolean canAcceptorReview(AppealApply appeal, LoginUser user) {
        return hasAnyRole(user.getRoles(), ROLE_ACCEPTOR, ROLE_ADMIN, ROLE_SUPERVISOR)
                && TimeoutAppealConstant.STATUS_PENDING_ACCEPTOR.equals(appeal.getAppealStatus());
    }

    private AppealApply requireTimeoutAppeal(Long id) {
        AppealApply appeal = appealApplyMapper.selectById(id);
        if (appeal == null || !TimeoutAppealConstant.APPLY_TYPE.equals(appeal.getApplyType())) {
            throw new BusinessException("申诉不存在");
        }
        return appeal;
    }

    private CaseInfo requireCase(Long caseId) {
        CaseInfo caseInfo = caseInfoMapper.selectById(caseId);
        if (caseInfo == null || Integer.valueOf(1).equals(caseInfo.getIsDeleted())) {
            throw new BusinessException("案件不存在");
        }
        return caseInfo;
    }

    private CaseInfo findCase(Long caseId) {
        if (caseId == null) return null;
        CaseInfo caseInfo = caseInfoMapper.selectById(caseId);
        if (caseInfo == null || Integer.valueOf(1).equals(caseInfo.getIsDeleted())) {
            return null;
        }
        return caseInfo;
    }

    private Long requireUserDeptId(LoginUser user) {
        if (user.getDepartmentId() != null) {
            return user.getDepartmentId();
        }
        SysUser u = sysUserMapper.selectById(user.getId());
        if (u != null && u.getDepartmentId() != null) {
            return u.getDepartmentId();
        }
        throw new BusinessException("当前账号未关联处置部门");
    }

    private String generateAppealCode() {
        return "AP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private static String displayName(LoginUser user) {
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        return user.getUsername();
    }

    private static void assertRole(LoginUser user, String... allowed) {
        if (user == null || user.getRoles() == null) {
            throw new BusinessException("未登录");
        }
        if (!hasAnyRole(user.getRoles(), allowed)) {
            throw new BusinessException("无权操作");
        }
    }

    private static boolean hasRole(List<String> roles, String role) {
        return roles != null && roles.contains(role);
    }

    private static boolean hasAnyRole(List<String> roles, String... allowed) {
        if (roles == null) {
            return false;
        }
        for (String a : allowed) {
            if (roles.contains(a)) {
                return true;
            }
        }
        return false;
    }
}

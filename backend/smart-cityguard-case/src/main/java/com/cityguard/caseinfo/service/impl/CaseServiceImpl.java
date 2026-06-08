package com.cityguard.caseinfo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.auth.entity.SysDepartment;
import com.cityguard.auth.entity.SysUser;
import com.cityguard.auth.mapper.SysDepartmentMapper;
import com.cityguard.auth.mapper.SysUserMapper;
import com.cityguard.caseinfo.dto.CaseAcceptorReturnDispatcherRequest;
import com.cityguard.caseinfo.dto.CaseDashboardStatsDto;
import com.cityguard.caseinfo.dto.CaseDashboardTodoItemDto;
import com.cityguard.caseinfo.dto.CaseDashboardTodosDto;
import com.cityguard.caseinfo.dto.CaseAcceptorRegisterRequest;
import com.cityguard.caseinfo.dto.CaseAssignHandlerRequest;
import com.cityguard.caseinfo.dto.CaseDeptConfirmRequest;
import com.cityguard.caseinfo.dto.CaseDeptReturnRequest;
import com.cityguard.caseinfo.dto.CaseDispatcherForwardRequest;
import com.cityguard.caseinfo.dto.CaseDispatcherReturnAcceptorRequest;
import com.cityguard.caseinfo.dto.CaseDateFilter;
import com.cityguard.caseinfo.dto.CaseQueryCriteria;
import com.cityguard.caseinfo.dto.CaseRegisterRequest;
import com.cityguard.caseinfo.dto.CaseReturnRequest;
import com.cityguard.caseinfo.dto.CaseRevokeAssignRequest;
import com.cityguard.caseinfo.dto.CaseSendTaskRequest;
import com.cityguard.caseinfo.dto.CollectorCandidateDto;
import com.cityguard.caseinfo.dto.HandlerDeptNotice;
import com.cityguard.common.constant.CaseFlowOperateType;
import com.cityguard.common.constant.CaseSourceConstant;
import com.cityguard.common.spi.UserNotificationSender;
import com.cityguard.caseinfo.entity.*;
import com.cityguard.caseinfo.mapper.*;
import com.cityguard.caseinfo.support.DashboardPeriodHelper;
import com.cityguard.caseinfo.service.CaseAdjustmentService;
import com.cityguard.caseinfo.service.CaseService;
import com.cityguard.common.constant.CaseStatusConstant;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.geo.entity.ResponsibilityGrid;
import com.cityguard.geo.entity.SysGrid;
import com.cityguard.geo.service.GeoService;
import com.cityguard.geo.service.RespGridService;
import com.cityguard.common.constant.TaskStatusConstant;
import com.cityguard.task.entity.CheckTask;
import com.cityguard.task.entity.VerifyTask;
import com.cityguard.task.mapper.CheckTaskMapper;
import com.cityguard.task.mapper.VerifyTaskMapper;
import com.cityguard.task.service.TaskService;
import com.cityguard.timer.constant.TimerStageConstant;
import com.cityguard.timer.model.CaseTimerDisplayInfo;
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
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseServiceImpl implements CaseService {

    private static final String BIZ_CASE = "case";
    private static final String BIZ_CHECK_TASK = "check_task";
    private static final String BIZ_VERIFY_TASK = "verify_task";

    private final CaseInfoMapper caseInfoMapper;
    private final CaseFlowRecordMapper flowRecordMapper;
    private final CaseAttachmentMapper attachmentMapper;
    private final JdbcTemplate jdbcTemplate;
    private final RespGridService respGridService;
    private final GeoService geoService;
    private final SysUserMapper sysUserMapper;
    private final SysDepartmentMapper sysDepartmentMapper;
    private final CheckTaskMapper checkTaskMapper;
    private final VerifyTaskMapper verifyTaskMapper;
    private final TaskService taskService;
    private final CaseTimerService caseTimerService;
    private final CaseAdjustmentService caseAdjustmentService;

    @Autowired(required = false)
    private UserNotificationSender userNotificationSender;

    private static final String ROLE_DISPATCHER = "DISPATCHER";
    private static final String ROLE_COLLECTOR = "COLLECTOR";
    private static final String ROLE_ACCEPTOR = "ACCEPTOR";
    private static final String ROLE_HANDLER = "HANDLER";
    private static final String ROLE_DEPT = "DEPT";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SUPERVISOR = "SUPERVISOR";
    private static final String ROLE_LEADER = "LEADER";

    /**
     * 工作台「待处理」：入口阶段 + 已派至部门但尚未指派处置人员（待指派）+ 部门回退待重派。
     */
    private static final List<String> DASHBOARD_PENDING_STATUSES = List.of(
            CaseStatusConstant.REPORTED,
            CaseStatusConstant.PENDING_VERIFY,
            CaseStatusConstant.PENDING_REGISTER,
            CaseStatusConstant.PENDING_DISPATCH,
            CaseStatusConstant.PENDING_HANDLE,
            CaseStatusConstant.RETURNED
    );

    private static final List<String> DASHBOARD_COMPLETED_STATUSES = List.of(
            CaseStatusConstant.CLOSED,
            CaseStatusConstant.FORCED_CLOSE
    );

    private static final List<String> DASHBOARD_TERMINAL_STATUSES = List.of(
            CaseStatusConstant.CLOSED,
            CaseStatusConstant.FORCED_CLOSE,
            CaseStatusConstant.NOT_ACCEPTED,
            CaseStatusConstant.CANCELLED
    );

    /** 工作台「作废案件」：受理员作废走 not_accepted，预留 cancelled */
    private static final List<String> DASHBOARD_CANCELLED_STATUSES = List.of(
            CaseStatusConstant.NOT_ACCEPTED,
            CaseStatusConstant.CANCELLED
    );

    /** 「我立案的案件」等列表排除：作废后虽写入 register_operator_id，但不应出现在立案跟进列表 */
    private static final List<String> ACCEPTOR_REGISTERED_EXCLUDED_STATUSES = List.of(
            CaseStatusConstant.NOT_ACCEPTED,
            CaseStatusConstant.CANCELLED
    );

    @Override
    @Transactional
    public CaseInfo reportCase(Map<String, Object> reportData) {
        CaseInfo caseInfo = new CaseInfo();
        caseInfo.setCaseCode(generateCaseCode());
        caseInfo.setSourceType("collector");
        caseInfo.setCategoryType((String) reportData.getOrDefault("categoryType", "event"));
        caseInfo.setBigCode((String) reportData.getOrDefault("bigCode", ""));
        caseInfo.setBigName((String) reportData.get("bigName"));
        caseInfo.setSmallCode((String) reportData.getOrDefault("smallCode", ""));
        caseInfo.setSmallName((String) reportData.get("smallName"));
        caseInfo.setSmallId(toLong(reportData.get("smallId")));
        caseInfo.setStandardId(reportData.get("standardId") != null ? Long.valueOf(reportData.get("standardId").toString()) : null);
        caseInfo.setConditionDesc((String) reportData.get("conditionDesc"));
        caseInfo.setAddress((String) reportData.get("address"));
        caseInfo.setLongitude(toDouble(reportData.get("longitude")));
        caseInfo.setLatitude(toDouble(reportData.get("latitude")));
        caseInfo.setDescription((String) reportData.get("description"));
        // 主干流程：进入待立案，由受理员在待办中立案后批转派遣员
        caseInfo.setCaseStatus(CaseStatusConstant.PENDING_REGISTER);
        caseInfo.setReportTime(LocalDateTime.now());
        caseInfo.setCreateTime(LocalDateTime.now());

        // 上报人信息
        caseInfo.setReporterId(reportData.get("reporterId") != null ? Long.valueOf(reportData.get("reporterId").toString()) : null);
        caseInfo.setReporterName((String) reportData.get("reporterName"));
        caseInfo.setReporterPhone((String) reportData.get("reporterPhone"));

        // 采集员仅可在已绑定的责任片区范围内上报
        if (caseInfo.getReporterId() != null
                && caseInfo.getLongitude() != null
                && caseInfo.getLatitude() != null) {
            ResponsibilityGrid respGrid = respGridService.validateCollectorReportLocation(
                    caseInfo.getReporterId(), caseInfo.getLongitude(), caseInfo.getLatitude());
            caseInfo.setRespGridId(respGrid.getId());
        }

        caseInfoMapper.insert(caseInfo);

        // 保存附件
        List<String> attachmentUrls = (List<String>) reportData.get("attachments");
        if (attachmentUrls != null && !attachmentUrls.isEmpty()) {
            for (String url : attachmentUrls) {
                CaseAttachment attachment = new CaseAttachment();
                attachment.setCaseId(caseInfo.getId());
                attachment.setFileType("image");
                attachment.setFilePath(url);
                attachment.setFileName(url.substring(url.lastIndexOf("/") + 1));
                attachment.setNodeCode("reported");
                attachment.setUploaderId(caseInfo.getReporterId() != null ? caseInfo.getReporterId() : 1L);
                attachment.setUploaderName(caseInfo.getReporterName() != null ? caseInfo.getReporterName() : "系统");
                attachmentMapper.insert(attachment);
            }
        }

        // 记录流程
        long reporterId = caseInfo.getReporterId() != null ? caseInfo.getReporterId() : 1L;
        String reporterName = caseInfo.getReporterName() != null && !caseInfo.getReporterName().isBlank()
                ? caseInfo.getReporterName() : "采集员";
        saveFlowRecord(caseInfo.getId(), caseInfo.getCaseCode(), CaseStatusConstant.PENDING_REGISTER, "问题上报",
                "采集员上报（待立案）", reporterId, reporterName, null, "受理员");

        caseTimerService.onCaseReported(caseInfo.getId(), caseInfo.getCaseCode(), caseInfo.getReportTime());

        notifyAllAcceptorsNewCase(caseInfo);
        return caseInfo;
    }

    @Override
    @Transactional
    public CaseInfo acceptorRegisterCase(CaseAcceptorRegisterRequest req, Long operatorId, String operatorName,
                                         List<String> roles) {
        assertAcceptorRegisterRole(roles);
        if (req.getDescription() == null || req.getDescription().isBlank()) {
            throw new BusinessException("请填写问题描述");
        }
        if (req.getAddress() == null || req.getAddress().isBlank()) {
            throw new BusinessException("请填写发生地址");
        }
        if (req.getSmallId() == null && (req.getSmallName() == null || req.getSmallName().isBlank())) {
            throw new BusinessException("请选择案件小类");
        }

        CaseInfo caseInfo = new CaseInfo();
        caseInfo.setCaseCode(generateCaseCode());
        caseInfo.setSourceType("register");
        String sourceDesc = req.getSourceDesc();
        caseInfo.setSourceDesc(sourceDesc != null && !sourceDesc.isBlank() ? sourceDesc.trim() : "电话投诉");
        caseInfo.setCategoryType(req.getCategoryType() != null ? req.getCategoryType() : "event");
        caseInfo.setBigCode(req.getBigCode() != null ? req.getBigCode() : "");
        caseInfo.setBigName(req.getBigName());
        caseInfo.setSmallCode(req.getSmallCode() != null ? req.getSmallCode() : "");
        caseInfo.setSmallName(req.getSmallName());
        caseInfo.setSmallId(req.getSmallId());
        caseInfo.setStandardId(req.getStandardId());
        caseInfo.setConditionDesc(req.getConditionDesc());
        caseInfo.setAddress(req.getAddress().trim());
        caseInfo.setDescription(req.getDescription().trim());
        caseInfo.setLongitude(req.getLongitude());
        caseInfo.setLatitude(req.getLatitude());
        caseInfo.setReporterName(req.getReporterName());
        caseInfo.setReporterPhone(req.getReporterPhone());
        caseInfo.setCaseStatus(CaseStatusConstant.PENDING_REGISTER);
        caseInfo.setReportTime(LocalDateTime.now());
        caseInfo.setCreateTime(LocalDateTime.now());

        Double lng = req.getLongitude();
        Double lat = req.getLatitude();
        if (lng != null && lat != null) {
            for (ResponsibilityGrid grid : respGridService.listAll()) {
                if (grid.getId() != null && respGridService.checkPointInArea(grid.getId(), lng, lat)) {
                    caseInfo.setRespGridId(grid.getId());
                    break;
                }
            }
        }

        caseInfoMapper.insert(caseInfo);

        List<String> attachmentUrls = req.getAttachments();
        if (attachmentUrls != null && !attachmentUrls.isEmpty()) {
            long uploaderId = operatorId != null ? operatorId : 1L;
            String uploaderName = resolveOperatorName(operatorId);
            if ("系统".equals(uploaderName)) {
                uploaderName = "受理员";
            }
            for (String url : attachmentUrls) {
                if (url == null || url.isBlank()) {
                    continue;
                }
                CaseAttachment attachment = new CaseAttachment();
                attachment.setCaseId(caseInfo.getId());
                attachment.setFileType("image");
                attachment.setFilePath(url.trim());
                attachment.setFileName(url.substring(url.lastIndexOf('/') + 1));
                attachment.setNodeCode("reported");
                attachment.setUploaderId(uploaderId);
                attachment.setUploaderName(uploaderName);
                attachmentMapper.insert(attachment);
            }
        }

        String opinion = req.getRemark() != null && !req.getRemark().isBlank()
                ? req.getRemark().trim()
                : "受理员人工登记";
        saveFlowRecord(caseInfo.getId(), caseInfo.getCaseCode(), CaseStatusConstant.PENDING_REGISTER,
                "案件登记", opinion, operatorId, operatorName, null, "受理员");

        if (operatorId != null) {
            caseInfo.setRegisterOperatorId(operatorId);
            caseInfo.setRegisterOperatorName(resolveOperatorName(operatorId));
            caseInfoMapper.updateById(caseInfo);
        }

        caseTimerService.onCaseReported(caseInfo.getId(), caseInfo.getCaseCode(), caseInfo.getReportTime());
        notifyAllAcceptorsNewCase(caseInfo, operatorId);
        return getCaseDetail(caseInfo.getId());
    }

    private void assertAcceptorRegisterRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new BusinessException("未登录或角色无效");
        }
        if (canViewAllPending(roles) || roles.contains(ROLE_ACCEPTOR)) {
            return;
        }
        throw new BusinessException("仅受理员或管理员可登记案件");
    }

    @Override
    public CaseInfo getCaseDetail(Long id) {
        return loadCaseDetail(id);
    }

    @Override
    public CaseInfo getCaseDetail(Long id, Long userId, List<String> roles) {
        CaseInfo caseInfo = loadCaseDetail(id);
        assertCaseReadScope(caseInfo, userId, roles);
        enrichHandlerDeptNotice(caseInfo, userId, roles);
        return caseInfo;
    }

    private CaseInfo loadCaseDetail(Long id) {
        CaseInfo caseInfo = caseInfoMapper.selectById(id);
        if (caseInfo == null) {
            throw new BusinessException("案件不存在");
        }
        enrichCaseGeoDisplay(caseInfo);
        enrichHandleDeptDisplay(caseInfo);
        enrichCaseOperatorDisplay(caseInfo);
        caseInfo.setAwaitingDeptConfirm(isAwaitingDeptConfirm(caseInfo));
        caseInfo.setAwaitingDispatcherForward(isAwaitingDispatcherForward(caseInfo));
        caseInfo.setPendingCheckTask(taskService.hasPendingCheckTask(id));
        caseInfo.setPendingVerifyTask(taskService.hasPendingVerifyTask(id));
        applyTimerDisplay(caseInfo);
        caseInfo.setTimerStages(caseTimerService.buildCaseTimerStages(caseInfo.getId()));
        caseAdjustmentService.enrichCaseDetail(caseInfo);
        return caseInfo;
    }

    private static final List<String> HANDLER_ROUND_FLOW_NODES = List.of("指派处置人员", "部门打回处置人员");
    private static final List<String> DEPT_TO_HANDLER_FLOW_NODES = List.of(
            "指派处置人员", "部门打回处置人员", "部门驳回延期", "部门驳回挂账");

    /**
     * 处置人员详情：展示本处置轮次内处置部门最新一条反馈（指派说明、打回理由、驳回延期/挂账等）。
     */
    private void enrichHandlerDeptNotice(CaseInfo caseInfo, Long viewerId, List<String> roles) {
        if (caseInfo == null || caseInfo.getCurrentHandlerId() == null || viewerId == null) {
            return;
        }
        if (roles == null || !roles.contains(ROLE_HANDLER)) {
            return;
        }
        if (!viewerId.equals(caseInfo.getCurrentHandlerId())) {
            return;
        }
        Long handlerId = caseInfo.getCurrentHandlerId();
        String handlerName = caseInfo.getCurrentHandlerName();

        List<CaseFlowRecord> flows = flowRecordMapper.selectByCaseId(caseInfo.getId());
        if (flows == null || flows.isEmpty()) {
            return;
        }

        CaseFlowRecord roundAnchor = null;
        for (int i = flows.size() - 1; i >= 0; i--) {
            CaseFlowRecord flow = flows.get(i);
            if (!HANDLER_ROUND_FLOW_NODES.contains(flow.getNodeName())) {
                continue;
            }
            if (flowTargetsHandler(flow, handlerId, handlerName)) {
                roundAnchor = flow;
                break;
            }
        }
        if (roundAnchor == null || roundAnchor.getOperateTime() == null) {
            return;
        }

        HandlerDeptNotice latest = null;
        LocalDateTime anchorTime = roundAnchor.getOperateTime();
        for (int i = flows.size() - 1; i >= 0; i--) {
            CaseFlowRecord flow = flows.get(i);
            if (flow.getOperateTime() == null || flow.getOperateTime().isBefore(anchorTime)) {
                continue;
            }
            if (!DEPT_TO_HANDLER_FLOW_NODES.contains(flow.getNodeName())) {
                continue;
            }
            if (("指派处置人员".equals(flow.getNodeName()) || "部门打回处置人员".equals(flow.getNodeName()))
                    && !flowTargetsHandler(flow, handlerId, handlerName)) {
                continue;
            }
            HandlerDeptNotice candidate = buildHandlerDeptNoticeFromFlow(flow);
            if (candidate != null && isNewerNotice(candidate, latest)) {
                latest = candidate;
            }
        }

        HandlerDeptNotice extReject = buildHandlerDeptNoticeFromRejectedApply(
                caseInfo.getLastRejectedExtensionApply(), handlerId, "部门驳回延期", anchorTime);
        if (extReject != null && isNewerNotice(extReject, latest)) {
            latest = extReject;
        }
        HandlerDeptNotice suspendReject = buildHandlerDeptNoticeFromRejectedApply(
                caseInfo.getLastRejectedSuspendApply(), handlerId, "部门驳回挂账", anchorTime);
        if (suspendReject != null && isNewerNotice(suspendReject, latest)) {
            latest = suspendReject;
        }

        if (latest != null && latest.getContent() != null && !latest.getContent().isBlank()) {
            caseInfo.setHandlerDeptNotice(latest);
        }
    }

    private static boolean flowTargetsHandler(CaseFlowRecord flow, Long handlerId, String handlerName) {
        if (flow.getReceiverId() != null && handlerId != null && handlerId.equals(flow.getReceiverId())) {
            return true;
        }
        String opinion = flow.getOperateOpinion();
        return handlerName != null && !handlerName.isBlank()
                && opinion != null && opinion.contains(handlerName);
    }

    private static HandlerDeptNotice buildHandlerDeptNoticeFromFlow(CaseFlowRecord flow) {
        if (flow == null) {
            return null;
        }
        String content = extractHandlerNoticeContent(flow.getNodeName(), flow.getOperateOpinion());
        if (content == null || content.isBlank()) {
            return null;
        }
        HandlerDeptNotice notice = new HandlerDeptNotice();
        notice.setTitle(flow.getNodeName());
        notice.setContent(content);
        notice.setTime(flow.getOperateTime());
        return notice;
    }

    private static HandlerDeptNotice buildHandlerDeptNoticeFromRejectedApply(
            CaseAdjustmentApply apply, Long handlerId, String title, LocalDateTime anchorTime) {
        if (apply == null || handlerId == null || !handlerId.equals(apply.getApplicantId())) {
            return null;
        }
        if (apply.getDeptReviewerId() == null || apply.getReviewerId() != null) {
            return null;
        }
        if (apply.getDeptReviewTime() == null || apply.getDeptReviewTime().isBefore(anchorTime)) {
            return null;
        }
        String content = apply.getDeptReviewRemark() != null ? apply.getDeptReviewRemark().trim() : "";
        if (content.isBlank()) {
            return null;
        }
        HandlerDeptNotice notice = new HandlerDeptNotice();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setTime(apply.getDeptReviewTime());
        return notice;
    }

    private static boolean isNewerNotice(HandlerDeptNotice candidate, HandlerDeptNotice current) {
        if (candidate == null || candidate.getTime() == null) {
            return false;
        }
        if (current == null || current.getTime() == null) {
            return true;
        }
        return !candidate.getTime().isBefore(current.getTime());
    }

    private static String extractHandlerNoticeContent(String nodeName, String opinion) {
        if (opinion == null || opinion.isBlank()) {
            return "";
        }
        if (nodeName != null && nodeName.startsWith("部门驳回")) {
            int colon = opinion.indexOf('：');
            return colon >= 0 ? opinion.substring(colon + 1).trim() : opinion.trim();
        }
        if ("指派处置人员".equals(nodeName) || "部门打回处置人员".equals(nodeName)) {
            int semi = opinion.indexOf('；');
            return semi >= 0 && semi < opinion.length() - 1 ? opinion.substring(semi + 1).trim() : "";
        }
        return opinion.trim();
    }

    private void applyTimerDisplay(CaseInfo caseInfo) {
        CaseTimerDisplayInfo display = caseTimerService.buildCaseTimerDisplay(caseInfo.getId());
        if (display == null) {
            return;
        }
        caseInfo.setTimerStage(display.getTimerStage());
        caseInfo.setTimerStageName(display.getStageName());
        caseInfo.setStageDeadlineTime(display.getDeadlineTime());
        if (display.getTimeRemaining() != null) {
            caseInfo.setTimeRemaining(display.getTimeRemaining());
        }
        if (display.getHandleRemainingSeconds() != null) {
            caseInfo.setHandleRemainingSeconds(display.getHandleRemainingSeconds());
        }
        if (display.getStageTimeout() != null) {
            caseInfo.setStageTimeout(display.getStageTimeout());
        }
        if (display.getHandleTimeout() != null) {
            caseInfo.setHandleTimeout(display.getHandleTimeout());
        } else if (!TimerStageConstant.HANDLE.equals(display.getTimerStage())) {
            caseInfo.setHandleTimeout(null);
        }
        caseInfo.setHandleStageTimedOut(caseTimerService.wasHandleStageTimedOut(caseInfo.getId()));
    }

    private void applyTimerDisplayList(List<CaseInfo> records) {
        if (records == null) {
            return;
        }
        for (CaseInfo c : records) {
            applyTimerDisplay(c);
        }
    }

    private void enrichHandleDeptDisplay(CaseInfo caseInfo) {
        if (caseInfo.getHandleDeptId() != null
                && (caseInfo.getHandleDeptName() == null || caseInfo.getHandleDeptName().isBlank())) {
            SysDepartment dept = sysDepartmentMapper.selectById(caseInfo.getHandleDeptId());
            if (dept != null) {
                caseInfo.setHandleDeptName(dept.getDeptName());
            }
        }
    }

    private void enrichCaseGeoDisplay(CaseInfo caseInfo) {
        if (caseInfo.getRespGridId() != null) {
            ResponsibilityGrid respGrid = respGridService.getById(caseInfo.getRespGridId());
            if (respGrid != null) {
                caseInfo.setRespGridName(respGrid.getRespGridName());
            }
        } else if (caseInfo.getLongitude() != null && caseInfo.getLatitude() != null) {
            // 历史案件未写入 resp_grid_id 时，按坐标反查所属责任片区名称（东/西/南/北/中片区等）
            for (ResponsibilityGrid grid : respGridService.listAll()) {
                if (grid.getId() != null
                        && respGridService.checkPointInArea(grid.getId(), caseInfo.getLongitude(), caseInfo.getLatitude())) {
                    caseInfo.setRespGridName(grid.getRespGridName());
                    break;
                }
            }
        }
        if (caseInfo.getGridId() != null) {
            SysGrid grid = geoService.getGridInfo(caseInfo.getGridId());
            if (grid != null) {
                caseInfo.setGridName(grid.getGridName());
            }
        }
    }

    @Override
    public Page<CaseInfo> getCaseList(Integer pageNum, Integer pageSize, Map<String, Object> params,
                                      Long userId, List<String> roles) {
        Page<CaseInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CaseInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseInfo::getIsDeleted, 0);
        applyCaseListRoleScope(wrapper, userId, roles);

        if (params.get("caseCode") != null && !params.get("caseCode").toString().isBlank()) {
            wrapper.like(CaseInfo::getCaseCode, params.get("caseCode"));
        }
        String statGroup = params.get("statGroup") != null ? params.get("statGroup").toString().trim() : null;
        String period = params.get("period") != null ? params.get("period").toString().trim() : null;
        if (statGroup != null && !statGroup.isBlank()) {
            applyDashboardStatGroup(wrapper, statGroup);
            DashboardPeriodHelper.Range range = DashboardPeriodHelper.resolve(period);
            if (range != null) {
                DashboardPeriodHelper.applyPeriodFilter(wrapper, statGroup, range);
            }
        } else if (params.get("caseStatus") != null && !params.get("caseStatus").toString().isBlank()) {
            wrapper.eq(CaseInfo::getCaseStatus, params.get("caseStatus"));
        }
        if (params.get("smallId") != null) {
            wrapper.eq(CaseInfo::getSmallId, params.get("smallId"));
        }
        if (params.get("categoryBigId") != null) {
            Long bigId = toLong(params.get("categoryBigId"));
            if (bigId != null) {
                wrapper.apply("small_id IN (SELECT id FROM category_small WHERE big_id = {0} AND is_deleted = 0)", bigId);
            }
        }
        if (statGroup == null || statGroup.isBlank() || DashboardPeriodHelper.resolve(period) == null) {
            LocalDateTime start = parseDateTimeParam(params.get("startTime"), false);
            LocalDateTime end = parseDateTimeParam(params.get("endTime"), true);
            if (start != null) {
                wrapper.ge(CaseInfo::getReportTime, start);
            }
            if (end != null) {
                wrapper.le(CaseInfo::getReportTime, end);
            }
        }
        wrapper.orderByDesc(CaseInfo::getReportTime);

        Page<CaseInfo> pageResult = caseInfoMapper.selectPage(page, wrapper);
        enrichCaseListDisplay(pageResult.getRecords());
        applyTimerDisplayList(pageResult.getRecords());
        return pageResult;
    }

    @Override
    public Page<CaseInfo> queryCases(CaseQueryCriteria criteria, Long userId, List<String> roles) {
        assertCaseQueryRole(roles);
        CaseQueryCriteria q = criteria != null ? criteria : new CaseQueryCriteria();
        int pageNum = q.getPageNum() != null && q.getPageNum() > 0 ? q.getPageNum() : 1;
        int pageSize = q.getPageSize() != null && q.getPageSize() > 0 ? Math.min(q.getPageSize(), 100) : 10;
        Page<CaseInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CaseInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseInfo::getIsDeleted, 0);
        applyComprehensiveQueryRoleScope(wrapper, userId, roles);
        applyCaseQueryCriteria(wrapper, q);
        wrapper.orderByDesc(CaseInfo::getReportTime);
        Page<CaseInfo> pageResult = caseInfoMapper.selectPage(page, wrapper);
        enrichCaseListDisplay(pageResult.getRecords());
        applyTimerDisplayList(pageResult.getRecords());
        for (CaseInfo row : pageResult.getRecords()) {
            enrichCaseGeoDisplay(row);
        }
        return pageResult;
    }

    @Override
    public Page<CaseInfo> getPendingCases(String status, Integer pageNum, Integer pageSize,
                                          Long userId, List<String> roles) {
        Page<CaseInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CaseInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseInfo::getIsDeleted, 0);
        if ("acceptor_todo".equals(status)) {
            wrapper.in(CaseInfo::getCaseStatus,
                    CaseStatusConstant.PENDING_REGISTER,
                    CaseStatusConstant.PENDING_VERIFY,
                    CaseStatusConstant.REPORTED);
            applyAcceptorUnclaimedPoolScope(wrapper, userId, roles);
        } else if ("acceptor_pending_register".equals(status)) {
            wrapper.in(CaseInfo::getCaseStatus,
                    CaseStatusConstant.REPORTED,
                    CaseStatusConstant.PENDING_REGISTER);
            applyAcceptorUnclaimedPoolScope(wrapper, userId, roles);
        } else if ("acceptor_pending_verify".equals(status)) {
            applyAcceptorPendingVerifyScope(wrapper, userId, roles);
        } else if ("acceptor_collect_check".equals(status)) {
            applyAcceptorCollectCheckScope(wrapper, userId, roles);
        } else if ("acceptor_pending_close".equals(status)) {
            applyAcceptorPendingCloseScope(wrapper, userId, roles);
        } else if ("acceptor_registered".equals(status)) {
            applyAcceptorRegisteredScope(wrapper, userId, roles);
        } else if ("dispatcher_pending_dispatch".equals(status)
                && userId != null
                && roles != null
                && roles.contains(ROLE_DISPATCHER)
                && !canViewAllPending(roles)) {
            wrapper.eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_DISPATCH)
                    .eq(CaseInfo::getCurrentHandlerId, userId);
        } else if ("dispatcher_pending_review".equals(status)
                && userId != null
                && roles != null
                && roles.contains(ROLE_DISPATCHER)
                && !canViewAllPending(roles)) {
            wrapper.eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_CHECK)
                    .eq(CaseInfo::getCurrentHandlerId, userId);
        } else if ("dispatcher_returned".equals(status)
                && userId != null
                && roles != null
                && roles.contains(ROLE_DISPATCHER)
                && !canViewAllPending(roles)) {
            wrapper.eq(CaseInfo::getCaseStatus, CaseStatusConstant.RETURNED)
                    .eq(CaseInfo::getCurrentHandlerId, userId);
        } else if ("dispatcher_handled".equals(status)
                && userId != null
                && roles != null
                && roles.contains(ROLE_DISPATCHER)
                && !canViewAllPending(roles)) {
            wrapper.apply("id IN (SELECT DISTINCT case_id FROM case_flow_record WHERE operator_id = {0})", userId);
        } else if (CaseStatusConstant.PENDING_DISPATCH.equals(status)
                && userId != null
                && roles != null
                && roles.contains(ROLE_DISPATCHER)
                && !canViewAllPending(roles)) {
            wrapper.eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_DISPATCH)
                    .eq(CaseInfo::getCurrentHandlerId, userId);
        } else if ("handler_dept_todo".equals(status)) {
            // 处置部门待指派：由部门登录账号分派给本部门处置人员
            if (canViewAllPending(roles)) {
                wrapper.eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_HANDLE)
                        .isNull(CaseInfo::getCurrentHandlerId);
            } else if (userId != null && roles != null && roles.contains(ROLE_DEPT)) {
                Long deptId = resolveUserDepartmentId(userId);
                wrapper.eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_HANDLE)
                        .eq(CaseInfo::getHandleDeptId, deptId)
                        .isNull(CaseInfo::getCurrentHandlerId);
            } else {
                wrapper.eq(CaseInfo::getId, -1L);
            }
        } else if ("dept_confirm_todo".equals(status)) {
            applyAwaitingDeptConfirmScope(wrapper, userId, roles);
        } else if (CaseStatusConstant.HANDLING.equals(status)
                && userId != null
                && roles != null
                && roles.contains(ROLE_HANDLER)
                && !canViewAllPending(roles)) {
            wrapper.eq(CaseInfo::getCaseStatus, CaseStatusConstant.HANDLING)
                    .eq(CaseInfo::getCurrentHandlerId, userId);
        } else if (CaseStatusConstant.PENDING_CHECK.equals(status)
                && userId != null
                && roles != null
                && !canViewAllPending(roles)
                && roles.contains(ROLE_DISPATCHER)) {
            wrapper.eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_CHECK)
                    .eq(CaseInfo::getCurrentHandlerId, userId);
        } else if (CaseStatusConstant.NOT_ACCEPTED.equals(status)
                && userId != null
                && roles != null
                && roles.contains(ROLE_ACCEPTOR)
                && !canViewAllPending(roles)) {
            wrapper.eq(CaseInfo::getCaseStatus, CaseStatusConstant.NOT_ACCEPTED)
                    .eq(CaseInfo::getRegisterOperatorId, userId);
        } else if (userId != null
                && roles != null
                && roles.contains(ROLE_DISPATCHER)
                && !canViewAllPending(roles)) {
            wrapper.eq(CaseInfo::getCaseStatus, status)
                    .eq(CaseInfo::getCurrentHandlerId, userId);
        } else {
            wrapper.eq(CaseInfo::getCaseStatus, status);
        }
        wrapper.orderByDesc(CaseInfo::getReportTime);
        Page<CaseInfo> result = caseInfoMapper.selectPage(page, wrapper);
        enrichCaseListDisplay(result.getRecords());
        applyTimerDisplayList(result.getRecords());
        return result;
    }

    @Override
    public CaseDashboardStatsDto getDashboardStats(Long userId, List<String> roles, String period) {
        CaseDashboardStatsDto stats = new CaseDashboardStatsDto();
        stats.setPendingCases(countByDashboardStatGroup("pending", userId, roles, period));
        stats.setProcessingCases(countByDashboardStatGroup("processing", userId, roles, period));
        stats.setCompletedCases(countByDashboardStatGroup("completed", userId, roles, period));
        stats.setOverdueCases(countByDashboardStatGroup("overdue", userId, roles, period));
        stats.setCancelledCases(countByDashboardStatGroup("cancelled", userId, roles, period));
        return stats;
    }

    @Override
    public CaseDashboardTodosDto getDashboardTodos(Long userId, List<String> roles, int limit) {
        int cap = limit <= 0 ? 10 : Math.min(limit, 30);
        List<CaseInfo> merged = collectMergedDashboardTodoCases(userId, roles);
        enrichCaseListDisplay(merged);
        CaseDashboardTodosDto result = new CaseDashboardTodosDto();
        List<CaseDashboardTodoItemDto> items = new ArrayList<>();
        for (int i = 0; i < merged.size() && i < cap; i++) {
            items.add(toDashboardTodoItem(merged.get(i)));
        }
        result.setItems(items);
        return result;
    }

    @Override
    public Page<CaseDashboardTodoItemDto> getDashboardTodosPage(Integer pageNum, Integer pageSize,
                                                              Long userId, List<String> roles) {
        List<CaseInfo> merged = collectMergedDashboardTodoCases(userId, roles);
        enrichCaseListDisplay(merged);
        int pn = pageNum != null && pageNum > 0 ? pageNum : 1;
        int ps = pageSize != null && pageSize > 0 ? Math.min(pageSize, 100) : 10;
        int total = merged.size();
        int from = (pn - 1) * ps;
        List<CaseDashboardTodoItemDto> records = new ArrayList<>();
        if (from < total) {
            int to = Math.min(from + ps, total);
            for (int i = from; i < to; i++) {
                records.add(toDashboardTodoItem(merged.get(i)));
            }
        }
        Page<CaseDashboardTodoItemDto> page = new Page<>(pn, ps, total);
        page.setRecords(records);
        return page;
    }

    /** 按角色合并各待办队列，按截止时间与上报时间排序 */
    private List<CaseInfo> collectMergedDashboardTodoCases(Long userId, List<String> roles) {
        List<String> statuses = resolveDashboardTodoStatuses(roles);
        if (statuses.isEmpty()) {
            return List.of();
        }
        Map<Long, CaseInfo> merged = new LinkedHashMap<>();
        int perQueue = 200;
        for (String status : statuses) {
            Page<CaseInfo> page = getPendingCases(status, 1, perQueue, userId, roles);
            if (page.getRecords() == null) {
                continue;
            }
            for (CaseInfo c : page.getRecords()) {
                if (c.getId() != null) {
                    merged.putIfAbsent(c.getId(), c);
                }
            }
        }
        List<CaseInfo> sorted = new ArrayList<>(merged.values());
        sorted.sort(this::compareDashboardTodoCases);
        applyTimerDisplayList(sorted);
        return sorted;
    }

    private List<String> resolveDashboardTodoStatuses(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> statuses = new LinkedHashSet<>();
        if (canViewAllPending(roles)) {
            statuses.add("acceptor_todo");
            statuses.add("handler_dept_todo");
            statuses.add("dept_confirm_todo");
            statuses.add(CaseStatusConstant.PENDING_DISPATCH);
            statuses.add("dispatcher_pending_review");
            statuses.add(CaseStatusConstant.RETURNED);
            return new ArrayList<>(statuses);
        }
        if (roles.contains(ROLE_ACCEPTOR)) {
            statuses.add("acceptor_todo");
            statuses.add("acceptor_pending_verify");
            statuses.add("acceptor_collect_check");
            statuses.add("acceptor_pending_close");
        }
        if (roles.contains(ROLE_DISPATCHER)) {
            statuses.add("dispatcher_pending_dispatch");
            statuses.add("dispatcher_pending_review");
            statuses.add("dispatcher_returned");
        }
        if (roles.contains(ROLE_DEPT)) {
            statuses.add("handler_dept_todo");
            statuses.add("dept_confirm_todo");
        }
        if (roles.contains(ROLE_HANDLER)) {
            statuses.add(CaseStatusConstant.HANDLING);
        }
        return new ArrayList<>(statuses);
    }

    private int compareDashboardTodoCases(CaseInfo a, CaseInfo b) {
        LocalDateTime da = effectiveStageDeadline(a);
        LocalDateTime db = effectiveStageDeadline(b);
        if (da != null && db != null) {
            int cmp = da.compareTo(db);
            if (cmp != 0) {
                return cmp;
            }
        } else if (da != null) {
            return -1;
        } else if (db != null) {
            return 1;
        }
        LocalDateTime ra = a.getReportTime();
        LocalDateTime rb = b.getReportTime();
        if (ra != null && rb != null) {
            return rb.compareTo(ra);
        }
        return 0;
    }

    private CaseDashboardTodoItemDto toDashboardTodoItem(CaseInfo c) {
        CaseDashboardTodoItemDto item = new CaseDashboardTodoItemDto();
        item.setType("case");
        item.setId(c.getId());
        item.setCaseId(c.getId());
        String code = c.getCaseCode() != null ? c.getCaseCode() : ("案件#" + c.getId());
        if (c.getSmallName() != null && !c.getSmallName().isBlank()) {
            item.setTitle(code + " · " + c.getSmallName());
        } else if (c.getAddress() != null && !c.getAddress().isBlank()) {
            item.setTitle(code + " · " + c.getAddress());
        } else {
            item.setTitle(code);
        }
        item.setStatus(resolveTodoStatusLabel(c));
        item.setTimerStageName(c.getTimerStageName());
        item.setDeadline(effectiveStageDeadline(c));
        return item;
    }

    private LocalDateTime effectiveStageDeadline(CaseInfo c) {
        if (c.getStageDeadlineTime() != null) {
            return c.getStageDeadlineTime();
        }
        return c.getDeadlineTime();
    }

    private String resolveTodoStatusLabel(CaseInfo c) {
        if (Boolean.TRUE.equals(c.getAwaitingDeptConfirm())) {
            return "处置人员已处置";
        }
        if (Boolean.TRUE.equals(c.getAwaitingDispatcherForward())) {
            return "待派遣员把关";
        }
        if (Boolean.TRUE.equals(c.getPendingVerifyTask())) {
            return "核实中";
        }
        if (Boolean.TRUE.equals(c.getPendingCheckTask())) {
            return "核查中";
        }
        String st = c.getCaseStatus();
        if (st == null) {
            return "--";
        }
        return switch (st) {
            case CaseStatusConstant.REPORTED -> "已上报";
            case CaseStatusConstant.PENDING_VERIFY -> "待核实";
            case CaseStatusConstant.PENDING_REGISTER -> "待立案";
            case CaseStatusConstant.PENDING_DISPATCH -> "待派遣";
            case CaseStatusConstant.PENDING_HANDLE -> "待指派";
            case CaseStatusConstant.HANDLING -> "处置中";
            case CaseStatusConstant.HANDLE_FINISH -> "处置人员已处置";
            case CaseStatusConstant.PENDING_CHECK -> c.getCurrentHandlerId() != null ? "待结案" : "待批转受理员";
            case CaseStatusConstant.CHECKING -> "核查中";
            case CaseStatusConstant.RETURNED -> "部门回退";
            default -> st;
        };
    }

    /** 列表/待办：补充支线任务与批转展示字段，供前端统一状态文案 */
    private void enrichCaseListDisplay(List<CaseInfo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (CaseInfo record : records) {
            record.setAwaitingDeptConfirm(isAwaitingDeptConfirm(record));
            record.setAwaitingDispatcherForward(isAwaitingDispatcherForward(record));
            record.setPendingCheckTask(taskService.hasPendingCheckTask(record.getId()));
            record.setPendingVerifyTask(taskService.hasPendingVerifyTask(record.getId()));
        }
    }

    private long countByDashboardStatGroup(String statGroup, Long userId, List<String> roles, String period) {
        LambdaQueryWrapper<CaseInfo> wrapper = buildDashboardScopeWrapper(userId, roles);
        applyDashboardStatGroup(wrapper, statGroup);
        DashboardPeriodHelper.Range range = DashboardPeriodHelper.resolve(period);
        DashboardPeriodHelper.applyPeriodFilter(wrapper, statGroup, range);
        return caseInfoMapper.selectCount(wrapper);
    }

    private void applyDashboardStatGroup(LambdaQueryWrapper<CaseInfo> wrapper, String statGroup) {
        if (statGroup == null || statGroup.isBlank()) {
            return;
        }
        switch (statGroup) {
            case "pending" -> wrapper.in(CaseInfo::getCaseStatus, DASHBOARD_PENDING_STATUSES);
            case "processing" -> wrapper.notIn(CaseInfo::getCaseStatus, DASHBOARD_TERMINAL_STATUSES)
                    .notIn(CaseInfo::getCaseStatus, DASHBOARD_PENDING_STATUSES);
            case "completed" -> wrapper.in(CaseInfo::getCaseStatus, DASHBOARD_COMPLETED_STATUSES);
            case "overdue" -> {
                wrapper.notIn(CaseInfo::getCaseStatus, DASHBOARD_TERMINAL_STATUSES);
                applyDashboardOverdueCondition(wrapper);
            }
            case "cancelled" -> wrapper.in(CaseInfo::getCaseStatus, DASHBOARD_CANCELLED_STATUSES);
            default -> { /* ignore unknown group */ }
        }
    }

    private LambdaQueryWrapper<CaseInfo> buildDashboardScopeWrapper(Long userId, List<String> roles) {
        LambdaQueryWrapper<CaseInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseInfo::getIsDeleted, 0);
        applyCaseListRoleScope(wrapper, userId, roles);
        return wrapper;
    }

    private void applyDashboardOverdueCondition(LambdaQueryWrapper<CaseInfo> wrapper) {
        wrapper.and(w -> w.isNull(CaseInfo::getHandleTimeoutExempt).or().eq(CaseInfo::getHandleTimeoutExempt, 0));
        wrapper.apply("EXISTS (SELECT 1 FROM case_timer_record t WHERE t.case_id = id "
                + "AND t.timer_status IN ('running', 'paused') AND t.deadline_time < NOW())");
    }

    @Override
    public Page<CaseInfo> getMyCaseList(Long reporterId, Integer pageNum, Integer pageSize) {
        Page<CaseInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CaseInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseInfo::getReporterId, reporterId)
               .eq(CaseInfo::getIsDeleted, 0)
               .orderByDesc(CaseInfo::getReportTime);
        Page<CaseInfo> result = caseInfoMapper.selectPage(page, wrapper);
        enrichCaseListDisplay(result.getRecords());
        applyTimerDisplayList(result.getRecords());
        return result;
    }

    @Override
    @Transactional
    public CaseInfo registerCase(CaseRegisterRequest req, Long operatorId, String operatorName) {
        Long caseId = req.getCaseId();
        CaseInfo loaded = getCaseDetail(caseId);
        String st = loaded.getCaseStatus() != null ? loaded.getCaseStatus().trim() : null;
        if (CaseStatusConstant.CHECKING.equals(st) || taskService.hasPendingCheckTask(caseId)) {
            throw new BusinessException("核查任务进行中，请等待采集员完成后再立案（核查为可选分支）");
        }
        boolean canRegister = CaseStatusConstant.PENDING_REGISTER.equals(st)
                || CaseStatusConstant.PENDING_VERIFY.equals(st)
                || CaseStatusConstant.REPORTED.equals(st)
                || isAcceptorRedispatchAfterReturn(loaded, operatorId);
        if (!canRegister) {
            throw new BusinessException(
                    "案件状态不正确，无法立案（当前状态：" + (st != null ? st : "空") + "，仅待立案/已上报或回退待重派可立案）");
        }
        assertAcceptorCanOperateCase(loaded, operatorId);
        if (req.getStandardId() == null && loaded.getStandardId() == null) {
            throw new BusinessException("请选择立结案标准");
        }

        SysUser dispatcher = resolveDispatcherUser(req.getDispatcherUserId());

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<CaseInfo> uw = new LambdaUpdateWrapper<>();
        List<String> registerStatuses = new ArrayList<>(List.of(
                CaseStatusConstant.PENDING_REGISTER,
                CaseStatusConstant.PENDING_VERIFY,
                CaseStatusConstant.REPORTED));
        if (isAcceptorRedispatchAfterReturn(loaded, operatorId)) {
            registerStatuses.add(CaseStatusConstant.RETURNED);
        }
        uw.eq(CaseInfo::getId, caseId)
                .in(CaseInfo::getCaseStatus, registerStatuses)
                .eq(CaseInfo::getIsDeleted, 0)
                .and(w -> w.isNull(CaseInfo::getRegisterOperatorId)
                        .or()
                        .eq(CaseInfo::getRegisterOperatorId, operatorId));
        if (req.getClientUpdateTime() != null) {
            uw.eq(CaseInfo::getUpdateTime, req.getClientUpdateTime());
        }
        String acceptorDisplayName = resolveOperatorName(operatorId);
        String dispatcherDisplayName = displayUserName(dispatcher);
        uw.set(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_DISPATCH)
                .set(CaseInfo::getAcceptTime, now)
                .set(CaseInfo::getRegisterOperatorId, operatorId)
                .set(CaseInfo::getRegisterOperatorName, acceptorDisplayName)
                .set(CaseInfo::getCurrentHandlerId, dispatcher.getId())
                .set(CaseInfo::getCurrentHandlerName, dispatcherDisplayName);
        if (dispatcher.getDepartmentId() != null) {
            uw.set(CaseInfo::getCurrentDeptId, dispatcher.getDepartmentId());
            if (dispatcher.getDepartmentName() != null) {
                uw.set(CaseInfo::getCurrentDeptName, dispatcher.getDepartmentName());
            }
        }
        if (req.getAddress() != null) {
            uw.set(CaseInfo::getAddress, req.getAddress());
        }
        if (req.getDescription() != null) {
            uw.set(CaseInfo::getDescription, req.getDescription());
        }
        if (req.getBigName() != null) {
            uw.set(CaseInfo::getBigName, req.getBigName());
        }
        if (req.getSmallName() != null) {
            uw.set(CaseInfo::getSmallName, req.getSmallName());
        }
        if (req.getSmallId() != null) {
            uw.set(CaseInfo::getSmallId, req.getSmallId());
        }
        if (req.getStandardId() != null) {
            uw.set(CaseInfo::getStandardId, req.getStandardId());
        }
        if (req.getConditionDesc() != null) {
            uw.set(CaseInfo::getConditionDesc, req.getConditionDesc());
        }
        if (req.getLongitude() != null) {
            uw.set(CaseInfo::getLongitude, req.getLongitude());
        }
        if (req.getLatitude() != null) {
            uw.set(CaseInfo::getLatitude, req.getLatitude());
        }

        int rows = caseInfoMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException("立案失败：案件已被其他同事处理或数据已变更，请刷新页面后重试");
        }

        CaseInfo updated = getCaseDetail(caseId);
        String opinion = req.getRemark() != null ? req.getRemark() : "";
        String flowRemark = "批转派遣员：" + dispatcherDisplayName
                + (opinion.isBlank() ? "" : "；" + opinion);
        saveFlowRecord(caseId, updated.getCaseCode(), CaseStatusConstant.PENDING_DISPATCH, "立案并批转",
                CaseFlowOperateType.FORWARD, flowRemark, operatorId, acceptorDisplayName,
                dispatcher.getId(), dispatcherDisplayName);

        notifyUserTask(dispatcher.getId(), "新案件待派遣",
                "案件 " + updated.getCaseCode() + " 已立案，请派遣处置部门",
                BIZ_CASE, updated.getId(), updated.getCaseCode());
        caseTimerService.onCaseRegistered(updated.getId(), updated.getCaseCode(), now);
        return updated;
    }

    @Override
    @Transactional
    public CaseInfo dispatchCase(Long caseId, Long departmentId, String remark, LocalDateTime clientUpdateTime,
                                 Long operatorId, List<String> operatorRoles) {
        CaseInfo loaded = getCaseDetail(caseId);
        String st = loaded.getCaseStatus();
        if (!CaseStatusConstant.PENDING_DISPATCH.equals(st)
                && !CaseStatusConstant.RETURNED.equals(st)) {
            throw new BusinessException("案件状态不正确，无法派遣");
        }
        assertDispatcherAssignee(loaded, operatorId, operatorRoles);

        SysDepartment handleDept = sysDepartmentMapper.selectById(departmentId);
        if (handleDept == null) {
            throw new BusinessException("处置部门不存在");
        }
        String deptName = handleDept.getDeptName();

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<CaseInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(CaseInfo::getId, caseId)
                .in(CaseInfo::getCaseStatus,
                        CaseStatusConstant.PENDING_DISPATCH,
                        CaseStatusConstant.RETURNED)
                .eq(CaseInfo::getIsDeleted, 0);
        if (clientUpdateTime != null) {
            uw.eq(CaseInfo::getUpdateTime, clientUpdateTime);
        }
        if (CaseStatusConstant.RETURNED.equals(st)) {
            uw.eq(CaseInfo::getCurrentHandlerId, operatorId);
        }
        uw.set(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_HANDLE)
                .set(CaseInfo::getHandleDeptId, departmentId)
                .set(CaseInfo::getHandleDeptName, deptName)
                .set(CaseInfo::getCurrentDeptId, departmentId)
                .set(CaseInfo::getCurrentDeptName, deptName)
                .set(CaseInfo::getDispatchTime, now)
                .set(CaseInfo::getDispatchOperatorId, operatorId)
                .set(CaseInfo::getDispatchOperatorName, resolveOperatorName(operatorId))
                .set(CaseInfo::getCurrentHandlerId, null)
                .set(CaseInfo::getCurrentHandlerName, null);

        int rows = caseInfoMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException("派遣失败：案件已被其他同事处理或数据已变更，请刷新页面后重试");
        }

        CaseInfo updated = getCaseDetail(caseId);
        String flowRemark = "派遣至处置部门：" + deptName
                + (remark != null && !remark.isBlank() ? "；" + remark : "");
        saveFlowRecord(caseId, updated.getCaseCode(), CaseStatusConstant.PENDING_HANDLE, "派遣至处置部门",
                CaseFlowOperateType.FORWARD, flowRemark, operatorId, resolveOperatorName(operatorId),
                departmentId, deptName);

        notifyDeptUsersNewCase(updated, deptName);
        caseTimerService.onCaseDispatched(updated.getId(), updated.getCaseCode(),
                updated.getSmallId(), updated.getStandardId(), now);
        return updated;
    }

    @Override
    @Transactional
    public CaseInfo assignHandler(CaseAssignHandlerRequest req, Long operatorId, String operatorName,
                                  List<String> operatorRoles) {
        Long caseId = req.getCaseId();
        CaseInfo loaded = getCaseDetail(caseId);
        caseAdjustmentService.assertCaseOperable(loaded);
        if (!CaseStatusConstant.PENDING_HANDLE.equals(loaded.getCaseStatus())) {
            throw new BusinessException("案件状态不正确，无法指派处置人员");
        }
        if (loaded.getCurrentHandlerId() != null) {
            throw new BusinessException("案件已指派处置人员，请勿重复操作");
        }
        if (loaded.getHandleDeptId() == null) {
            throw new BusinessException("案件尚未派遣至处置部门");
        }
        assertHandlerDeptOperator(loaded, operatorId, operatorRoles);

        SysUser handler = resolveHandlerUser(req.getHandlerUserId(), loaded.getHandleDeptId());
        String handlerName = displayUserName(handler);
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<CaseInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(CaseInfo::getId, caseId)
                .eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_HANDLE)
                .isNull(CaseInfo::getCurrentHandlerId)
                .eq(CaseInfo::getIsDeleted, 0);
        if (req.getClientUpdateTime() != null) {
            uw.eq(CaseInfo::getUpdateTime, req.getClientUpdateTime());
        }
        uw.set(CaseInfo::getCaseStatus, CaseStatusConstant.HANDLING)
                .set(CaseInfo::getCurrentHandlerId, handler.getId())
                .set(CaseInfo::getCurrentHandlerName, handlerName)
                .set(CaseInfo::getHandleReceiveTime, now);

        int rows = caseInfoMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException("指派失败：案件已被其他同事处理或数据已变更，请刷新后重试");
        }

        CaseInfo updated = getCaseDetail(caseId);
        String opinion = req.getRemark() != null ? req.getRemark() : "";
        String flowRemark = "指派处置人员：" + handlerName
                + (opinion.isBlank() ? "" : "；" + opinion);
        saveFlowRecord(caseId, updated.getCaseCode(), CaseStatusConstant.HANDLING, "指派处置人员",
                CaseFlowOperateType.FORWARD, flowRemark, operatorId, operatorName,
                handler.getId(), handlerName);

        notifyUserTask(handler.getId(), "新案件待处置",
                "案件 " + updated.getCaseCode() + " 已指派给您，请现场处置",
                BIZ_CASE, updated.getId(), updated.getCaseCode());
        return updated;
    }

    @Override
    @Transactional
    public CaseInfo handleCase(Long caseId, String remark, List<String> attachments,
                               Long operatorId, List<String> operatorRoles) {
        CaseInfo caseInfo = getCaseDetail(caseId);
        caseAdjustmentService.assertCaseOperable(caseInfo);
        if (!CaseStatusConstant.HANDLING.equals(caseInfo.getCaseStatus())) {
            throw new BusinessException("案件状态不正确，无法处置（需先由处置部门指派处置人员）");
        }
        assertAssignedHandler(caseInfo, operatorId, operatorRoles);

        caseInfo.setCaseStatus(CaseStatusConstant.HANDLE_FINISH);
        caseInfo.setHandleFinishTime(LocalDateTime.now());
        caseInfoMapper.updateById(caseInfo);

        // 保存处置附件
        if (attachments != null && !attachments.isEmpty()) {
            Long uploaderId = operatorId != null ? operatorId : 1L;
            String uploaderName = resolveOperatorName(operatorId);
            for (String url : attachments) {
                CaseAttachment attachment = new CaseAttachment();
                attachment.setCaseId(caseId);
                attachment.setFileType("image");
                attachment.setFilePath(url);
                attachment.setFileName(url.substring(url.lastIndexOf("/") + 1));
                attachment.setNodeCode("handle_finish");
                attachment.setUploaderId(uploaderId);
                attachment.setUploaderName(uploaderName);
                attachmentMapper.insert(attachment);
            }
        }

        saveFlowRecord(caseId, caseInfo.getCaseCode(), CaseStatusConstant.HANDLE_FINISH, "处置人员已处置",
                CaseFlowOperateType.FORWARD, remark != null ? remark : "",
                operatorId, resolveOperatorName(operatorId),
                null, caseInfo.getHandleDeptName());

        return caseInfo;
    }

    @Override
    @Transactional
    public CaseInfo deptConfirmCase(CaseDeptConfirmRequest req, Long operatorId, String operatorName,
                                    List<String> operatorRoles) {
        Long caseId = req.getCaseId();
        CaseInfo loaded = getCaseDetail(caseId);
        caseAdjustmentService.assertCaseOperable(loaded);
        if (!isAwaitingDeptConfirm(loaded)) {
            throw new BusinessException("案件状态不正确，无法批转（需为处置人员已处置、待部门确认）");
        }
        assertHandlerDeptOperator(loaded, operatorId, operatorRoles);

        SysUser dispatcher = resolveDispatcherUser(req.getDispatcherUserId());
        String dispatcherName = displayUserName(dispatcher);

        LambdaUpdateWrapper<CaseInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(CaseInfo::getId, caseId)
                .eq(CaseInfo::getIsDeleted, 0);
        if (CaseStatusConstant.HANDLE_FINISH.equals(loaded.getCaseStatus())) {
            uw.eq(CaseInfo::getCaseStatus, CaseStatusConstant.HANDLE_FINISH);
        } else {
            uw.eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_CHECK)
                    .isNotNull(CaseInfo::getHandleFinishTime);
        }
        if (req.getClientUpdateTime() != null) {
            uw.eq(CaseInfo::getUpdateTime, req.getClientUpdateTime());
        }
        uw.set(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_CHECK)
                .set(CaseInfo::getCurrentHandlerId, dispatcher.getId())
                .set(CaseInfo::getCurrentHandlerName, dispatcherName);

        int rows = caseInfoMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException("确认失败：案件已被其他同事处理或数据已变更，请刷新后重试");
        }

        CaseInfo updated = getCaseDetail(caseId);
        String opinion = req.getRemark() != null ? req.getRemark() : "";
        String flowRemark = "部门确认处置结果，批转派遣员把关：" + dispatcherName
                + (opinion.isBlank() ? "" : "；" + opinion);
        saveFlowRecord(caseId, updated.getCaseCode(), CaseStatusConstant.PENDING_CHECK, "部门确认并批转",
                CaseFlowOperateType.FORWARD, flowRemark, operatorId, operatorName,
                dispatcher.getId(), dispatcherName);

        notifyUserTask(dispatcher.getId(), "新案件待把关",
                "案件 " + updated.getCaseCode() + " 待派遣员批转受理员",
                BIZ_CASE, updated.getId(), updated.getCaseCode());
        caseTimerService.onCaseHandleFinished(updated.getId(), LocalDateTime.now());
        return updated;
    }

    @Override
    @Transactional
    public CaseInfo dispatcherForwardToAcceptor(CaseDispatcherForwardRequest req, Long operatorId,
                                                  String operatorName, List<String> operatorRoles) {
        Long caseId = req.getCaseId();
        CaseInfo loaded = getCaseDetail(caseId);
        if (!CaseStatusConstant.PENDING_CHECK.equals(loaded.getCaseStatus())) {
            throw new BusinessException("案件状态不正确，无法批转受理员");
        }
        assertDispatcherForwardOperator(loaded, operatorId, operatorRoles);
        assertDispatcherCanForwardAcceptor(loaded);

        SysUser acceptor = resolveAcceptorUser(req.getAcceptorUserId());
        String acceptorName = displayUserName(acceptor);

        LambdaUpdateWrapper<CaseInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(CaseInfo::getId, caseId)
                .eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_CHECK)
                .eq(CaseInfo::getCurrentHandlerId, operatorId)
                .eq(CaseInfo::getIsDeleted, 0);
        if (req.getClientUpdateTime() != null) {
            uw.eq(CaseInfo::getUpdateTime, req.getClientUpdateTime());
        }
        uw.set(CaseInfo::getCurrentHandlerId, acceptor.getId())
                .set(CaseInfo::getCurrentHandlerName, acceptorName);

        int rows = caseInfoMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException("批转失败：案件已被其他同事处理或数据已变更，请刷新后重试");
        }

        CaseInfo updated = getCaseDetail(caseId);
        String opinion = req.getRemark() != null ? req.getRemark() : "";
        String flowRemark = "派遣员把关通过，批转受理员：" + acceptorName
                + (opinion.isBlank() ? "" : "；" + opinion);
        saveFlowRecord(caseId, updated.getCaseCode(), CaseStatusConstant.PENDING_CHECK, "批转受理员",
                CaseFlowOperateType.FORWARD, flowRemark, operatorId, operatorName,
                acceptor.getId(), acceptorName);

        notifyUserTask(acceptor.getId(), "新案件待结案",
                "案件 " + updated.getCaseCode() + " 已批转给您，请核实或结案",
                BIZ_CASE, updated.getId(), updated.getCaseCode());
        caseTimerService.onCaseAcceptorPendingClose(updated.getId(), updated.getCaseCode(), LocalDateTime.now());
        return updated;
    }

    @Override
    @Transactional
    public CaseInfo deptReturnCase(CaseDeptReturnRequest req, Long operatorId, String operatorName,
                                   List<String> operatorRoles) {
        Long caseId = req.getCaseId();
        CaseInfo loaded = getCaseDetail(caseId);
        caseAdjustmentService.assertCaseOperable(loaded);
        if (!CaseStatusConstant.PENDING_HANDLE.equals(loaded.getCaseStatus())) {
            throw new BusinessException("仅「待指派」状态可回退至派遣员");
        }
        if (loaded.getCurrentHandlerId() != null) {
            throw new BusinessException("已指派处置人员，请先「撤销指派」再回退至派遣员");
        }
        assertHandlerDeptOperator(loaded, operatorId, operatorRoles);

        Long dispatcherId = resolveCaseDispatchOperatorId(caseId);
        if (dispatcherId == null) {
            throw new BusinessException("未找到原派遣员，无法回退");
        }
        SysUser dispatcher = sysUserMapper.selectById(dispatcherId);
        if (dispatcher == null || dispatcher.getStatus() == null || dispatcher.getStatus() != 1) {
            throw new BusinessException("原派遣员账号不存在或已停用");
        }
        List<String> dispatcherRoles = sysUserMapper.selectRoleCodesByUserId(dispatcherId);
        if (dispatcherRoles == null || !dispatcherRoles.contains(ROLE_DISPATCHER)) {
            throw new BusinessException("原处理人不是派遣员，无法回退");
        }
        String dispatcherName = displayUserName(dispatcher);

        LambdaUpdateWrapper<CaseInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(CaseInfo::getId, caseId)
                .eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_HANDLE)
                .isNull(CaseInfo::getCurrentHandlerId)
                .eq(CaseInfo::getIsDeleted, 0);
        if (req.getClientUpdateTime() != null) {
            uw.eq(CaseInfo::getUpdateTime, req.getClientUpdateTime());
        }
        uw.set(CaseInfo::getCaseStatus, CaseStatusConstant.RETURNED)
                .set(CaseInfo::getCurrentHandlerId, dispatcherId)
                .set(CaseInfo::getCurrentHandlerName, dispatcherName)
                .set(CaseInfo::getCurrentDeptId, dispatcher.getDepartmentId())
                .set(CaseInfo::getCurrentDeptName, dispatcher.getDepartmentName());

        int rows = caseInfoMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException("回退失败：案件已被处理或数据已变更，请刷新后重试");
        }

        CaseInfo updated = getCaseDetail(caseId);
        String opinion = req.getRemark() != null ? req.getRemark() : "";
        String flowRemark = "部门回退至派遣员：" + dispatcherName
                + (opinion.isBlank() ? "" : "；" + opinion);
        saveFlowRecord(caseId, updated.getCaseCode(), CaseStatusConstant.RETURNED, "部门回退",
                CaseFlowOperateType.RETURN, flowRemark, operatorId, operatorName,
                dispatcherId, dispatcherName);

        return updated;
    }

    @Override
    @Transactional
    public CaseInfo deptRevokeAssign(CaseRevokeAssignRequest req, Long operatorId, String operatorName,
                                     List<String> operatorRoles) {
        Long caseId = req.getCaseId();
        CaseInfo loaded = getCaseDetail(caseId);
        caseAdjustmentService.assertCaseOperable(loaded);
        if (!CaseStatusConstant.HANDLING.equals(loaded.getCaseStatus())) {
            throw new BusinessException("仅「处置中」状态可撤销指派");
        }
        if (loaded.getCurrentHandlerId() == null) {
            throw new BusinessException("案件未指派处置人员");
        }
        assertHandlerDeptOperator(loaded, operatorId, operatorRoles);

        Long handlerId = loaded.getCurrentHandlerId();
        String handlerName = loaded.getCurrentHandlerName();

        LambdaUpdateWrapper<CaseInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(CaseInfo::getId, caseId)
                .eq(CaseInfo::getCaseStatus, CaseStatusConstant.HANDLING)
                .eq(CaseInfo::getCurrentHandlerId, handlerId)
                .eq(CaseInfo::getIsDeleted, 0);
        if (req.getClientUpdateTime() != null) {
            uw.eq(CaseInfo::getUpdateTime, req.getClientUpdateTime());
        }
        uw.set(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_HANDLE)
                .set(CaseInfo::getCurrentHandlerId, null)
                .set(CaseInfo::getCurrentHandlerName, null);

        int rows = caseInfoMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException("撤销指派失败：案件已被处理或数据已变更，请刷新后重试");
        }

        CaseInfo updated = getCaseDetail(caseId);
        String opinion = req.getRemark() != null ? req.getRemark().trim() : "";
        String flowRemark = "撤销指派处置人员：" + handlerName
                + (opinion.isBlank() ? "" : "；" + opinion);
        saveFlowRecord(caseId, updated.getCaseCode(), CaseStatusConstant.PENDING_HANDLE, "撤销指派",
                CaseFlowOperateType.REVOKE_ASSIGN, flowRemark, operatorId, operatorName,
                handlerId, handlerName);

        return updated;
    }

    @Override
    @Transactional
    public CaseInfo deptReturnHandler(CaseReturnRequest req, Long operatorId, String operatorName,
                                      List<String> operatorRoles) {
        Long caseId = req.getCaseId();
        String opinion = requireRemark(req.getRemark());
        CaseInfo loaded = getCaseDetail(caseId);
        if (!CaseStatusConstant.HANDLE_FINISH.equals(loaded.getCaseStatus())) {
            throw new BusinessException("仅「处置人员已处置」状态可打回处置人员");
        }
        assertHandlerDeptOperator(loaded, operatorId, operatorRoles);
        if (loaded.getCurrentHandlerId() == null) {
            throw new BusinessException("未找到处置人员，无法打回");
        }
        Long handlerId = loaded.getCurrentHandlerId();
        String handlerName = loaded.getCurrentHandlerName() != null
                ? loaded.getCurrentHandlerName()
                : resolveOperatorName(handlerId);

        LambdaUpdateWrapper<CaseInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(CaseInfo::getId, caseId)
                .eq(CaseInfo::getCaseStatus, CaseStatusConstant.HANDLE_FINISH)
                .eq(CaseInfo::getIsDeleted, 0);
        if (req.getClientUpdateTime() != null) {
            uw.eq(CaseInfo::getUpdateTime, req.getClientUpdateTime());
        }
        uw.set(CaseInfo::getCaseStatus, CaseStatusConstant.HANDLING)
                .set(CaseInfo::getCurrentHandlerId, handlerId)
                .set(CaseInfo::getCurrentHandlerName, handlerName);

        int rows = caseInfoMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException("打回失败：案件已被处理或数据已变更，请刷新后重试");
        }

        CaseInfo updated = getCaseDetail(caseId);
        String flowRemark = "部门打回处置人员再处置：" + handlerName + "；" + opinion;
        saveFlowRecord(caseId, updated.getCaseCode(), CaseStatusConstant.HANDLING, "部门打回处置人员",
                CaseFlowOperateType.RETURN, flowRemark, operatorId, operatorName,
                handlerId, handlerName);

        return updated;
    }

    @Override
    @Transactional
    public CaseInfo handlerReturnDept(CaseReturnRequest req, Long operatorId, List<String> operatorRoles) {
        Long caseId = req.getCaseId();
        String opinion = requireRemark(req.getRemark());
        CaseInfo loaded = getCaseDetail(caseId);
        caseAdjustmentService.assertCaseOperable(loaded);
        if (!CaseStatusConstant.HANDLING.equals(loaded.getCaseStatus())) {
            throw new BusinessException("仅「处置中」且提交前可回退处置部门");
        }
        assertAssignedHandler(loaded, operatorId, operatorRoles);

        LambdaUpdateWrapper<CaseInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(CaseInfo::getId, caseId)
                .eq(CaseInfo::getCaseStatus, CaseStatusConstant.HANDLING)
                .eq(CaseInfo::getCurrentHandlerId, operatorId)
                .eq(CaseInfo::getIsDeleted, 0);
        if (req.getClientUpdateTime() != null) {
            uw.eq(CaseInfo::getUpdateTime, req.getClientUpdateTime());
        }
        uw.set(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_HANDLE)
                .set(CaseInfo::getCurrentHandlerId, null)
                .set(CaseInfo::getCurrentHandlerName, null);

        int rows = caseInfoMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException("回退失败：案件已被处理或数据已变更，请刷新后重试");
        }

        CaseInfo updated = getCaseDetail(caseId);
        String flowRemark = "处置人员回退至部门：" + opinion;
        saveFlowRecord(caseId, updated.getCaseCode(), CaseStatusConstant.PENDING_HANDLE, "处置人员回退部门",
                CaseFlowOperateType.RETURN, flowRemark, operatorId, resolveOperatorName(operatorId),
                null, updated.getHandleDeptName());

        return updated;
    }

    @Override
    @Transactional
    public CaseInfo dispatcherReturnAcceptor(CaseDispatcherReturnAcceptorRequest req, Long operatorId,
                                             String operatorName, List<String> operatorRoles) {
        Long caseId = req.getCaseId();
        String opinion = requireRemark(req.getRemark());
        CaseInfo loaded = getCaseDetail(caseId);
        if (!CaseStatusConstant.PENDING_DISPATCH.equals(loaded.getCaseStatus())) {
            throw new BusinessException("仅「待派遣」状态可回退受理员");
        }
        assertDispatcherAssignee(loaded, operatorId, operatorRoles);

        SysUser acceptor = resolveReturnAcceptorUser(loaded, req.getAcceptorUserId());
        String acceptorName = displayUserName(acceptor);

        LambdaUpdateWrapper<CaseInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(CaseInfo::getId, caseId)
                .eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_DISPATCH)
                .eq(CaseInfo::getIsDeleted, 0);
        if (req.getClientUpdateTime() != null) {
            uw.eq(CaseInfo::getUpdateTime, req.getClientUpdateTime());
        }
        if (!canViewAllPending(operatorRoles) && operatorId != null) {
            uw.eq(CaseInfo::getCurrentHandlerId, operatorId);
        }
        uw.set(CaseInfo::getCaseStatus, CaseStatusConstant.RETURNED)
                .set(CaseInfo::getCurrentHandlerId, acceptor.getId())
                .set(CaseInfo::getCurrentHandlerName, acceptorName);

        int rows = caseInfoMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException("回退失败：案件已被其他同事处理或数据已变更，请刷新后重试");
        }

        CaseInfo updated = getCaseDetail(caseId);
        String flowRemark = "派遣员回退受理员（非本局）：" + acceptorName + "；" + opinion;
        saveFlowRecord(caseId, updated.getCaseCode(), CaseStatusConstant.RETURNED, "派遣员回退受理员",
                CaseFlowOperateType.RETURN, flowRemark, operatorId, operatorName,
                acceptor.getId(), acceptorName);

        return updated;
    }

    @Override
    @Transactional
    public CaseInfo dispatcherReturnDept(CaseReturnRequest req, Long operatorId, String operatorName,
                                         List<String> operatorRoles) {
        Long caseId = req.getCaseId();
        String opinion = requireRemark(req.getRemark());
        CaseInfo loaded = getCaseDetail(caseId);
        if (!CaseStatusConstant.PENDING_CHECK.equals(loaded.getCaseStatus())) {
            throw new BusinessException("仅「待核查/反馈」状态可由派遣员打回处置部门返工");
        }
        assertDispatcherForwardOperator(loaded, operatorId, operatorRoles);
        if (isAwaitingDeptConfirm(loaded)) {
            throw new BusinessException("案件待处置部门确认，请等待部门批转后再操作");
        }
        if (loaded.getHandleDeptId() == null) {
            throw new BusinessException("案件未关联处置部门，无法返工");
        }

        LambdaUpdateWrapper<CaseInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(CaseInfo::getId, caseId)
                .eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_CHECK)
                .eq(CaseInfo::getIsDeleted, 0);
        if (req.getClientUpdateTime() != null) {
            uw.eq(CaseInfo::getUpdateTime, req.getClientUpdateTime());
        }
        if (!canViewAllPending(operatorRoles) && operatorId != null) {
            uw.eq(CaseInfo::getCurrentHandlerId, operatorId);
        }
        uw.set(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_HANDLE)
                .set(CaseInfo::getCurrentHandlerId, null)
                .set(CaseInfo::getCurrentHandlerName, null);

        int rows = caseInfoMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException("返工失败：案件已被其他同事处理或数据已变更，请刷新后重试");
        }

        CaseInfo updated = getCaseDetail(caseId);
        String deptName = updated.getHandleDeptName() != null ? updated.getHandleDeptName() : "处置部门";
        String flowRemark = "派遣员打回处置部门返工：" + deptName + "；" + opinion;
        saveFlowRecord(caseId, updated.getCaseCode(), CaseStatusConstant.PENDING_HANDLE, "派遣员返工部门",
                CaseFlowOperateType.RETURN, flowRemark, operatorId, operatorName,
                null, deptName);

        return updated;
    }

    @Override
    @Transactional
    public CaseInfo acceptorReturnDispatcher(CaseAcceptorReturnDispatcherRequest req, Long operatorId,
                                             String operatorName, List<String> operatorRoles) {
        Long caseId = req.getCaseId();
        String opinion = requireRemark(req.getRemark());
        CaseInfo loaded = getCaseDetail(caseId);
        if (!CaseStatusConstant.PENDING_CHECK.equals(loaded.getCaseStatus())) {
            throw new BusinessException("案件状态不正确，无法回退派遣员返工");
        }
        assertAcceptorCheckCloseOperator(loaded, operatorId, operatorRoles);
        if (loaded.getHandleDeptId() == null) {
            throw new BusinessException("案件未关联处置部门，无法返工");
        }

        SysUser dispatcher = resolveDispatcherUser(req.getDispatcherUserId());

        LambdaUpdateWrapper<CaseInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(CaseInfo::getId, caseId)
                .eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_CHECK)
                .eq(CaseInfo::getCurrentHandlerId, operatorId)
                .eq(CaseInfo::getIsDeleted, 0);
        if (req.getClientUpdateTime() != null) {
            uw.eq(CaseInfo::getUpdateTime, req.getClientUpdateTime());
        }
        uw.set(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_HANDLE)
                .set(CaseInfo::getCurrentHandlerId, dispatcher.getId())
                .set(CaseInfo::getCurrentHandlerName, displayUserName(dispatcher));

        int rows = caseInfoMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException("回退失败：案件已被其他同事处理或数据已变更，请刷新后重试");
        }

        CaseInfo updated = getCaseDetail(caseId);
        String dispatcherName = displayUserName(dispatcher);
        String deptName = updated.getHandleDeptName() != null ? updated.getHandleDeptName() : "原处置部门";
        String flowRemark = "受理员认定处置不达标，回退派遣员返工：" + dispatcherName
                + " → " + deptName + "；" + opinion;
        saveFlowRecord(caseId, updated.getCaseCode(), CaseStatusConstant.PENDING_HANDLE, "受理员回退返工",
                CaseFlowOperateType.RETURN, flowRemark, operatorId, operatorName,
                dispatcher.getId(), dispatcherName);

        return updated;
    }

    @Override
    public List<CollectorCandidateDto> listCollectorCandidates(Long caseId, Long userId, List<String> roles) {
        CaseInfo caseInfo = getCaseDetail(caseId, userId, roles);
        return buildCollectorCandidates(caseInfo);
    }

    @Override
    @Transactional
    public CaseInfo sendCheckTask(CaseSendTaskRequest request, Long operatorId, String operatorName,
                                  List<String> operatorRoles) {
        if (request == null || request.getCaseId() == null) {
            throw new BusinessException("案件ID不能为空");
        }
        CaseInfo caseInfo = getCaseDetail(request.getCaseId());
        String st = caseInfo.getCaseStatus();
        if (!CaseStatusConstant.PENDING_REGISTER.equals(st)
                && !CaseStatusConstant.REPORTED.equals(st)
                && !CaseStatusConstant.RETURNED.equals(st)) {
            throw new BusinessException("仅待立案阶段可下发核查（可选分支），当前状态：" + st);
        }
        if (CaseStatusConstant.CHECKING.equals(st) || taskService.hasPendingCheckTask(caseInfo.getId())) {
            throw new BusinessException("已有进行中的核查任务，请等待采集员完成");
        }
        assertAcceptorCanOperateCase(caseInfo, operatorId);

        SysUser collector = resolveCollectorForCase(caseInfo, request.getCollectorUserId());
        CheckTask task = new CheckTask();
        task.setTaskCode(generateTaskCode("HC"));
        task.setCaseId(caseInfo.getId());
        task.setCaseCode(caseInfo.getCaseCode());
        task.setSmallName(caseInfo.getSmallName());
        task.setAddress(caseInfo.getAddress());
        task.setLongitude(caseInfo.getLongitude());
        task.setLatitude(caseInfo.getLatitude());
        task.setTaskStatus(TaskStatusConstant.PENDING);
        task.setAssignTime(LocalDateTime.now());
        task.setDeadlineTime(caseTimerService.taskDeadline(task.getAssignTime()));
        task.setCollectorId(collector.getId());
        task.setCollectorName(displayUserName(collector));
        task.setCollectorPhone(collector.getPhone());
        task.setAssignerId(operatorId);
        String operatorDisplayName = resolveOperatorName(operatorId);
        task.setAssignerName(operatorDisplayName);
        if (request.getRemark() != null && !request.getRemark().isBlank()) {
            task.setAssignRemark(request.getRemark().trim());
        }
        checkTaskMapper.insert(task);
        caseTimerService.onCheckTaskAssigned(caseInfo.getId(), caseInfo.getCaseCode(), task.getAssignTime());

        caseInfo.setCaseStatus(CaseStatusConstant.CHECKING);
        caseInfo.setCheckTaskId(task.getId());
        if (caseInfo.getRegisterOperatorId() == null && operatorId != null) {
            caseInfo.setRegisterOperatorId(operatorId);
            caseInfo.setRegisterOperatorName(operatorDisplayName);
        }
        caseInfoMapper.updateById(caseInfo);

        String remark = request.getRemark() != null ? request.getRemark() : "受理员下发核查（可选）";
        saveFlowRecord(caseInfo.getId(), caseInfo.getCaseCode(), CaseStatusConstant.CHECKING, "下发核查",
                CaseFlowOperateType.ASSIGN, remark,
                operatorId, operatorName, collector.getId(), task.getCollectorName());
        notifyUserTask(collector.getId(), "新核查任务",
                "案件 " + caseInfo.getCaseCode() + "，请现场核查",
                BIZ_CASE, caseInfo.getId(), caseInfo.getCaseCode());
        return getCaseDetail(caseInfo.getId(), operatorId, operatorRoles);
    }

    @Override
    @Transactional
    public CaseInfo sendVerifyTask(CaseSendTaskRequest request, Long operatorId, String operatorName,
                                   List<String> operatorRoles) {
        if (request == null || request.getCaseId() == null) {
            throw new BusinessException("案件ID不能为空");
        }
        CaseInfo caseInfo = getCaseDetail(request.getCaseId());
        if (!CaseStatusConstant.PENDING_CHECK.equals(caseInfo.getCaseStatus())) {
            throw new BusinessException("仅待结案阶段可下发核实（可选分支），当前状态：" + caseInfo.getCaseStatus());
        }
        assertAcceptorCheckCloseOperator(caseInfo, operatorId, operatorRoles);
        if (taskService.hasPendingVerifyTask(caseInfo.getId())) {
            throw new BusinessException("已有进行中的核实任务，请等待采集员完成");
        }

        SysUser collector = resolveCollectorForCase(caseInfo, request.getCollectorUserId());
        VerifyTask task = new VerifyTask();
        task.setTaskCode(generateTaskCode("HV"));
        task.setCaseId(caseInfo.getId());
        task.setCaseCode(caseInfo.getCaseCode());
        task.setSourceType(caseInfo.getSourceType() != null ? caseInfo.getSourceType() : "collector");
        task.setBigCode(caseInfo.getBigCode());
        task.setBigName(caseInfo.getBigName());
        task.setSmallCode(caseInfo.getSmallCode());
        task.setSmallName(caseInfo.getSmallName());
        task.setDescription(caseInfo.getDescription());
        task.setLongitude(caseInfo.getLongitude());
        task.setLatitude(caseInfo.getLatitude());
        task.setAddress(caseInfo.getAddress());
        task.setRespGridId(caseInfo.getRespGridId());
        task.setTaskStatus(TaskStatusConstant.PENDING);
        task.setAssignTime(LocalDateTime.now());
        task.setDeadlineTime(caseTimerService.taskDeadline(task.getAssignTime()));
        task.setCollectorId(collector.getId());
        task.setCollectorName(displayUserName(collector));
        task.setCollectorPhone(collector.getPhone());
        task.setCreatorId(operatorId);
        task.setCreatorName(resolveOperatorName(operatorId));
        if (request.getRemark() != null && !request.getRemark().isBlank()) {
            task.setAssignRemark(request.getRemark().trim());
        }
        verifyTaskMapper.insert(task);
        ensureAcceptCloseTimerBeforeVerify(caseInfo);
        caseTimerService.onVerifyTaskAssigned(caseInfo.getId(), caseInfo.getCaseCode(), task.getAssignTime());

        caseInfo.setVerifyTaskId(task.getId());
        caseInfoMapper.updateById(caseInfo);

        String remark = request.getRemark() != null ? request.getRemark() : "受理员下发核实（可选）";
        saveFlowRecord(caseInfo.getId(), caseInfo.getCaseCode(), CaseStatusConstant.PENDING_CHECK, "下发核实",
                CaseFlowOperateType.ASSIGN, remark,
                operatorId, operatorName, collector.getId(), task.getCollectorName());
        notifyUserTask(collector.getId(), "新核实任务",
                "案件 " + caseInfo.getCaseCode() + "，请现场核实",
                BIZ_CASE, caseInfo.getId(), caseInfo.getCaseCode());
        return getCaseDetail(caseInfo.getId(), operatorId, operatorRoles);
    }

    @Override
    @Transactional
    public CaseInfo verifyCase(Long caseId, Integer result, String remark, List<String> attachments) {
        throw new BusinessException("请使用「发送核查」下发采集员任务（可选分支），完成后在待立案中立案或作废");
    }

    @Override
    @Transactional
    public CaseInfo checkCase(Long caseId, Integer result, String remark, List<String> attachments,
                              Long operatorId, List<String> operatorRoles) {
        throw new BusinessException("请使用「结案」或「回退派遣员返工」；现场核实任务请通过「发送核实」下发采集员（P1）");
    }

    @Override
    @Transactional
    public CaseInfo closeCase(Long caseId, String remark, Long operatorId, List<String> operatorRoles) {
        CaseInfo caseInfo = getCaseDetail(caseId);
        if (CaseStatusConstant.CLOSED.equals(caseInfo.getCaseStatus())) {
            throw new BusinessException("案件已结案");
        }
        if (CaseStatusConstant.NOT_ACCEPTED.equals(caseInfo.getCaseStatus())) {
            throw new BusinessException("已作废案件不可结案");
        }
        assertNotDeptOperatorForCheckClose(operatorRoles);
        assertDispatcherCannotCheckClose(operatorRoles);
        if (CaseStatusConstant.PENDING_CHECK.equals(caseInfo.getCaseStatus())) {
            assertAcceptorCheckCloseOperator(caseInfo, operatorId, operatorRoles);
        }
        if (caseInfo.getStandardId() == null) {
            throw new BusinessException("案件未关联立结案标准，无法结案");
        }
        if (taskService.hasPendingVerifyTask(caseId)) {
            throw new BusinessException("核实任务进行中，请等待采集员完成；如无需现场核实可直接结案");
        }
        caseInfo.setCaseStatus(CaseStatusConstant.CLOSED);
        caseInfo.setCloseTime(LocalDateTime.now());
        caseInfoMapper.updateById(caseInfo);

        saveFlowRecord(caseId, caseInfo.getCaseCode(), CaseStatusConstant.CLOSED, "结案",
                CaseFlowOperateType.CLOSE, remark != null ? remark : "",
                operatorId, resolveOperatorName(operatorId), null, null);
        caseTimerService.onCaseClosed(caseId, caseInfo.getCloseTime());

        return caseInfo;
    }

    private void ensureAcceptCloseTimerBeforeVerify(CaseInfo caseInfo) {
        if (caseInfo == null || caseInfo.getId() == null) {
            return;
        }
        caseTimerService.onCaseAcceptorPendingClose(
                caseInfo.getId(), caseInfo.getCaseCode(), LocalDateTime.now());
    }

    @Override
    @Transactional
    public CaseInfo rejectCase(Long caseId, String reason, Long operatorId, String operatorName) {
        CaseInfo loaded = getCaseDetail(caseId);
        String rs = loaded.getCaseStatus() != null ? loaded.getCaseStatus().trim() : null;
        if (!CaseStatusConstant.PENDING_VERIFY.equals(rs)
                && !CaseStatusConstant.PENDING_REGISTER.equals(rs)
                && !CaseStatusConstant.REPORTED.equals(rs)) {
            throw new BusinessException("案件状态不正确，无法作废");
        }
        assertAcceptorCanOperateCase(loaded, operatorId);

        LambdaUpdateWrapper<CaseInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(CaseInfo::getId, caseId)
                .in(CaseInfo::getCaseStatus,
                        CaseStatusConstant.PENDING_VERIFY,
                        CaseStatusConstant.PENDING_REGISTER,
                        CaseStatusConstant.REPORTED)
                .eq(CaseInfo::getIsDeleted, 0)
                .and(w -> w.isNull(CaseInfo::getRegisterOperatorId)
                        .or()
                        .eq(CaseInfo::getRegisterOperatorId, operatorId))
                .set(CaseInfo::getCaseStatus, CaseStatusConstant.NOT_ACCEPTED)
                .set(CaseInfo::getRemark, reason)
                .set(CaseInfo::getRegisterOperatorId, operatorId)
                .set(CaseInfo::getRegisterOperatorName, resolveOperatorName(operatorId));

        int rows = caseInfoMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException("作废失败：案件已被其他受理员接手或状态已变更，请刷新后重试");
        }

        CaseInfo updated = getCaseDetail(caseId);
        saveFlowRecord(caseId, updated.getCaseCode(), CaseStatusConstant.NOT_ACCEPTED, "作废",
                CaseFlowOperateType.CANCEL, reason, operatorId, operatorName, null, null);

        return updated;
    }

    @Override
    public List<CaseFlowRecord> getFlowRecords(Long caseId) {
        List<CaseFlowRecord> flows = flowRecordMapper.selectByCaseId(caseId);
        for (CaseFlowRecord flow : flows) {
            enrichFlowRecordDisplay(flow);
        }
        return flows;
    }

    @Override
    public List<CaseAttachment> getAttachments(Long caseId) {
        return attachmentMapper.selectByCaseId(caseId);
    }

    @Override
    public List<com.cityguard.task.dto.CheckTaskRecordView> listCheckTaskRecords(
            Long caseId, Long userId, List<String> roles) {
        getCaseDetail(caseId, userId, roles);
        return taskService.listCheckTaskRecordsByCaseId(caseId);
    }

    @Override
    public List<com.cityguard.task.dto.VerifyTaskRecordView> listVerifyTaskRecords(
            Long caseId, Long userId, List<String> roles) {
        getCaseDetail(caseId, userId, roles);
        return taskService.listVerifyTaskRecordsByCaseId(caseId);
    }

    @Override
    @Transactional
    public int deleteCases(List<Long> caseIds, List<String> operatorRoles) {
        assertAdminForDelete(operatorRoles);
        if (caseIds == null || caseIds.isEmpty()) {
            throw new BusinessException("请选择要删除的案件");
        }
        List<Long> distinctIds = caseIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new BusinessException("请选择要删除的案件");
        }
        int deleted = 0;
        for (Long caseId : distinctIds) {
            CaseInfo existing = caseInfoMapper.selectById(caseId);
            if (existing == null) {
                continue;
            }
            int rows = caseInfoMapper.deleteById(caseId);
            if (rows > 0) {
                deleted++;
            }
        }
        if (deleted == 0) {
            throw new BusinessException("未找到可删除的案件，可能已被删除");
        }
        return deleted;
    }

    private static boolean canViewAllPending(List<String> roles) {
        return roles != null && (roles.contains(ROLE_ADMIN) || roles.contains(ROLE_SUPERVISOR));
    }

    /** 综合查询全库角色：管理员、值班长、领导 */
    private static boolean canQueryAllCases(List<String> roles) {
        return roles != null && (roles.contains(ROLE_ADMIN) || roles.contains(ROLE_SUPERVISOR)
                || roles.contains(ROLE_LEADER));
    }

    private static void assertCaseQueryRole(List<String> roles) {
        if (roles == null) {
            throw new BusinessException("无权使用综合查询");
        }
        if (canQueryAllCases(roles)) {
            return;
        }
        if (roles.contains(ROLE_ACCEPTOR) || roles.contains(ROLE_DISPATCHER)) {
            return;
        }
        throw new BusinessException("无权使用综合查询");
    }

    private void applyComprehensiveQueryRoleScope(LambdaQueryWrapper<CaseInfo> wrapper, Long userId,
                                                  List<String> roles) {
        if (userId == null || roles == null || canQueryAllCases(roles)) {
            return;
        }
        if (roles.contains(ROLE_DISPATCHER)) {
            applyDispatcherCaseScope(wrapper, userId);
            return;
        }
        if (roles.contains(ROLE_ACCEPTOR)) {
            applyAcceptorCaseScope(wrapper, userId);
        }
    }

    private void applyCaseQueryCriteria(LambdaQueryWrapper<CaseInfo> wrapper, CaseQueryCriteria q) {
        if (q.getCaseCode() != null && !q.getCaseCode().isBlank()) {
            String code = q.getCaseCode().trim();
            String match = q.getCaseCodeMatch() != null ? q.getCaseCodeMatch().trim() : "exact";
            if ("prefix".equalsIgnoreCase(match)) {
                wrapper.likeRight(CaseInfo::getCaseCode, code);
            } else {
                wrapper.eq(CaseInfo::getCaseCode, code);
            }
        }
        applyDateFilter(wrapper, CaseInfo::getReportTime, q.getReportTime(), false);
        applyDateFilter(wrapper, CaseInfo::getCloseTime, q.getCloseTime(), true);
        com.cityguard.caseinfo.support.CaseQueryFilterSupport.applyCommonFilters(wrapper, q);
        if (q.getRespGridIds() != null && !q.getRespGridIds().isEmpty()) {
            wrapper.in(CaseInfo::getRespGridId, q.getRespGridIds());
        }
        if (q.getCaseStatuses() != null && !q.getCaseStatuses().isEmpty()) {
            wrapper.in(CaseInfo::getCaseStatus, q.getCaseStatuses());
        }
        if (q.getSmallIds() != null && !q.getSmallIds().isEmpty()) {
            wrapper.in(CaseInfo::getSmallId, q.getSmallIds());
        }
        if (q.getHandleDeptId() != null) {
            wrapper.eq(CaseInfo::getHandleDeptId, q.getHandleDeptId());
        }
        if (q.getReporterId() != null) {
            wrapper.eq(CaseInfo::getReporterId, q.getReporterId());
        }
        if (q.getRegisterOperatorId() != null) {
            wrapper.eq(CaseInfo::getRegisterOperatorId, q.getRegisterOperatorId());
        }
        if (q.getDispatchOperatorId() != null) {
            wrapper.eq(CaseInfo::getDispatchOperatorId, q.getDispatchOperatorId());
        }
        if (q.getAddress() != null && !q.getAddress().isBlank()) {
            String addr = q.getAddress().trim();
            String match = q.getAddressMatch() != null ? q.getAddressMatch().trim() : "contains";
            if ("eq".equalsIgnoreCase(match)) {
                wrapper.eq(CaseInfo::getAddress, addr);
            } else {
                wrapper.like(CaseInfo::getAddress, addr);
            }
        }
    }

    private void applyDateFilter(LambdaQueryWrapper<CaseInfo> wrapper,
                                 com.baomidou.mybatisplus.core.toolkit.support.SFunction<CaseInfo, LocalDateTime> column,
                                 CaseDateFilter filter,
                                 boolean requireNonNullForEq) {
        if (filter == null || filter.getOp() == null || filter.getOp().isBlank()) {
            return;
        }
        String op = filter.getOp().trim().toLowerCase(Locale.ROOT);
        if ("between".equals(op)) {
            LocalDateTime start = parseDateTimeParam(filter.getStart(), false);
            LocalDateTime end = parseDateTimeParam(filter.getEnd(), true);
            if (start != null) {
                wrapper.ge(column, start);
            }
            if (end != null) {
                wrapper.le(column, end);
            }
            return;
        }
        LocalDateTime dayStart = parseDateTimeParam(filter.getStart(), false);
        LocalDateTime dayEnd = parseDateTimeParam(filter.getStart(), true);
        switch (op) {
            case "eq" -> {
                if (dayStart == null) {
                    return;
                }
                if (requireNonNullForEq) {
                    wrapper.isNotNull(column);
                }
                wrapper.ge(column, dayStart);
                if (dayEnd != null) {
                    wrapper.le(column, dayEnd);
                }
            }
            case "gt" -> {
                if (dayEnd != null) {
                    wrapper.gt(column, dayEnd);
                }
            }
            case "lt" -> {
                if (dayStart != null) {
                    wrapper.lt(column, dayStart);
                }
            }
            default -> {
            }
        }
    }

    /**
     * 案件列表按角色收窄：DEPT → 本部门；派遣员 → 本人经办/参与；受理员 → 公共池 + 本人立案
     */
    private void applyCaseListRoleScope(LambdaQueryWrapper<CaseInfo> wrapper, Long userId, List<String> roles) {
        if (userId == null || roles == null || canViewAllPending(roles)) {
            return;
        }
        if (roles.contains(ROLE_DEPT)) {
            Long deptId = resolveUserDepartmentId(userId);
            wrapper.eq(CaseInfo::getHandleDeptId, deptId);
            return;
        }
        if (roles.contains(ROLE_DISPATCHER)) {
            applyDispatcherCaseScope(wrapper, userId);
            return;
        }
        if (roles.contains(ROLE_ACCEPTOR)) {
            applyAcceptorCaseScope(wrapper, userId);
        }
    }

    private void assertCaseReadScope(CaseInfo caseInfo, Long userId, List<String> roles) {
        if (userId == null || roles == null || canViewAllPending(roles)) {
            return;
        }
        if (roles.contains(ROLE_DISPATCHER)) {
            if (!canDispatcherViewCase(caseInfo, userId)) {
                throw new BusinessException("您无权查看该案件");
            }
            return;
        }
        if (roles.contains(ROLE_ACCEPTOR)) {
            if (!canAcceptorViewCase(caseInfo, userId)) {
                throw new BusinessException("您无权查看该案件");
            }
            return;
        }
        if (roles.contains(ROLE_DEPT)) {
            Long deptId = resolveUserDepartmentId(userId);
            if (caseInfo.getHandleDeptId() == null || !caseInfo.getHandleDeptId().equals(deptId)) {
                throw new BusinessException("您无权查看该案件");
            }
        }
    }

    private void assertAdminForDelete(List<String> operatorRoles) {
        if (operatorRoles == null || !operatorRoles.contains(ROLE_ADMIN)) {
            throw new BusinessException("仅管理员可删除案件");
        }
    }

    /** 处置部门账号不能核实、结案 */
    private void assertNotDeptOperatorForCheckClose(List<String> operatorRoles) {
        if (operatorRoles == null || canViewAllPending(operatorRoles)) {
            return;
        }
        if (operatorRoles.contains(ROLE_DEPT)
                && !operatorRoles.contains(ROLE_DISPATCHER)
                && !operatorRoles.contains(ROLE_ADMIN)
                && !operatorRoles.contains(ROLE_SUPERVISOR)) {
            throw new BusinessException("处置部门账号不能核实或结案，请由受理员结案");
        }
    }

    /** 派遣员仅把关批转，不能核实/结案 */
    private void assertDispatcherCannotCheckClose(List<String> operatorRoles) {
        if (operatorRoles == null || canViewAllPending(operatorRoles)) {
            return;
        }
        if (operatorRoles.contains(ROLE_DISPATCHER)
                && !operatorRoles.contains(ROLE_ACCEPTOR)) {
            throw new BusinessException("派遣员不能核实或结案，请批转受理员岗后由受理员结案");
        }
    }

    private void assertDispatcherForwardOperator(CaseInfo caseInfo, Long operatorId, List<String> operatorRoles) {
        if (canViewAllPending(operatorRoles)) {
            return;
        }
        if (operatorRoles == null || !operatorRoles.contains(ROLE_DISPATCHER)) {
            throw new BusinessException("仅派遣员可批转受理员");
        }
        if (caseInfo.getCurrentHandlerId() == null || !caseInfo.getCurrentHandlerId().equals(operatorId)) {
            throw new BusinessException("该案件已批转给其他派遣员，您无权操作");
        }
    }

    /** 核实/结案：仅被派遣员批转指定的受理员（current_handler）可操作；立案人仅可查看 */
    private void assertAcceptorCheckCloseOperator(CaseInfo caseInfo, Long operatorId, List<String> operatorRoles) {
        if (canViewAllPending(operatorRoles)) {
            return;
        }
        if (operatorRoles == null || !operatorRoles.contains(ROLE_ACCEPTOR)) {
            throw new BusinessException("仅受理员可核实或结案");
        }
        if (isAwaitingDispatcherForward(caseInfo)) {
            throw new BusinessException("案件尚在派遣员把关，请等待批转受理员岗");
        }
        if (isAwaitingDeptConfirm(caseInfo)) {
            throw new BusinessException("案件待处置部门确认，暂不能核实或结案");
        }
        if (!CaseStatusConstant.PENDING_CHECK.equals(caseInfo.getCaseStatus())) {
            throw new BusinessException("案件状态不正确，无法核实或结案");
        }
        if (caseInfo.getCurrentHandlerId() == null
                || !caseInfo.getCurrentHandlerId().equals(operatorId)) {
            throw new BusinessException("仅被批转的受理员可核实或结案，立案人请在我立案的案件中查看进度");
        }
    }

    /**
     * 待核实案件：上报待核实/核实任务 + 批转后待核查（核实操作，与待我结案分菜单）
     */
    private void applyAcceptorPendingVerifyScope(LambdaQueryWrapper<CaseInfo> wrapper, Long userId,
                                                 List<String> roles) {
        if (userId != null && roles != null && roles.contains(ROLE_ACCEPTOR) && !canViewAllPending(roles)) {
            wrapper.and(w -> w
                    .nested(early -> early.and(ev -> ev
                                    .eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_VERIFY)
                                    .or()
                                    .apply("id IN (SELECT case_id FROM verify_task WHERE is_deleted = 0 "
                                            + "AND (finish_time IS NULL OR task_status IN ('pending','PENDING',"
                                            + "'assigned','ASSIGNED')))"))
                            .and(sub -> sub.isNull(CaseInfo::getRegisterOperatorId)
                                    .or()
                                    .eq(CaseInfo::getRegisterOperatorId, userId)))
                    .or()
                    .nested(late -> late.eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_CHECK)
                            .eq(CaseInfo::getCurrentHandlerId, userId)));
            return;
        }
        wrapper.and(w -> w.eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_VERIFY)
                .or()
                .apply("id IN (SELECT case_id FROM verify_task WHERE is_deleted = 0 "
                        + "AND (finish_time IS NULL OR task_status IN ('pending','PENDING','assigned','ASSIGNED')))"));
    }

    /**
     * 派遣员案件范围：当前处理人为本人，或流程中本人曾操作（派遣、批转等）
     */
    private void applyDispatcherCaseScope(LambdaQueryWrapper<CaseInfo> wrapper, Long userId) {
        if (userId == null) {
            wrapper.eq(CaseInfo::getId, -1L);
            return;
        }
        wrapper.and(w -> w.eq(CaseInfo::getCurrentHandlerId, userId)
                .or()
                .apply("id IN (SELECT DISTINCT case_id FROM case_flow_record WHERE operator_id = {0})", userId));
    }

    private boolean canDispatcherViewCase(CaseInfo caseInfo, Long userId) {
        if (caseInfo == null || userId == null) {
            return false;
        }
        if (userId.equals(caseInfo.getCurrentHandlerId())) {
            return true;
        }
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM case_flow_record WHERE case_id = ? AND operator_id = ?",
                Long.class,
                caseInfo.getId(),
                userId);
        return count != null && count > 0;
    }

    /** 待我结案案件：批转后待核查，仅结案（核实见「待核实案件」） */
    private void applyAcceptorPendingCloseScope(LambdaQueryWrapper<CaseInfo> wrapper, Long userId,
                                                List<String> roles) {
        wrapper.eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_CHECK);
        if (userId == null || roles == null || !roles.contains(ROLE_ACCEPTOR) || canViewAllPending(roles)) {
            return;
        }
        wrapper.eq(CaseInfo::getCurrentHandlerId, userId);
    }

    /** 我立案的案件：立案人全程可查（含已批转他人收尾、已结案）；不含作废 */
    private void applyAcceptorRegisteredScope(LambdaQueryWrapper<CaseInfo> wrapper, Long userId,
                                              List<String> roles) {
        if (userId == null || roles == null || !roles.contains(ROLE_ACCEPTOR) || canViewAllPending(roles)) {
            return;
        }
        applyAcceptorRegisteredCaseCondition(wrapper, userId);
    }

    /** 新上报公共池：尚未有受理员经手 */
    private void applyAcceptorUnclaimedPoolScope(LambdaQueryWrapper<CaseInfo> wrapper, Long userId,
                                                 List<String> roles) {
        if (userId == null || roles == null || !roles.contains(ROLE_ACCEPTOR) || canViewAllPending(roles)) {
            return;
        }
        wrapper.isNull(CaseInfo::getRegisterOperatorId);
    }

    /** 本人经手案件（待核查等） */
    private void applyAcceptorOwnedScope(LambdaQueryWrapper<CaseInfo> wrapper, Long userId, List<String> roles) {
        if (userId == null || roles == null || !roles.contains(ROLE_ACCEPTOR) || canViewAllPending(roles)) {
            return;
        }
        wrapper.eq(CaseInfo::getRegisterOperatorId, userId);
    }

    /**
     * 待核查案件：核查支线（立案前下发核查 / 处置后复核）。
     * 人工登记后未立案时 register_operator 可能为空，需按核查任务下发人（assigner_id）匹配。
     */
    private void applyAcceptorCollectCheckScope(LambdaQueryWrapper<CaseInfo> wrapper, Long userId,
                                                List<String> roles) {
        wrapper.and(statusCond -> statusCond
                .apply("id IN (SELECT case_id FROM check_task WHERE is_deleted = 0 AND finish_time IS NULL)")
                .or().eq(CaseInfo::getCaseStatus, CaseStatusConstant.CHECKING)
                .or().eq(CaseInfo::getCaseStatus, CaseStatusConstant.HANDLE_FINISH));
        if (userId == null || roles == null || !roles.contains(ROLE_ACCEPTOR) || canViewAllPending(roles)) {
            return;
        }
        wrapper.and(scope -> scope.eq(CaseInfo::getRegisterOperatorId, userId)
                .or()
                .apply("id IN (SELECT case_id FROM check_task WHERE is_deleted = 0 "
                        + "AND finish_time IS NULL AND assigner_id = {0})", userId)
                .or()
                .apply("id IN (SELECT DISTINCT case_id FROM case_flow_record WHERE operator_id = {0} "
                        + "AND node_name = '下发核查')", userId));
    }

    /**
     * 受理员「案件列表」：未认领新上报公共池 + 本人立案案件（全程可查，含已批转他人收尾）
     */
    private void applyAcceptorCaseScope(LambdaQueryWrapper<CaseInfo> wrapper, Long userId) {
        if (userId == null) {
            wrapper.eq(CaseInfo::getId, -1L);
            return;
        }
        wrapper.and(w -> w
                .nested(pool -> pool.isNull(CaseInfo::getRegisterOperatorId)
                        .in(CaseInfo::getCaseStatus,
                                CaseStatusConstant.REPORTED,
                                CaseStatusConstant.PENDING_REGISTER,
                                CaseStatusConstant.PENDING_VERIFY,
                                CaseStatusConstant.CHECKING)
                        .apply("id NOT IN (SELECT DISTINCT case_id FROM case_flow_record "
                                + "WHERE node_name = '立案并批转')"))
                .or(owned -> applyAcceptorRegisteredCaseCondition(owned, userId))
                .or(chk -> chk.eq(CaseInfo::getCaseStatus, CaseStatusConstant.CHECKING)
                        .apply("id IN (SELECT case_id FROM check_task WHERE is_deleted = 0 "
                                + "AND task_status = 'pending' AND assigner_id = {0})", userId))
                .or(chkFlow -> chkFlow.eq(CaseInfo::getCaseStatus, CaseStatusConstant.CHECKING)
                        .apply("id IN (SELECT DISTINCT case_id FROM case_flow_record WHERE operator_id = {0})",
                                userId)));
    }

    private void applyAcceptorRegisteredCaseCondition(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CaseInfo> wrapper, Long userId) {
        wrapper.notIn(CaseInfo::getCaseStatus, ACCEPTOR_REGISTERED_EXCLUDED_STATUSES)
                .and(w -> w.eq(CaseInfo::getRegisterOperatorId, userId)
                        .or()
                        .apply("id IN (SELECT DISTINCT case_id FROM case_flow_record WHERE operator_id = {0} "
                                + "AND node_name = '立案并批转')", userId));
    }

    private boolean canAcceptorViewCase(CaseInfo caseInfo, Long userId) {
        if (caseInfo == null || userId == null) {
            return false;
        }
        String st = caseInfo.getCaseStatus();
        if (CaseStatusConstant.CHECKING.equals(st)) {
            if (caseInfo.getRegisterOperatorId() == null) {
                return true;
            }
            if (isAcceptorRegisterOperator(caseInfo, userId)) {
                return true;
            }
            if (isCheckTaskAssigner(caseInfo.getId(), userId)) {
                return true;
            }
            return isCaseFlowOperator(caseInfo.getId(), userId);
        }
        if (caseInfo.getRegisterOperatorId() == null && isUnclaimedAcceptorStatus(st)
                && !isAcceptorRegisterOperatorByFlow(caseInfo.getId(), userId)) {
            return true;
        }
        if (isAcceptorRegisterOperator(caseInfo, userId)) {
            return true;
        }
        if (CaseStatusConstant.PENDING_CHECK.equals(st) && userId.equals(caseInfo.getCurrentHandlerId())) {
            return true;
        }
        return CaseStatusConstant.PENDING_CHECK.equals(st)
                && (isVerifyTaskCreator(caseInfo.getId(), userId) || isCaseFlowOperator(caseInfo.getId(), userId));
    }

    private boolean isCaseFlowOperator(Long caseId, Long userId) {
        if (caseId == null || userId == null) {
            return false;
        }
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM case_flow_record WHERE case_id = ? AND operator_id = ?",
                Long.class, caseId, userId);
        return count != null && count > 0;
    }

    private boolean isCheckTaskAssigner(Long caseId, Long userId) {
        if (caseId == null || userId == null) {
            return false;
        }
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM check_task WHERE case_id = ? AND assigner_id = ? "
                        + "AND is_deleted = 0 AND task_status = 'pending'",
                Long.class, caseId, userId);
        return count != null && count > 0;
    }

    private boolean isVerifyTaskCreator(Long caseId, Long userId) {
        if (caseId == null || userId == null) {
            return false;
        }
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM verify_task WHERE case_id = ? AND creator_id = ? "
                        + "AND is_deleted = 0 AND task_status = 'pending'",
                Long.class, caseId, userId);
        return count != null && count > 0;
    }

    private boolean isAcceptorRegisterOperator(CaseInfo caseInfo, Long userId) {
        if (caseInfo == null || userId == null) {
            return false;
        }
        if (userId.equals(caseInfo.getRegisterOperatorId())) {
            return true;
        }
        return isAcceptorRegisterOperatorByFlow(caseInfo.getId(), userId);
    }

    private boolean isAcceptorRegisterOperatorByFlow(Long caseId, Long userId) {
        if (caseId == null || userId == null) {
            return false;
        }
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM case_flow_record WHERE case_id = ? AND operator_id = ? "
                        + "AND node_name = '立案并批转'",
                Long.class,
                caseId,
                userId);
        return count != null && count > 0;
    }

    private static boolean isUnclaimedAcceptorStatus(String status) {
        return CaseStatusConstant.REPORTED.equals(status)
                || CaseStatusConstant.PENDING_REGISTER.equals(status)
                || CaseStatusConstant.CHECKING.equals(status)
                || CaseStatusConstant.PENDING_VERIFY.equals(status);
    }

    /** 立案/作废前：仅公共池或本人已认领 */
    private void assertAcceptorCanOperateCase(CaseInfo caseInfo, Long operatorId) {
        if (caseInfo == null || operatorId == null) {
            throw new BusinessException("未登录");
        }
        Long owner = caseInfo.getRegisterOperatorId();
        if (owner == null) {
            return;
        }
        if (!owner.equals(operatorId)) {
            throw new BusinessException("该案件已由其他受理员接手，您无权操作");
        }
    }

    /**
     * 处置人员已提交、待处置部门确认：handle_finish，或旧数据误落在 pending_check 且当前处理人仍为 HANDLER
     */
    private boolean isAwaitingDeptConfirm(CaseInfo caseInfo) {
        if (caseInfo == null) {
            return false;
        }
        if (CaseStatusConstant.HANDLE_FINISH.equals(caseInfo.getCaseStatus())) {
            return true;
        }
        if (CaseStatusConstant.PENDING_CHECK.equals(caseInfo.getCaseStatus())
                && caseInfo.getHandleFinishTime() != null
                && caseInfo.getCurrentHandlerId() != null) {
            List<String> handlerRoles = sysUserMapper.selectRoleCodesByUserId(caseInfo.getCurrentHandlerId());
            return handlerRoles != null && handlerRoles.contains(ROLE_HANDLER);
        }
        return false;
    }

    /** 部门已批转派遣员，待派遣员把关后批转受理员 */
    private boolean isAwaitingDispatcherForward(CaseInfo caseInfo) {
        if (caseInfo == null || !CaseStatusConstant.PENDING_CHECK.equals(caseInfo.getCaseStatus())) {
            return false;
        }
        if (caseInfo.getCurrentHandlerId() == null) {
            return false;
        }
        List<String> roles = sysUserMapper.selectRoleCodesByUserId(caseInfo.getCurrentHandlerId());
        return roles != null && roles.contains(ROLE_DISPATCHER);
    }

    private void applyAwaitingDeptConfirmScope(LambdaQueryWrapper<CaseInfo> wrapper, Long userId, List<String> roles) {
        wrapper.and(w -> w.eq(CaseInfo::getCaseStatus, CaseStatusConstant.HANDLE_FINISH)
                .or(w2 -> w2.eq(CaseInfo::getCaseStatus, CaseStatusConstant.PENDING_CHECK)
                        .isNotNull(CaseInfo::getHandleFinishTime)
                        .apply("current_handler_id IN (SELECT ru.user_id FROM sys_role_user ru "
                                + "INNER JOIN sys_role r ON ru.role_id = r.id "
                                + "WHERE r.role_code = {0} AND r.deleted = 0)", ROLE_HANDLER)));
        if (canViewAllPending(roles)) {
            return;
        }
        if (userId != null && roles != null && roles.contains(ROLE_DEPT)) {
            Long deptId = resolveUserDepartmentId(userId);
            wrapper.eq(CaseInfo::getHandleDeptId, deptId);
        } else {
            wrapper.eq(CaseInfo::getId, -1L);
        }
    }

    private SysUser resolveDispatcherUser(Long dispatcherUserId) {
        if (dispatcherUserId == null) {
            throw new BusinessException("请选择派遣员");
        }
        SysUser user = sysUserMapper.selectById(dispatcherUserId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("派遣员账号不存在或已停用");
        }
        List<String> roleCodes = sysUserMapper.selectRoleCodesByUserId(dispatcherUserId);
        if (roleCodes == null || !roleCodes.contains(ROLE_DISPATCHER)) {
            throw new BusinessException("所选用户不是派遣员，请重新选择");
        }
        return user;
    }

    /** 从流程记录解析立案后批转的派遣员（派遣至处置部门操作人） */
    private Long resolveCaseDispatchOperatorId(Long caseId) {
        List<CaseFlowRecord> flows = flowRecordMapper.selectByCaseId(caseId);
        if (flows == null || flows.isEmpty()) {
            return null;
        }
        for (int i = flows.size() - 1; i >= 0; i--) {
            CaseFlowRecord flow = flows.get(i);
            if ("派遣至处置部门".equals(flow.getNodeName()) && flow.getOperatorId() != null) {
                return flow.getOperatorId();
            }
        }
        return null;
    }

    private SysUser resolveAcceptorUser(Long acceptorUserId) {
        if (acceptorUserId == null) {
            throw new BusinessException("请选择受理员");
        }
        SysUser user = sysUserMapper.selectById(acceptorUserId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("受理员账号不存在或已停用");
        }
        List<String> roleCodes = sysUserMapper.selectRoleCodesByUserId(acceptorUserId);
        if (roleCodes == null || !roleCodes.contains(ROLE_ACCEPTOR)) {
            throw new BusinessException("所选用户不是受理员，请重新选择");
        }
        return user;
    }

    private static String displayUserName(SysUser user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        return user.getUsername();
    }

    /** 详情展示：按用户 ID 刷新立案/派遣/当前处理人姓名（修正历史写入的账号名） */
    private void enrichCaseOperatorDisplay(CaseInfo caseInfo) {
        if (caseInfo.getRegisterOperatorId() != null) {
            caseInfo.setRegisterOperatorName(resolveOperatorName(caseInfo.getRegisterOperatorId()));
        }
        if (caseInfo.getDispatchOperatorId() != null) {
            caseInfo.setDispatchOperatorName(resolveOperatorName(caseInfo.getDispatchOperatorId()));
        }
        if (caseInfo.getCurrentHandlerId() != null) {
            String handlerName = resolveOperatorName(caseInfo.getCurrentHandlerId());
            if (!"系统".equals(handlerName)) {
                caseInfo.setCurrentHandlerName(handlerName);
            }
        }
    }

    /** 流程记录展示：按 operator_id / receiver_id（仅对应真实用户）刷新姓名 */
    private void enrichFlowRecordDisplay(CaseFlowRecord flow) {
        if (flow.getOperatorId() != null) {
            String operatorName = resolveOperatorName(flow.getOperatorId());
            if (!"系统".equals(operatorName)) {
                flow.setOperatorName(operatorName);
            }
        }
        if (flow.getReceiverId() != null) {
            SysUser receiver = sysUserMapper.selectById(flow.getReceiverId());
            if (receiver != null) {
                flow.setReceiverName(displayUserName(receiver));
            }
        }
    }

    private void assertDispatcherAssignee(CaseInfo caseInfo, Long operatorId, List<String> operatorRoles) {
        if (caseInfo.getCurrentHandlerId() == null || operatorId == null) {
            return;
        }
        if (canViewAllPending(operatorRoles)) {
            return;
        }
        if (!caseInfo.getCurrentHandlerId().equals(operatorId)) {
            throw new BusinessException("该案件已批转给其他派遣员，您无权操作");
        }
    }

    private Long resolveUserDepartmentId(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户信息无效");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getDepartmentId() == null) {
            throw new BusinessException("当前用户未绑定部门，无法查看部门待办");
        }
        return user.getDepartmentId();
    }

    private void assertHandlerDeptOperator(CaseInfo caseInfo, Long operatorId, List<String> operatorRoles) {
        if (canViewAllPending(operatorRoles)) {
            return;
        }
        if (operatorRoles == null || !operatorRoles.contains(ROLE_DEPT)) {
            throw new BusinessException("仅部门登录账号可指派处置人员");
        }
        SysUser op = sysUserMapper.selectById(operatorId);
        if (op == null || op.getDepartmentId() == null
                || !op.getDepartmentId().equals(caseInfo.getHandleDeptId())) {
            throw new BusinessException("您无权操作其他处置部门的案件");
        }
    }

    private SysUser resolveHandlerUser(Long handlerUserId, Long handleDeptId) {
        if (handlerUserId == null) {
            throw new BusinessException("请选择处置人员");
        }
        SysUser user = sysUserMapper.selectById(handlerUserId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("处置人员账号不存在或已停用");
        }
        if (user.getDepartmentId() == null || !user.getDepartmentId().equals(handleDeptId)) {
            throw new BusinessException("所选人员不属于该处置部门");
        }
        List<String> roles = sysUserMapper.selectRoleCodesByUserId(handlerUserId);
        if (roles == null || !roles.contains(ROLE_HANDLER)) {
            throw new BusinessException("所选用户不是处置人员角色");
        }
        return user;
    }

    private void assertAssignedHandler(CaseInfo caseInfo, Long operatorId, List<String> operatorRoles) {
        if (canViewAllPending(operatorRoles)) {
            return;
        }
        if (caseInfo.getCurrentHandlerId() == null) {
            throw new BusinessException("案件尚未指派处置人员");
        }
        if (operatorId == null || !caseInfo.getCurrentHandlerId().equals(operatorId)) {
            throw new BusinessException("仅被指派的处置人员可提交处置结果");
        }
    }

    private String resolveOperatorName(Long operatorId) {
        if (operatorId == null) {
            return "系统";
        }
        SysUser user = sysUserMapper.selectById(operatorId);
        return user != null ? displayUserName(user) : "系统";
    }

    private List<CollectorCandidateDto> buildCollectorCandidates(CaseInfo caseInfo) {
        List<Long> collectorIds = sysUserMapper.selectUserIdsByRoleCode("COLLECTOR");
        if (collectorIds == null || collectorIds.isEmpty()) {
            return List.of();
        }
        Set<Long> onGridIds = new LinkedHashSet<>();
        if (caseInfo.getRespGridId() != null) {
            List<Long> gridCollectorIds = jdbcTemplate.queryForList(
                    "SELECT user_id FROM responsibility_grid_collector WHERE resp_grid_id = ?",
                    Long.class,
                    caseInfo.getRespGridId());
            onGridIds.addAll(gridCollectorIds);
        }
        Double caseLng = caseInfo.getLongitude();
        Double caseLat = caseInfo.getLatitude();
        List<CollectorCandidateDto> list = new ArrayList<>();
        for (Long uid : collectorIds) {
            SysUser u = sysUserMapper.selectById(uid);
            if (!isActiveCollector(u)) {
                continue;
            }
            CollectorCandidateDto dto = new CollectorCandidateDto();
            dto.setUserId(u.getId());
            dto.setUsername(u.getUsername());
            dto.setRealName(u.getRealName());
            dto.setPhone(u.getPhone());
            dto.setOnRespGrid(onGridIds.contains(u.getId()));
            double[] loc = loadCollectorLastLocation(u.getId());
            if (loc != null) {
                dto.setLongitude(loc[0]);
                dto.setLatitude(loc[1]);
                if (caseLng != null && caseLat != null) {
                    dto.setDistanceKm(roundKm(haversineKm(caseLng, caseLat, loc[0], loc[1])));
                }
                dto.setLocationHint("最近上报位置");
            } else if (Boolean.TRUE.equals(dto.getOnRespGrid())) {
                dto.setLocationHint("责任片区采集员（暂无最近坐标）");
            } else {
                dto.setLocationHint("暂无最近坐标");
            }
            list.add(dto);
        }
        list.sort((a, b) -> {
            int g1 = Boolean.TRUE.equals(b.getOnRespGrid()) ? 1 : 0;
            int g2 = Boolean.TRUE.equals(a.getOnRespGrid()) ? 1 : 0;
            if (g1 != g2) {
                return g1 - g2;
            }
            double d1 = a.getDistanceKm() != null ? a.getDistanceKm() : Double.MAX_VALUE;
            double d2 = b.getDistanceKm() != null ? b.getDistanceKm() : Double.MAX_VALUE;
            return Double.compare(d1, d2);
        });
        if (!list.isEmpty()) {
            CollectorCandidateDto first = list.get(0);
            first.setRecommended(true);
            for (int i = 1; i < list.size(); i++) {
                list.get(i).setRecommended(false);
            }
        }
        return list;
    }

    private SysUser resolveCollectorForCase(CaseInfo caseInfo, Long collectorUserId) {
        if (collectorUserId != null) {
            return resolveCollectorUser(collectorUserId);
        }
        List<CollectorCandidateDto> candidates = buildCollectorCandidates(caseInfo);
        if (candidates.isEmpty()) {
            throw new BusinessException("无在班采集员可指派，请先维护采集员账号");
        }
        Long pick = candidates.get(0).getUserId();
        return resolveCollectorUser(pick);
    }

    private SysUser resolveCollectorUser(Long collectorUserId) {
        SysUser user = sysUserMapper.selectById(collectorUserId);
        if (!isActiveCollector(user)) {
            throw new BusinessException("采集员不存在或已停用");
        }
        List<String> roles = sysUserMapper.selectRoleCodesByUserId(collectorUserId);
        if (roles == null || !roles.contains("COLLECTOR")) {
            throw new BusinessException("所选用户不是采集员");
        }
        return user;
    }

    /** 采集员最近一次上报案件的坐标，用作地图展示参考 */
    private double[] loadCollectorLastLocation(Long collectorUserId) {
        if (collectorUserId == null) {
            return null;
        }
        List<double[]> rows = jdbcTemplate.query(
                """
                SELECT longitude, latitude FROM case_info
                WHERE reporter_id = ? AND is_deleted = 0
                  AND longitude IS NOT NULL AND latitude IS NOT NULL
                ORDER BY report_time DESC LIMIT 1
                """,
                (rs, rowNum) -> new double[] { rs.getDouble("longitude"), rs.getDouble("latitude") },
                collectorUserId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static double haversineKm(double lng1, double lat1, double lng2, double lat2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double roundKm(double km) {
        return Math.round(km * 100.0) / 100.0;
    }

    private static boolean isActiveCollector(SysUser user) {
        return user != null && user.getStatus() != null && user.getStatus() == 1;
    }

    private String generateTaskCode(String prefix) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String head = prefix + dateStr;
        String sql = prefix.startsWith("HC")
                ? "SELECT task_code FROM check_task WHERE task_code LIKE ? ORDER BY task_code DESC LIMIT 1"
                : "SELECT task_code FROM verify_task WHERE task_code LIKE ? ORDER BY task_code DESC LIMIT 1";
        List<String> codes = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1), head + "%");
        int seq = 1;
        if (!codes.isEmpty() && codes.get(0) != null && codes.get(0).length() >= head.length() + 4) {
            try {
                seq = Integer.parseInt(codes.get(0).substring(codes.get(0).length() - 4)) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return head + String.format("%04d", seq);
    }

    private String generateCaseCode() {
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "YC" + dateStr;
        LambdaQueryWrapper<CaseInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(CaseInfo::getCaseCode, prefix)
               .orderByDesc(CaseInfo::getCaseCode)
               .last("LIMIT 1");
        CaseInfo lastCase = caseInfoMapper.selectOne(wrapper);

        int seq = 1;
        if (lastCase != null && lastCase.getCaseCode() != null) {
            String lastCode = lastCase.getCaseCode();
            seq = Integer.parseInt(lastCode.substring(lastCode.length() - 4)) + 1;
        }
        return prefix + String.format("%04d", seq);
    }

    private void saveFlowRecord(Long caseId, String caseCode, String nodeCode, String nodeName, String operateOpinion) {
        saveFlowRecord(caseId, caseCode, nodeCode, nodeName, CaseFlowOperateType.FORWARD, operateOpinion, 1L, "系统",
                null, null);
    }

    private void saveFlowRecord(Long caseId, String caseCode, String nodeCode, String nodeName, String operateOpinion,
                                Long operatorId, String operatorName) {
        saveFlowRecord(caseId, caseCode, nodeCode, nodeName, CaseFlowOperateType.FORWARD, operateOpinion,
                operatorId, operatorName, null, null);
    }

    private void saveFlowRecord(Long caseId, String caseCode, String nodeCode, String nodeName, String operateOpinion,
                                Long operatorId, String operatorName,
                                Long receiverId, String receiverName) {
        saveFlowRecord(caseId, caseCode, nodeCode, nodeName, CaseFlowOperateType.FORWARD, operateOpinion,
                operatorId, operatorName, receiverId, receiverName);
    }

    private void saveFlowRecord(Long caseId, String caseCode, String nodeCode, String nodeName,
                                String operateType, String operateOpinion,
                                Long operatorId, String operatorName,
                                Long receiverId, String receiverName) {
        if (caseCode == null || caseCode.isBlank()) {
            CaseInfo loaded = caseInfoMapper.selectById(caseId);
            caseCode = loaded != null ? loaded.getCaseCode() : null;
        }
        if (caseCode == null || caseCode.isBlank()) {
            throw new BusinessException("案件编码为空，无法写入流程记录，请检查 case_info.case_code");
        }
        long opId = operatorId != null ? operatorId : 1L;
        String opName = resolveOperatorName(opId);
        if ("系统".equals(opName) && operatorName != null && !operatorName.isBlank()) {
            opName = operatorName;
        }
        String opType = operateType != null && !operateType.isBlank() ? operateType : CaseFlowOperateType.FORWARD;
        LocalDateTime now = LocalDateTime.now();
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
                caseId,
                caseCode,
                nodeCode,
                nodeName,
                opType,
                opType,
                operateOpinion != null ? operateOpinion : "",
                opId,
                opName,
                receiverId,
                receiverName,
                now,
                now
        );
    }

    private void notifyAllAcceptorsNewCase(CaseInfo caseInfo) {
        notifyAllAcceptorsNewCase(caseInfo, null);
    }

    /**
     * 新案进入待立案时通知受理员。采集员上报通知全员；受理员人工登记时排除登记人本人，且文案区分渠道。
     */
    private void notifyAllAcceptorsNewCase(CaseInfo caseInfo, Long excludeUserId) {
        if (userNotificationSender == null || caseInfo == null) {
            return;
        }
        try {
            List<Long> acceptorIds = sysUserMapper.selectUserIdsByRoleCode(ROLE_ACCEPTOR);
            if (excludeUserId != null) {
                acceptorIds = acceptorIds.stream()
                        .filter(id -> !excludeUserId.equals(id))
                        .toList();
            }
            if (acceptorIds.isEmpty()) {
                return;
            }
            String addr = caseInfo.getAddress() != null ? caseInfo.getAddress() : "";
            String content;
            if (CaseSourceConstant.REGISTER.equals(caseInfo.getSourceType())) {
                String channel = caseInfo.getSourceDesc() != null && !caseInfo.getSourceDesc().isBlank()
                        ? caseInfo.getSourceDesc().trim() : "人工登记";
                content = "受理员登记新案件（" + channel + "），请协同处理。地址：" + addr;
            } else {
                content = "采集员上报新案件，请及时立案。地址：" + addr;
            }
            userNotificationSender.notifyUsers(acceptorIds, "新案件待立案", content,
                    BIZ_CASE, caseInfo.getId(), caseInfo.getCaseCode());
        } catch (Exception e) {
            log.warn("发送受理员新案提醒失败: {}", e.getMessage());
        }
    }

    private void notifyUserTask(Long userId, String title, String content, String bizType, Long bizId, String bizCode) {
        if (userNotificationSender == null || userId == null) {
            return;
        }
        try {
            userNotificationSender.notifyUser(userId, title, content, bizType, bizId, bizCode);
        } catch (Exception e) {
            log.warn("发送用户提醒失败 userId={}: {}", userId, e.getMessage());
        }
    }

    private void notifyDeptUsersNewCase(CaseInfo caseInfo, String deptName) {
        if (userNotificationSender == null || caseInfo == null || caseInfo.getHandleDeptId() == null) {
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
                    Long.class, ROLE_DEPT, caseInfo.getHandleDeptId());
            String content = "案件已派遣至 " + (deptName != null ? deptName : "本部门") + "，请指派处置人员";
            userNotificationSender.notifyUsers(deptUserIds, "新案件待指派",
                    content, BIZ_CASE, caseInfo.getId(), caseInfo.getCaseCode());
        } catch (Exception e) {
            log.warn("发送部门提醒失败: {}", e.getMessage());
        }
    }

    private static String requireRemark(String remark) {
        if (remark == null || remark.isBlank()) {
            throw new BusinessException("请填写操作原因");
        }
        return remark.trim();
    }

    /** 派遣员回退受理员后，受理员可再次立案批转（不清空已填信息） */
    private boolean isAcceptorRedispatchAfterReturn(CaseInfo caseInfo, Long operatorId) {
        if (caseInfo == null || operatorId == null) {
            return false;
        }
        if (!CaseStatusConstant.RETURNED.equals(caseInfo.getCaseStatus())) {
            return false;
        }
        if (operatorId.equals(caseInfo.getRegisterOperatorId())) {
            return true;
        }
        return operatorId.equals(caseInfo.getCurrentHandlerId())
                && userHasRole(operatorId, ROLE_ACCEPTOR);
    }

    private SysUser resolveReturnAcceptorUser(CaseInfo caseInfo, Long acceptorUserId) {
        if (acceptorUserId != null) {
            return resolveAcceptorUser(acceptorUserId);
        }
        Long regId = caseInfo.getRegisterOperatorId();
        if (regId != null) {
            SysUser reg = sysUserMapper.selectById(regId);
            if (reg != null && reg.getStatus() != null && reg.getStatus() == 1
                    && userHasRole(regId, ROLE_ACCEPTOR)) {
                return reg;
            }
        }
        throw new BusinessException("请选择受理员（立案受理员不在班时请指定其他受理员）");
    }

    /** 部门回退至派遣员后，须先再派部门，不可直接批转受理员 */
    private void assertDispatcherCanForwardAcceptor(CaseInfo caseInfo) {
        if (caseInfo == null || !CaseStatusConstant.PENDING_CHECK.equals(caseInfo.getCaseStatus())) {
            return;
        }
        List<CaseFlowRecord> flows = flowRecordMapper.selectByCaseId(caseInfo.getId());
        if (flows == null || flows.isEmpty()) {
            return;
        }
        int lastDeptReturnIdx = -1;
        int lastDispatchIdx = -1;
        for (int i = 0; i < flows.size(); i++) {
            String nodeName = flows.get(i).getNodeName();
            if ("部门回退".equals(nodeName)) {
                lastDeptReturnIdx = i;
            }
            if ("派遣至处置部门".equals(nodeName)) {
                lastDispatchIdx = i;
            }
        }
        if (lastDeptReturnIdx >= 0 && lastDispatchIdx < lastDeptReturnIdx) {
            throw new BusinessException("案件由处置部门回退，请先重新派遣至处置部门后再批转受理员");
        }
    }

    private boolean userHasRole(Long userId, String roleCode) {
        if (userId == null || roleCode == null) {
            return false;
        }
        List<String> roles = sysUserMapper.selectRoleCodesByUserId(userId);
        return roles != null && roles.contains(roleCode);
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(v.toString());
    }

    private static Double toDouble(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return Double.valueOf(v.toString());
    }

    /**
     * 解析前端传入的日期（支持 yyyy-MM-dd、ISO-8601 等）；endOfDay 为 true 时取当天最后一刻。
     */
    private static LocalDateTime parseDateTimeParam(Object raw, boolean endOfDay) {
        if (raw == null || raw.toString().isBlank()) {
            return null;
        }
        String s = raw.toString().trim();
        try {
            if (s.length() == 10 && s.charAt(4) == '-') {
                LocalDate d = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
                return endOfDay ? LocalDateTime.of(d, LocalTime.of(23, 59, 59)) : LocalDateTime.of(d, LocalTime.MIN);
            }
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
}
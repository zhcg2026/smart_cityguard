package com.cityguard.caseinfo.service.impl;

import com.cityguard.caseinfo.entity.CaseInfo;
import com.cityguard.caseinfo.mapper.CaseInfoMapper;
import com.cityguard.common.constant.CaseFlowOperateType;
import com.cityguard.common.constant.CaseStatusConstant;
import com.cityguard.common.spi.CaseTaskCompletionHandler;
import com.cityguard.common.spi.UserNotificationSender;
import com.cityguard.task.entity.CheckTask;
import com.cityguard.task.entity.VerifyTask;
import com.cityguard.task.mapper.CheckTaskMapper;
import com.cityguard.task.mapper.VerifyTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 采集员完成可选分支任务后回写案件状态（核查/核实非必经步骤）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseTaskCompletionHandlerImpl implements CaseTaskCompletionHandler {

    private static final String BIZ_CASE = "case";
    private static final String BIZ_CHECK_TASK = "check_task";
    private static final String BIZ_VERIFY_TASK = "verify_task";

    private final CaseInfoMapper caseInfoMapper;
    private final CheckTaskMapper checkTaskMapper;
    private final VerifyTaskMapper verifyTaskMapper;
    private final JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private UserNotificationSender userNotificationSender;

    @Override
    public void afterCheckTaskCompleted(Long taskId, String checkResult, String checkOpinion, Long collectorId) {
        CheckTask task = checkTaskMapper.selectById(taskId);
        if (task == null || task.getCaseId() == null) {
            return;
        }
        CaseInfo caseInfo = caseInfoMapper.selectById(task.getCaseId());
        if (caseInfo == null) {
            return;
        }
        caseInfo.setCheckTaskId(taskId);
        caseInfo.setCaseStatus(CaseStatusConstant.PENDING_REGISTER);
        caseInfoMapper.updateById(caseInfo);

        String opinion = (checkOpinion != null && !checkOpinion.isBlank())
                ? checkOpinion
                : labelCheckResult(checkResult);
        insertFlow(caseInfo, CaseStatusConstant.PENDING_REGISTER, "核查完成",
                collectorId, task.getCollectorName(), opinion,
                task.getAssignerId(), task.getAssignerName());
        if (task.getAssignerId() != null) {
            notifyUser(task.getAssignerId(), "核查任务已完成",
                    "案件 " + caseInfo.getCaseCode() + " 核查已提交，可继续立案",
                    BIZ_CASE, caseInfo.getId(), caseInfo.getCaseCode());
        }
    }

    @Override
    public void afterVerifyTaskCompleted(Long taskId, String verifyResult, String verifyOpinion, Long collectorId) {
        VerifyTask task = verifyTaskMapper.selectById(taskId);
        if (task == null || task.getCaseId() == null) {
            return;
        }
        CaseInfo caseInfo = caseInfoMapper.selectById(task.getCaseId());
        if (caseInfo == null) {
            return;
        }
        caseInfo.setVerifyTaskId(taskId);
        caseInfoMapper.updateById(caseInfo);

        String opinion = (verifyOpinion != null && !verifyOpinion.isBlank())
                ? verifyOpinion
                : labelVerifyResult(verifyResult);
        insertFlow(caseInfo, caseInfo.getCaseStatus(), "核实完成",
                collectorId, task.getCollectorName(), opinion,
                task.getCreatorId(), task.getCreatorName());
        if (task.getCreatorId() != null) {
            notifyUser(task.getCreatorId(), "核实任务已完成",
                    "案件 " + caseInfo.getCaseCode() + " 核实已提交，可继续结案",
                    BIZ_CASE, caseInfo.getId(), caseInfo.getCaseCode());
        }
    }

    private void notifyUser(Long userId, String title, String content, String bizType, Long bizId, String bizCode) {
        if (userNotificationSender == null || userId == null) {
            return;
        }
        try {
            userNotificationSender.notifyUser(userId, title, content, bizType, bizId, bizCode);
        } catch (Exception e) {
            log.warn("任务完成提醒失败: {}", e.getMessage());
        }
    }

    private void insertFlow(CaseInfo caseInfo, String nodeCode, String nodeName,
                            Long operatorId, String operatorName, String opinion,
                            Long receiverId, String receiverName) {
        long opId = operatorId != null ? operatorId : 1L;
        String opName = operatorName != null && !operatorName.isBlank() ? operatorName : "系统";
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
                caseInfo.getId(),
                caseInfo.getCaseCode(),
                nodeCode,
                nodeName,
                CaseFlowOperateType.FORWARD,
                CaseFlowOperateType.FORWARD,
                opinion != null ? opinion : "",
                opId,
                opName,
                receiverId,
                receiverName,
                now,
                now);
    }

    private static String labelCheckResult(String r) {
        if (r == null) {
            return "核查完成";
        }
        return switch (r) {
            case "pass" -> "核查：问题确认存在";
            case "not_pass" -> "核查：未发现问题";
            case "unable" -> "核查：无法核查";
            default -> "核查完成";
        };
    }

    private static String labelVerifyResult(String r) {
        if (r == null) {
            return "核实完成";
        }
        return switch (r) {
            case "exist" -> "核实：处置到位";
            case "not_exist" -> "核实：处置不到位";
            case "unable" -> "核实：无法核实";
            default -> "核实完成";
        };
    }
}

package com.cityguard.caseinfo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.caseinfo.dto.CaseAcceptorReturnDispatcherRequest;
import com.cityguard.caseinfo.dto.CaseDashboardStatsDto;
import com.cityguard.caseinfo.dto.CaseDashboardTodosDto;
import com.cityguard.caseinfo.dto.CaseAcceptorRegisterRequest;
import com.cityguard.caseinfo.dto.CaseAssignHandlerRequest;
import com.cityguard.caseinfo.dto.CaseDeptConfirmRequest;
import com.cityguard.caseinfo.dto.CaseDeptReturnRequest;
import com.cityguard.caseinfo.dto.CaseDispatcherForwardRequest;
import com.cityguard.caseinfo.dto.CaseDispatcherReturnAcceptorRequest;
import com.cityguard.caseinfo.dto.CaseRegisterRequest;
import com.cityguard.caseinfo.dto.CaseReturnRequest;
import com.cityguard.caseinfo.dto.CaseRevokeAssignRequest;
import com.cityguard.caseinfo.dto.CaseSendTaskRequest;
import com.cityguard.caseinfo.entity.CaseInfo;
import com.cityguard.caseinfo.entity.CaseFlowRecord;
import com.cityguard.caseinfo.entity.CaseAttachment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CaseService {

    CaseInfo reportCase(Map<String, Object> reportData);

    CaseInfo acceptorRegisterCase(CaseAcceptorRegisterRequest request, Long operatorId, String operatorName,
                                  List<String> roles);

    CaseInfo getCaseDetail(Long id);

    CaseInfo getCaseDetail(Long id, Long userId, java.util.List<String> roles);

    Page<CaseInfo> getCaseList(Integer pageNum, Integer pageSize, Map<String, Object> params,
                               Long userId, java.util.List<String> roles);

    Page<CaseInfo> getPendingCases(String status, Integer pageNum, Integer pageSize, Long userId, java.util.List<String> roles);

    Page<CaseInfo> getMyCaseList(Long reporterId, Integer pageNum, Integer pageSize);

    CaseDashboardStatsDto getDashboardStats(Long userId, List<String> roles);

    CaseDashboardTodosDto getDashboardTodos(Long userId, List<String> roles, int limit);

    CaseInfo registerCase(CaseRegisterRequest request, Long operatorId, String operatorName);

    CaseInfo dispatchCase(Long caseId, Long departmentId, String remark, LocalDateTime clientUpdateTime,
                          Long operatorId, java.util.List<String> operatorRoles);

    CaseInfo assignHandler(CaseAssignHandlerRequest request, Long operatorId, String operatorName,
                           java.util.List<String> operatorRoles);

    CaseInfo handleCase(Long caseId, String remark, List<String> attachments, Long operatorId,
                        java.util.List<String> operatorRoles);

    CaseInfo deptConfirmCase(CaseDeptConfirmRequest request, Long operatorId, String operatorName,
                             java.util.List<String> operatorRoles);

    CaseInfo dispatcherForwardToAcceptor(CaseDispatcherForwardRequest request, Long operatorId,
                                           String operatorName, java.util.List<String> operatorRoles);

    CaseInfo deptReturnCase(CaseDeptReturnRequest request, Long operatorId, String operatorName,
                            java.util.List<String> operatorRoles);

    CaseInfo deptRevokeAssign(CaseRevokeAssignRequest request, Long operatorId, String operatorName,
                              java.util.List<String> operatorRoles);

    CaseInfo deptReturnHandler(CaseReturnRequest request, Long operatorId, String operatorName,
                               java.util.List<String> operatorRoles);

    CaseInfo handlerReturnDept(CaseReturnRequest request, Long operatorId,
                               java.util.List<String> operatorRoles);

    CaseInfo dispatcherReturnAcceptor(CaseDispatcherReturnAcceptorRequest request, Long operatorId,
                                      String operatorName, java.util.List<String> operatorRoles);

    CaseInfo dispatcherReturnDept(CaseReturnRequest request, Long operatorId, String operatorName,
                                  java.util.List<String> operatorRoles);

    CaseInfo acceptorReturnDispatcher(CaseAcceptorReturnDispatcherRequest request, Long operatorId,
                                      String operatorName, java.util.List<String> operatorRoles);

    java.util.List<com.cityguard.caseinfo.dto.CollectorCandidateDto> listCollectorCandidates(
            Long caseId, Long userId, java.util.List<String> roles);

    /** 可选：立案前下发核查任务（非必经） */
    CaseInfo sendCheckTask(CaseSendTaskRequest request, Long operatorId, String operatorName,
                           java.util.List<String> operatorRoles);

    /** 可选：结案前下发核实任务（非必经） */
    CaseInfo sendVerifyTask(CaseSendTaskRequest request, Long operatorId, String operatorName,
                            java.util.List<String> operatorRoles);

    CaseInfo verifyCase(Long caseId, Integer result, String remark, List<String> attachments);

    CaseInfo checkCase(Long caseId, Integer result, String remark, List<String> attachments,
                      Long operatorId, java.util.List<String> operatorRoles);

    CaseInfo closeCase(Long caseId, String remark, Long operatorId, java.util.List<String> operatorRoles);

    CaseInfo rejectCase(Long caseId, String reason, Long operatorId, String operatorName);

    List<CaseFlowRecord> getFlowRecords(Long caseId);

    List<CaseAttachment> getAttachments(Long caseId);

    java.util.List<com.cityguard.task.dto.CheckTaskRecordView> listCheckTaskRecords(
            Long caseId, Long userId, java.util.List<String> roles);

    java.util.List<com.cityguard.task.dto.VerifyTaskRecordView> listVerifyTaskRecords(
            Long caseId, Long userId, java.util.List<String> roles);

    /** 管理员逻辑删除案件（支持批量） */
    int deleteCases(java.util.List<Long> caseIds, java.util.List<String> operatorRoles);
}
package com.cityguard.caseinfo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.caseinfo.dto.CaseAdjustmentApplyRequest;
import com.cityguard.caseinfo.dto.CaseAdjustmentReviewRequest;
import com.cityguard.caseinfo.entity.CaseAdjustmentApply;
import com.cityguard.caseinfo.entity.CaseInfo;

import java.util.List;

public interface CaseAdjustmentService {

    CaseAdjustmentApply apply(CaseAdjustmentApplyRequest request, Long operatorId, String operatorName,
                              List<String> operatorRoles);

    Page<CaseAdjustmentApply> listPending(Integer pageNum, Integer pageSize, Long operatorId, List<String> roles);

    Page<CaseAdjustmentApply> listPendingDept(Integer pageNum, Integer pageSize, Long operatorId, List<String> roles);

    CaseAdjustmentApply deptReview(CaseAdjustmentReviewRequest request, Long operatorId, String operatorName,
                                   List<String> roles);

    CaseAdjustmentApply review(CaseAdjustmentReviewRequest request, Long operatorId, String operatorName,
                               List<String> roles);

    List<CaseAdjustmentApply> listByCaseId(Long caseId);

    void enrichCaseDetail(CaseInfo caseInfo);

    /**
     * 处置人员解除指派时，作废其发起且仍在审批中的延期/挂账申请。
     */
    void voidPendingAdjustmentsOnHandlerUnassign(Long caseId, Long unassignedHandlerId,
                                                 String voidReason, Long operatorId, String operatorName);

    void assertCaseOperable(CaseInfo caseInfo);

    /** 挂账到期自动恢复（定时任务） */
    void resumeExpiredSuspensions();
}

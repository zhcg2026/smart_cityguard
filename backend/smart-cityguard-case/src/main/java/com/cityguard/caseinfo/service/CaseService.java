package com.cityguard.caseinfo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.caseinfo.entity.CaseInfo;
import com.cityguard.caseinfo.entity.CaseFlowRecord;
import com.cityguard.caseinfo.entity.CaseAttachment;

import java.util.List;
import java.util.Map;

public interface CaseService {

    CaseInfo reportCase(Map<String, Object> reportData);

    CaseInfo getCaseDetail(Long id);

    Page<CaseInfo> getCaseList(Integer pageNum, Integer pageSize, Map<String, Object> params);

    List<CaseInfo> getPendingCases(Integer status);

    CaseInfo registerCase(Long caseId, String remark);

    CaseInfo dispatchCase(Long caseId, Long departmentId, String remark);

    CaseInfo handleCase(Long caseId, String remark, List<String> attachments);

    CaseInfo verifyCase(Long caseId, Integer result, String remark, List<String> attachments);

    CaseInfo checkCase(Long caseId, Integer result, String remark, List<String> attachments);

    CaseInfo closeCase(Long caseId, String remark);

    CaseInfo rejectCase(Long caseId, String reason);

    List<CaseFlowRecord> getFlowRecords(Long caseId);

    List<CaseAttachment> getAttachments(Long caseId);
}
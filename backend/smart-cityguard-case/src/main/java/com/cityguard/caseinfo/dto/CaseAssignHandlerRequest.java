package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 处置部门将案件指派给本部门具体处置人员。
 */
@Data
public class CaseAssignHandlerRequest {

    private Long caseId;
    /** 处置人员用户 ID */
    private Long handlerUserId;
    private String remark;
    private LocalDateTime clientUpdateTime;
}

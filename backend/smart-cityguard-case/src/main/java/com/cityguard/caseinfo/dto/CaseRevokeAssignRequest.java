package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 处置部门撤销已指派的处置人员（回退派遣员前置）
 */
@Data
public class CaseRevokeAssignRequest {

    private Long caseId;

    private String remark;

    private LocalDateTime clientUpdateTime;
}

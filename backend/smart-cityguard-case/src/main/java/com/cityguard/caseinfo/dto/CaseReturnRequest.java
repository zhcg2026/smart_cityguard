package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 案件回退/批转类请求基类
 */
@Data
public class CaseReturnRequest {

    private Long caseId;

    /** 操作意见（回退必填） */
    private String remark;

    private LocalDateTime clientUpdateTime;
}

package com.cityguard.config.dto;

import lombok.Data;

@Data
public class CaseStandardSaveRequest {

    private Long id;

    private Long smallId;

    private String conditionDesc;

    private String closeCondition;

    private String handleTimeType;

    private Integer handleTimeValue;

    private Integer sortOrder;

    private Integer status;
}

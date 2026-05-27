package com.cityguard.config.dto;

import lombok.Data;

@Data
public class TimeLimitOverrideSaveRequest {

    private Long id;

    private Long smallId;

    private String timeLimitType;

    private Integer timeLimitValue;

    private String remark;
}

package com.cityguard.caseinfo.dto;

import lombok.Data;

@Data
public class CaseSendTaskRequest {

    private Long caseId;

    /** 指派采集员（可选；不传则自动推荐距离最近者） */
    private Long collectorUserId;

    /** 下发说明（可选） */
    private String remark;
}

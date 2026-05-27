package com.cityguard.geo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CollectorMapCasePointDto {
    private Long id;
    private String caseCode;
    private String caseStatus;
    private Double longitude;
    private Double latitude;
    private Long respGridId;
    private String respGridName;
    /** 上报人（采集员）用户 ID */
    private Long reporterId;
    private LocalDateTime reportTime;
}

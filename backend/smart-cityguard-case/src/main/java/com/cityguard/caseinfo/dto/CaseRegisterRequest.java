package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 立案请求：可顺带修正上报信息；clientUpdateTime 与详情中 updateTime 一致时才能更新，用于多受理员并发防串改。
 */
@Data
public class CaseRegisterRequest {

    private Long caseId;
    /** 立案时指定批转的派遣员用户 ID（必填） */
    private Long dispatcherUserId;
    private String remark;

    /** 打开详情时拿到的 updateTime，提交立案时原样回传 */
    private LocalDateTime clientUpdateTime;

    private String address;
    private String description;
    private String bigName;
    private String smallName;
    private Long smallId;
    private Long standardId;
    private String conditionDesc;
    private Double longitude;
    private Double latitude;
}

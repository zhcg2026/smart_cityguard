package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.util.List;

/**
 * 受理员电话投诉等人工登记（入库后进入待立案，与采集上报同一主干流程）
 */
@Data
public class CaseAcceptorRegisterRequest {

    private String categoryType;
    private String bigCode;
    private String bigName;
    private String smallCode;
    private String smallName;
    private Long smallId;
    private Long standardId;
    private String conditionDesc;

    private String address;
    private String description;
    private Double longitude;
    private Double latitude;

    /** 投诉人/举报人 */
    private String reporterName;
    private String reporterPhone;

    /** 来源描述，默认「电话投诉」 */
    private String sourceDesc;

    private List<String> attachments;

    /** 登记备注，写入流程意见 */
    private String remark;
}

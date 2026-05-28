package com.cityguard.caseinfo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 考核统计 / 反查条件（在综合查询字段基础上增加截止时间与问题描述）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CaseReportCriteria extends CaseQueryCriteria {

    /** 处置截止时间 */
    private CaseDateFilter deadlineTime;

    /** 问题描述 */
    private String description;

    /** 描述匹配：eq、contains */
    private String descriptionMatch;

    /** 反查指标键，见 {@link com.cityguard.caseinfo.constant.CaseReportMetric} */
    private String metricKey;

    /** 反查时指定统计行处置部门 */
    private Long drillHandleDeptId;
}

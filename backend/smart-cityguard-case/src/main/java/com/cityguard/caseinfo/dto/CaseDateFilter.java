package com.cityguard.caseinfo.dto;

import lombok.Data;

/**
 * 日期类查询条件：eq / gt / lt / between（日期字符串 yyyy-MM-dd）
 */
@Data
public class CaseDateFilter {

    /** 运算符：eq、gt、lt、between */
    private String op;

    /** 单日或区间起点 */
    private String start;

    /** 区间终点（between 时必填） */
    private String end;
}

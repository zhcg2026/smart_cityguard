package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.util.List;

@Data
public class CaseReportStatisticsResult {

    private List<CaseDeptStatisticsRow> rows;

    /** 合计行（处置部门名称为「合计」） */
    private CaseDeptStatisticsRow totalRow;
}

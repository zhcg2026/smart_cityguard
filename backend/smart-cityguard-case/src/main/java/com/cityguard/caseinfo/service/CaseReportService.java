package com.cityguard.caseinfo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.caseinfo.dto.CaseDeptStatisticsRow;
import com.cityguard.caseinfo.dto.CaseReportCriteria;
import com.cityguard.caseinfo.dto.CaseReportStatisticsResult;
import com.cityguard.caseinfo.entity.CaseInfo;

import java.util.List;

public interface CaseReportService {

    CaseReportStatisticsResult statistics(CaseReportCriteria criteria, Long userId, List<String> roles);

    Page<CaseInfo> drillDown(CaseReportCriteria criteria, Long userId, List<String> roles);
}

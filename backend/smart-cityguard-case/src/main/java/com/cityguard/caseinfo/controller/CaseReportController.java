package com.cityguard.caseinfo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.auth.entity.LoginUser;
import com.cityguard.caseinfo.dto.CaseReportCriteria;
import com.cityguard.caseinfo.dto.CaseReportStatisticsResult;
import com.cityguard.caseinfo.entity.CaseInfo;
import com.cityguard.caseinfo.service.CaseReportService;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "考核统计", description = "按处置部门汇总案件指标，支持反查")
@RestController
@RequestMapping("/case/report")
@RequiredArgsConstructor
public class CaseReportController {

    private final CaseReportService caseReportService;

    @Operation(summary = "考核统计表（按处置部门）")
    @PostMapping("/statistics")
    public Result<CaseReportStatisticsResult> statistics(@RequestBody CaseReportCriteria criteria) {
        LoginUser user = currentUser();
        return Result.success(caseReportService.statistics(criteria,
                user != null ? user.getId() : null,
                user != null ? user.getRoles() : null));
    }

    @Operation(summary = "考核统计数字反查案件列表")
    @PostMapping("/drill")
    public Result<Page<CaseInfo>> drill(@RequestBody CaseReportCriteria criteria) {
        LoginUser user = currentUser();
        return Result.success(caseReportService.drillDown(criteria,
                user != null ? user.getId() : null,
                user != null ? user.getRoles() : null));
    }

    private static LoginUser currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BusinessException("未登录");
        }
        if (auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BusinessException("未登录");
    }
}

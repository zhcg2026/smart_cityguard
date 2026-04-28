package com.cityguard.config.controller;

import com.cityguard.config.entity.CategoryBig;
import com.cityguard.config.entity.CategorySmall;
import com.cityguard.config.entity.CaseStandard;
import com.cityguard.config.entity.TimeLimitRule;
import com.cityguard.config.service.ConfigService;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "配置管理", description = "立结案标准、案件分类、时限配置")
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @Operation(summary = "获取大类列表")
    @GetMapping("/category/big/list")
    public Result<List<CategoryBig>> getCategoryBigList(
            @Parameter(description = "类型: 1-部件, 2-事件, 3-服务事项") @RequestParam(required = false) Integer type) {
        return Result.success(configService.getCategoryBigList(type));
    }

    @Operation(summary = "获取小类列表")
    @GetMapping("/category/small/list/{categoryBigId}")
    public Result<List<CategorySmall>> getCategorySmallList(
            @Parameter(description = "大类ID") @PathVariable Long categoryBigId) {
        return Result.success(configService.getCategorySmallList(categoryBigId));
    }

    @Operation(summary = "获取立案条件")
    @GetMapping("/standard/conditions/{categorySmallId}")
    public Result<List<CaseStandard>> getConditions(
            @Parameter(description = "小类ID") @PathVariable Long categorySmallId) {
        return Result.success(configService.getConditions(categorySmallId));
    }

    @Operation(summary = "获取时限规则")
    @GetMapping("/timelimit/{categorySmallId}")
    public Result<TimeLimitRule> getTimeLimitRule(
            @Parameter(description = "小类ID") @PathVariable Long categorySmallId) {
        return Result.success(configService.getTimeLimitRule(categorySmallId));
    }
}
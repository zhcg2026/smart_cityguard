package com.cityguard.config.controller;

import com.cityguard.config.dto.SmallTimeLimitRowVO;
import com.cityguard.config.dto.TimeLimitOverrideSaveRequest;
import com.cityguard.config.entity.CategoryBig;
import com.cityguard.config.entity.CategorySmall;
import com.cityguard.config.entity.CaseStandard;
import com.cityguard.config.entity.CategoryTimeLimitOverride;
import com.cityguard.config.entity.TimeLimitRule;
import com.cityguard.config.dto.CaseStandardSaveRequest;
import com.cityguard.config.dto.CategoryBigSaveRequest;
import com.cityguard.config.dto.CategorySmallSaveRequest;
import com.cityguard.config.service.CategoryCatalogService;
import com.cityguard.config.service.ConfigService;
import com.cityguard.config.service.MubanStandardImportService;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "配置管理", description = "立结案标准、案件分类、时限配置")
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;
    private final CategoryCatalogService categoryCatalogService;
    private final MubanStandardImportService mubanStandardImportService;

    @Operation(summary = "获取大类列表")
    @GetMapping("/category/big/list")
    public Result<List<CategoryBig>> getCategoryBigList(
            @Parameter(description = "类型: 1-部件, 2-事件, 3-服务事项") @RequestParam(required = false) Integer type) {
        return Result.success(configService.getCategoryBigList(type));
    }

    @Operation(summary = "获取小类列表")
    @GetMapping("/category/small/list/{categoryBigId:\\d+}")
    public Result<List<CategorySmall>> getCategorySmallList(
            @Parameter(description = "大类ID") @PathVariable Long categoryBigId) {
        return Result.success(configService.getCategorySmallList(categoryBigId));
    }

    @Operation(summary = "获取立案条件")
    @GetMapping("/standard/conditions/{categorySmallId:\\d+}")
    public Result<List<CaseStandard>> getConditions(
            @Parameter(description = "小类ID") @PathVariable Long categorySmallId) {
        return Result.success(configService.getConditions(categorySmallId));
    }

    @Operation(summary = "案件分类-大类列表（管理端，含停用）")
    @GetMapping("/category/big/manage")
    public Result<List<CategoryBig>> listCategoryBigForManage(
            @Parameter(description = "类型: 1-部件, 2-事件") @RequestParam(required = false) Integer type) {
        return Result.success(categoryCatalogService.listBigForManage(type));
    }

    @Operation(summary = "保存大类")
    @PostMapping("/category/big")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CategoryBig> saveCategoryBig(@RequestBody CategoryBigSaveRequest request) {
        return Result.success(categoryCatalogService.saveBig(request));
    }

    @Operation(summary = "删除大类")
    @DeleteMapping("/category/big/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteCategoryBig(@PathVariable Long id) {
        categoryCatalogService.deleteBig(id);
        return Result.success();
    }

    @Operation(summary = "案件分类-小类列表（管理端，含停用）")
    @GetMapping("/category/small/manage/{bigId:\\d+}")
    public Result<List<CategorySmall>> listCategorySmallForManage(@PathVariable Long bigId) {
        return Result.success(categoryCatalogService.listSmallForManage(bigId));
    }

    @Operation(summary = "保存小类")
    @PostMapping("/category/small")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CategorySmall> saveCategorySmall(@RequestBody CategorySmallSaveRequest request) {
        return Result.success(categoryCatalogService.saveSmall(request));
    }

    @Operation(summary = "删除小类")
    @DeleteMapping("/category/small/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteCategorySmall(@PathVariable Long id) {
        categoryCatalogService.deleteSmall(id);
        return Result.success();
    }

    @Operation(summary = "立案条件列表（管理端，含停用）")
    @GetMapping("/standard/manage/{smallId:\\d+}")
    public Result<List<CaseStandard>> listStandardsForManage(@PathVariable Long smallId) {
        return Result.success(categoryCatalogService.listStandardsForManage(smallId));
    }

    @Operation(summary = "保存立案条件")
    @PostMapping("/standard")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CaseStandard> saveCaseStandard(@RequestBody CaseStandardSaveRequest request) {
        return Result.success(categoryCatalogService.saveStandard(request));
    }

    @Operation(summary = "删除立案条件")
    @DeleteMapping("/standard/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteCaseStandard(@PathVariable Long id) {
        categoryCatalogService.deleteStandard(id);
        return Result.success();
    }

    @Operation(summary = "导入立结案标准模板（muban.xlsx）", description = "仅管理员；按 Sheet「部件」「事件」全量替换对应分类与标准")
    @PostMapping("/standard/import-muban")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> importMubanStandard(
            @Parameter(description = "Excel 文件") @RequestParam("file") MultipartFile file) throws IOException {
        return Result.success(mubanStandardImportService.importToMap(file));
    }

    @Operation(summary = "获取时限规则")
    @GetMapping("/timelimit/{categorySmallId:\\d+}")
    public Result<TimeLimitRule> getTimeLimitRule(
            @Parameter(description = "小类ID") @PathVariable Long categorySmallId) {
        return Result.success(configService.getTimeLimitRule(categorySmallId));
    }

    @Operation(summary = "全局时限计算规则列表")
    @GetMapping("/timelimit/list")
    public Result<List<TimeLimitRule>> listTimeLimitRules() {
        return Result.success(configService.listTimeLimitRules());
    }

    @Operation(summary = "大类下小类时限一览（含标准默认与覆盖）")
    @GetMapping("/timelimit/small/list")
    public Result<List<SmallTimeLimitRowVO>> listSmallTimeLimits(
            @Parameter(description = "大类ID") @RequestParam Long bigId) {
        return Result.success(configService.listSmallTimeLimits(bigId));
    }

    @Operation(summary = "保存小类时限覆盖")
    @PostMapping("/timelimit/override")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CategoryTimeLimitOverride> saveTimeLimitOverride(
            @RequestBody TimeLimitOverrideSaveRequest request) {
        return Result.success(configService.saveTimeLimitOverride(request));
    }

    @Operation(summary = "删除小类时限覆盖")
    @DeleteMapping("/timelimit/override/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteTimeLimitOverride(@PathVariable Long id) {
        configService.deleteTimeLimitOverride(id);
        return Result.success();
    }
}
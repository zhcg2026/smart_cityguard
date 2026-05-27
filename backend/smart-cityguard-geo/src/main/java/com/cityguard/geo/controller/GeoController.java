package com.cityguard.geo.controller;

import com.cityguard.common.result.Result;
import com.cityguard.geo.entity.ResponsibilityGrid;
import com.cityguard.geo.entity.SysCommunity;
import com.cityguard.geo.entity.SysGrid;
import com.cityguard.geo.entity.SysStreet;
import com.cityguard.geo.service.GeoService;
import com.cityguard.geo.service.RespGridService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "地理信息", description = "街道社区、网格、责任片区")
@RestController
@RequestMapping("/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeoService geoService;
    private final RespGridService respGridService;

    @Operation(summary = "获取街道列表")
    @GetMapping("/street/list")
    public Result<List<SysStreet>> getStreetList() {
        return Result.success(geoService.getStreetList());
    }

    @Operation(summary = "获取社区列表")
    @GetMapping("/community/list/{streetId}")
    public Result<List<SysCommunity>> getCommunityList(@PathVariable Long streetId) {
        return Result.success(geoService.getCommunityList(streetId));
    }

    @Operation(summary = "获取网格列表")
    @GetMapping("/grid/list/{communityId}")
    public Result<List<SysGrid>> getGridList(@PathVariable Long communityId) {
        return Result.success(geoService.getGridList(communityId));
    }

    @Operation(summary = "获取网格详情")
    @GetMapping("/grid/{id}")
    public Result<SysGrid> getGridInfo(@PathVariable Long id) {
        return Result.success(geoService.getGridInfo(id));
    }

    @Operation(summary = "根据坐标查找最近网格")
    @GetMapping("/grid/info")
    public Result<SysGrid> findNearestGrid(
            @Parameter(description = "经度") @RequestParam Double lng,
            @Parameter(description = "纬度") @RequestParam Double lat) {
        return Result.success(geoService.findNearestGrid(lng, lat));
    }

    // ---------- 责任网格（片区），路径与原先 RespGridController 一致 ----------

    @Operation(summary = "查询所有片区列表", tags = {"片区管理"})
    @GetMapping("/resp-grid/list")
    public Result<List<ResponsibilityGrid>> respGridListAll() {
        return Result.success(respGridService.listAll());
    }

    @Operation(summary = "查询片区详情", tags = {"片区管理"})
    @GetMapping("/resp-grid/{id:\\d+}")
    public Result<ResponsibilityGrid> respGridGetById(@PathVariable Long id) {
        return Result.success(respGridService.getById(id));
    }

    @Operation(summary = "新建片区", tags = {"片区管理"})
    @PostMapping("/resp-grid")
    public Result<Void> respGridCreate(@RequestBody ResponsibilityGrid respGrid) {
        respGridService.create(respGrid);
        return Result.success();
    }

    @Operation(summary = "更新片区", tags = {"片区管理"})
    @PutMapping("/resp-grid/{id:\\d+}")
    public Result<Void> respGridUpdate(@PathVariable Long id, @RequestBody ResponsibilityGrid respGrid) {
        respGrid.setId(id);
        respGridService.update(respGrid);
        return Result.success();
    }

    @Operation(summary = "删除片区", tags = {"片区管理"})
    @DeleteMapping("/resp-grid/{id:\\d+}")
    public Result<Void> respGridDelete(@PathVariable Long id) {
        respGridService.delete(id);
        return Result.success();
    }

    @Operation(summary = "导入GeoJSON文件创建片区", tags = {"片区管理"})
    @PostMapping("/resp-grid/import")
    public Result<Map<String, Object>> respGridImportGeoJson(
            @Parameter(description = "GeoJSON文件") @RequestParam("file") MultipartFile file) {
        return Result.success(respGridService.importGeoJson(file));
    }

    @Operation(summary = "设置片区绑定的采集员（全量替换，空数组表示清空）", tags = {"片区管理"})
    @PutMapping("/resp-grid/{id:\\d+}/collectors")
    public Result<Void> respGridSetCollectors(
            @PathVariable Long id,
            @RequestBody(required = false) List<Long> userIds) {
        respGridService.setGridCollectors(id, userIds != null ? userIds : List.of());
        return Result.success();
    }

    @Operation(summary = "查询采集员所属片区列表", tags = {"片区管理"})
    @GetMapping("/resp-grid/collector/{userId:\\d+}")
    public Result<List<ResponsibilityGrid>> respGridListByCollector(@PathVariable Long userId) {
        return Result.success(respGridService.listGridsByCollectorUserId(userId));
    }

    @Operation(summary = "校验坐标是否在指定片区内", tags = {"片区管理"})
    @PostMapping("/resp-grid/check-location")
    public Result<Boolean> respGridCheckLocation(
            @Parameter(description = "片区ID") @RequestParam Long respGridId,
            @Parameter(description = "经度") @RequestParam Double lng,
            @Parameter(description = "纬度") @RequestParam Double lat) {
        return Result.success(respGridService.checkPointInArea(respGridId, lng, lat));
    }

    @Operation(summary = "校验采集员是否可在该坐标上报", tags = {"片区管理"})
    @PostMapping("/resp-grid/validate-collector-report")
    public Result<ResponsibilityGrid> validateCollectorReport(
            @Parameter(description = "采集员用户ID") @RequestParam Long userId,
            @Parameter(description = "经度") @RequestParam Double lng,
            @Parameter(description = "纬度") @RequestParam Double lat) {
        return Result.success(respGridService.validateCollectorReportLocation(userId, lng, lat));
    }
}

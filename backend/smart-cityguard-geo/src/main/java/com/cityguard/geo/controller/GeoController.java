package com.cityguard.geo.controller;

import com.cityguard.geo.entity.SysStreet;
import com.cityguard.geo.entity.SysCommunity;
import com.cityguard.geo.entity.SysGrid;
import com.cityguard.geo.service.GeoService;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "地理信息", description = "街道社区、网格管理")
@RestController
@RequestMapping("/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeoService geoService;

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
}
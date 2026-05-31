package com.cityguard.timer.controller;

import com.cityguard.common.result.Result;
import com.cityguard.timer.entity.HolidayConfig;
import com.cityguard.timer.entity.WorkTimeConfig;
import com.cityguard.timer.service.TimerConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "计时规则", description = "工作时段、节假日")
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class TimerConfigController {

    private final TimerConfigService timerConfigService;

    @Operation(summary = "工作时段配置列表")
    @GetMapping("/worktime/list")
    public Result<List<WorkTimeConfig>> listWorkTimeConfigs() {
        return Result.success(timerConfigService.listWorkTimeConfigs());
    }

    @Operation(summary = "更新工作时段配置")
    @PutMapping("/worktime/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<WorkTimeConfig> updateWorkTimeConfig(
            @PathVariable Long id, @RequestBody WorkTimeConfig payload) {
        return Result.success(timerConfigService.updateWorkTimeConfig(id, payload));
    }

    @Operation(summary = "节假日列表")
    @GetMapping("/holiday/list")
    public Result<List<HolidayConfig>> listHolidays(
            @Parameter(description = "年份，默认当年") @RequestParam(required = false) Integer year) {
        return Result.success(timerConfigService.listHolidays(year));
    }

    @Operation(summary = "保存节假日")
    @PostMapping("/holiday")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<HolidayConfig> saveHoliday(@RequestBody HolidayConfig payload) {
        return Result.success(timerConfigService.saveHoliday(payload));
    }

    @Operation(summary = "删除节假日")
    @DeleteMapping("/holiday/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteHoliday(@PathVariable Long id) {
        timerConfigService.deleteHoliday(id);
        return Result.success();
    }
}

package com.cityguard.appeal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.appeal.entity.AppealApply;
import com.cityguard.appeal.entity.AppealAttachment;
import com.cityguard.appeal.service.AppealService;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "申诉管理", description = "申诉申请、审核")
@RestController
@RequestMapping("/appeal")
@RequiredArgsConstructor
public class AppealController {

    private final AppealService appealService;

    @Operation(summary = "提交申诉")
    @PostMapping("/submit")
    public Result<AppealApply> submitAppeal(@RequestBody Map<String, Object> appealData) {
        return Result.success(appealService.submitAppeal(appealData));
    }

    @Operation(summary = "获取申诉详情")
    @GetMapping("/{id}")
    public Result<AppealApply> getAppealDetail(@PathVariable Long id) {
        return Result.success(appealService.getAppealDetail(id));
    }

    @Operation(summary = "获取申诉列表")
    @GetMapping("/list")
    public Result<Page<AppealApply>> getAppealList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long applicantId) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("applicantId", applicantId);
        return Result.success(appealService.getAppealList(pageNum, pageSize, params));
    }

    @Operation(summary = "获取我的申诉")
    @GetMapping("/my/{applicantId}")
    public Result<List<AppealApply>> getMyAppeals(@PathVariable Long applicantId) {
        return Result.success(appealService.getMyAppeals(applicantId));
    }

    @Operation(summary = "获取待审核申诉")
    @GetMapping("/pending")
    public Result<List<AppealApply>> getPendingAppeals() {
        return Result.success(appealService.getPendingAppeals());
    }

    @Operation(summary = "审核申诉")
    @PostMapping("/review")
    public Result<AppealApply> reviewAppeal(@RequestBody ReviewRequest request) {
        return Result.success(appealService.reviewAppeal(
            request.getAppealId(), request.getResult(), request.getRemark(), request.getReviewerId()));
    }

    @Operation(summary = "获取申诉附件")
    @GetMapping("/{id}/attachments")
    public Result<List<AppealAttachment>> getAppealAttachments(@PathVariable Long id) {
        return Result.success(appealService.getAppealAttachments(id));
    }

    @Data
    public static class ReviewRequest {
        private Long appealId;
        private Integer result;
        private String remark;
        private Long reviewerId;
    }
}
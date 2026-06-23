package com.cityguard.appeal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.appeal.dto.TimeoutAppealDetailVo;
import com.cityguard.appeal.dto.TimeoutAppealReviewRequest;
import com.cityguard.appeal.dto.TimeoutAppealSubmitRequest;
import com.cityguard.appeal.entity.AppealApply;
import com.cityguard.appeal.service.TimeoutAppealService;
import com.cityguard.auth.entity.LoginUser;
import com.cityguard.caseinfo.entity.CaseInfo;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "处置超时申诉", description = "部门申诉、派遣员初审、受理员二审")
@RestController
@RequestMapping("/appeal/timeout")
@RequiredArgsConstructor
public class TimeoutAppealController {

    private final TimeoutAppealService timeoutAppealService;

    @Operation(summary = "部门提交处置超时申诉")
    @PostMapping("/submit")
    public Result<AppealApply> submit(@RequestBody TimeoutAppealSubmitRequest request) {
        return Result.success(timeoutAppealService.submit(request, currentUser()));
    }

    @Operation(summary = "部门审核（处置人员提交的申诉）")
    @PostMapping("/dept-review")
    public Result<AppealApply> deptReview(@RequestBody TimeoutAppealReviewRequest request) {
        return Result.success(timeoutAppealService.deptReview(request, currentUser()));
    }

    @Operation(summary = "派遣员初审")
    @PostMapping("/dispatcher-review")
    public Result<AppealApply> dispatcherReview(@RequestBody TimeoutAppealReviewRequest request) {
        return Result.success(timeoutAppealService.dispatcherReview(request, currentUser()));
    }

    @Operation(summary = "受理员二审")
    @PostMapping("/acceptor-review")
    public Result<AppealApply> acceptorReview(@RequestBody TimeoutAppealReviewRequest request) {
        return Result.success(timeoutAppealService.acceptorReview(request, currentUser()));
    }

    @Operation(summary = "可申诉案件列表（超时已结案且未申诉）")
    @GetMapping("/appealable")
    public Result<Page<CaseInfo>> appealableCases(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String caseCode) {
        return Result.success(timeoutAppealService.listAppealableCases(pageNum, pageSize, caseCode, currentUser()));
    }

    @Operation(summary = "申诉详情")
    @GetMapping("/detail/{id:\\d+}")
    public Result<TimeoutAppealDetailVo> detail(@PathVariable Long id) {
        return Result.success(timeoutAppealService.getDetail(id, currentUser()));
    }

    @Operation(summary = "按案件查询申诉")
    @GetMapping("/by-case/{caseId:\\d+}")
    public Result<AppealApply> byCase(@PathVariable Long caseId) {
        return Result.success(timeoutAppealService.getByCaseId(caseId));
    }

    @Operation(summary = "申诉列表")
    @GetMapping("/list")
    public Result<Page<AppealApply>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String caseCode) {
        return Result.success(timeoutAppealService.list(pageNum, pageSize, tab, caseCode, currentUser()));
    }

    private static LoginUser currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException("未登录");
        }
        return loginUser;
    }
}

package com.cityguard.caseinfo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.auth.entity.LoginUser;
import com.cityguard.caseinfo.dto.CaseAdjustmentApplyRequest;
import com.cityguard.caseinfo.dto.CaseAdjustmentReviewRequest;
import com.cityguard.caseinfo.entity.CaseAdjustmentApply;
import com.cityguard.caseinfo.service.CaseAdjustmentService;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "案件延期挂账", description = "处置部门申请、派遣员审批")
@RestController
@RequestMapping("/case/adjustment")
@RequiredArgsConstructor
public class CaseAdjustmentController {

    private final CaseAdjustmentService caseAdjustmentService;

    @Operation(summary = "处置部门提交延期/挂账申请")
    @PostMapping("/apply")
    public Result<CaseAdjustmentApply> apply(@RequestBody CaseAdjustmentApplyRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = displayName(user);
        return Result.success(caseAdjustmentService.apply(
                request, user.getId(), opName, user.getRoles()));
    }

    @Operation(summary = "派遣员待审列表")
    @GetMapping("/pending")
    public Result<Page<CaseAdjustmentApply>> listPending(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        return Result.success(caseAdjustmentService.listPending(
                pageNum, pageSize, user.getId(), user.getRoles()));
    }

    @Operation(summary = "派遣员审批延期/挂账")
    @PostMapping("/review")
    public Result<CaseAdjustmentApply> review(@RequestBody CaseAdjustmentReviewRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = displayName(user);
        return Result.success(caseAdjustmentService.review(
                request, user.getId(), opName, user.getRoles()));
    }

    @Operation(summary = "案件延期/挂账申请记录")
    @GetMapping("/list/{caseId}")
    public Result<List<CaseAdjustmentApply>> listByCase(@PathVariable Long caseId) {
        return Result.success(caseAdjustmentService.listByCaseId(caseId));
    }

    private static LoginUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    private static String displayName(LoginUser user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        return user.getUsername();
    }
}

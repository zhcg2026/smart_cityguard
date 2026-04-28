package com.cityguard.case.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.case.entity.CaseInfo;
import com.cityguard.case.entity.CaseFlowRecord;
import com.cityguard.case.entity.CaseAttachment;
import com.cityguard.case.service.CaseService;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "案件管理", description = "案件上报、立案、派遣、处置、核查、核实、结案")
@RestController
@RequestMapping("/case")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @Operation(summary = "问题上报")
    @PostMapping("/report")
    public Result<CaseInfo> reportCase(@RequestBody Map<String, Object> reportData) {
        return Result.success(caseService.reportCase(reportData));
    }

    @Operation(summary = "获取案件详情")
    @GetMapping("/{id}")
    public Result<CaseInfo> getCaseDetail(@PathVariable Long id) {
        return Result.success(caseService.getCaseDetail(id));
    }

    @Operation(summary = "获取案件列表")
    @GetMapping("/list")
    public Result<Page<CaseInfo>> getCaseList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String caseNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long categoryBigId) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("caseNo", caseNo);
        params.put("status", status);
        params.put("categoryBigId", categoryBigId);
        return Result.success(caseService.getCaseList(pageNum, pageSize, params));
    }

    @Operation(summary = "获取待处理案件")
    @GetMapping("/pending")
    public Result<List<CaseInfo>> getPendingCases(@RequestParam Integer status) {
        return Result.success(caseService.getPendingCases(status));
    }

    @Operation(summary = "立案")
    @PostMapping("/register")
    public Result<CaseInfo> registerCase(@RequestBody ProcessRequest request) {
        return Result.success(caseService.registerCase(request.getCaseId(), request.getRemark()));
    }

    @Operation(summary = "派遣")
    @PostMapping("/dispatch")
    public Result<CaseInfo> dispatchCase(@RequestBody DispatchRequest request) {
        return Result.success(caseService.dispatchCase(request.getCaseId(), request.getDepartmentId(), request.getRemark()));
    }

    @Operation(summary = "处置")
    @PostMapping("/handle")
    public Result<CaseInfo> handleCase(@RequestBody HandleRequest request) {
        return Result.success(caseService.handleCase(request.getCaseId(), request.getRemark(), request.getAttachments()));
    }

    @Operation(summary = "核查")
    @PostMapping("/verify")
    public Result<CaseInfo> verifyCase(@RequestBody VerifyRequest request) {
        return Result.success(caseService.verifyCase(request.getCaseId(), request.getResult(), request.getRemark(), request.getAttachments()));
    }

    @Operation(summary = "核实")
    @PostMapping("/check")
    public Result<CaseInfo> checkCase(@RequestBody CheckRequest request) {
        return Result.success(caseService.checkCase(request.getCaseId(), request.getResult(), request.getRemark(), request.getAttachments()));
    }

    @Operation(summary = "结案")
    @PostMapping("/close")
    public Result<CaseInfo> closeCase(@RequestBody ProcessRequest request) {
        return Result.success(caseService.closeCase(request.getCaseId(), request.getRemark()));
    }

    @Operation(summary = "不受理")
    @PostMapping("/reject")
    public Result<CaseInfo> rejectCase(@RequestBody RejectRequest request) {
        return Result.success(caseService.rejectCase(request.getCaseId(), request.getReason()));
    }

    @Operation(summary = "获取流程记录")
    @GetMapping("/{id}/flow")
    public Result<List<CaseFlowRecord>> getFlowRecords(@PathVariable Long id) {
        return Result.success(caseService.getFlowRecords(id));
    }

    @Operation(summary = "获取附件列表")
    @GetMapping("/{id}/attachments")
    public Result<List<CaseAttachment>> getAttachments(@PathVariable Long id) {
        return Result.success(caseService.getAttachments(id));
    }

    @Data
    public static class ProcessRequest {
        private Long caseId;
        private String remark;
    }

    @Data
    public static class DispatchRequest {
        private Long caseId;
        private Long departmentId;
        private String remark;
    }

    @Data
    public static class HandleRequest {
        private Long caseId;
        private String remark;
        private List<String> attachments;
    }

    @Data
    public static class VerifyRequest {
        private Long caseId;
        private Integer result;
        private String remark;
        private List<String> attachments;
    }

    @Data
    public static class CheckRequest {
        private Long caseId;
        private Integer result;
        private String remark;
        private List<String> attachments;
    }

    @Data
    public static class RejectRequest {
        private Long caseId;
        private String reason;
    }
}
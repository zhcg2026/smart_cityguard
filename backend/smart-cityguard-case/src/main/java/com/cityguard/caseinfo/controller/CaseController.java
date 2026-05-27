package com.cityguard.caseinfo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.caseinfo.dto.CaseAcceptorRegisterRequest;
import com.cityguard.caseinfo.dto.CaseAssignHandlerRequest;
import com.cityguard.caseinfo.dto.CaseDashboardStatsDto;
import com.cityguard.caseinfo.dto.CaseDashboardTodosDto;
import com.cityguard.caseinfo.dto.CaseDeleteRequest;
import com.cityguard.caseinfo.dto.CaseDeptConfirmRequest;
import com.cityguard.caseinfo.dto.CaseAcceptorReturnDispatcherRequest;
import com.cityguard.caseinfo.dto.CaseDeptReturnRequest;
import com.cityguard.caseinfo.dto.CaseDispatcherForwardRequest;
import com.cityguard.caseinfo.dto.CaseDispatcherReturnAcceptorRequest;
import com.cityguard.caseinfo.dto.CaseRegisterRequest;
import com.cityguard.caseinfo.dto.CaseReturnRequest;
import com.cityguard.caseinfo.dto.CaseRevokeAssignRequest;
import com.cityguard.caseinfo.dto.CaseSendTaskRequest;
import com.cityguard.caseinfo.entity.CaseInfo;
import com.cityguard.caseinfo.entity.CaseFlowRecord;
import com.cityguard.caseinfo.entity.CaseAttachment;
import com.cityguard.auth.entity.LoginUser;
import com.cityguard.caseinfo.service.CaseService;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @Operation(summary = "受理员案件登记（电话投诉等人工录入，入库为待立案）")
    @PostMapping("/acceptor-register")
    public Result<CaseInfo> acceptorRegisterCase(@RequestBody CaseAcceptorRegisterRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName() : user.getUsername();
        return Result.success(caseService.acceptorRegisterCase(
                request, user.getId(), opName, user.getRoles()));
    }

    // 固定路径的 GET 必须写在 /{id} 之前；单段路径易被当成 /{id}，「我的上报」使用 /reporter/list

    @Operation(summary = "获取案件列表")
    @GetMapping("/list")
    public Result<Page<CaseInfo>> getCaseList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String caseCode,
            @RequestParam(required = false) String caseStatus,
            @RequestParam(required = false) Long smallId,
            @RequestParam(required = false) Long categoryBigId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String statGroup) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("caseCode", caseCode);
        params.put("caseStatus", caseStatus);
        params.put("smallId", smallId);
        params.put("categoryBigId", categoryBigId);
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        params.put("statGroup", statGroup);
        LoginUser user = currentUser();
        Long uid = user != null ? user.getId() : null;
        List<String> roles = user != null ? user.getRoles() : null;
        return Result.success(caseService.getCaseList(pageNum, pageSize, params, uid, roles));
    }

    @Operation(summary = "采集员我的上报（分页）")
    @GetMapping("/reporter/list")
    public Result<Page<CaseInfo>> getMyCaseList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long uid = currentUserId();
        if (uid == null) {
            throw new BusinessException("未登录");
        }
        return Result.success(caseService.getMyCaseList(uid, pageNum, pageSize));
    }

    @Operation(summary = "获取待处理案件，status=acceptor_todo 时合并待立案+待核查（受理员统一待办）")
    @GetMapping("/pending")
    public Result<Page<CaseInfo>> getPendingCases(
            @RequestParam String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        LoginUser user = currentUser();
        Long uid = user != null ? user.getId() : null;
        List<String> roles = user != null ? user.getRoles() : null;
        return Result.success(caseService.getPendingCases(status, pageNum, pageSize, uid, roles));
    }

    @Operation(summary = "工作台案件统计")
    @GetMapping("/dashboard/stats")
    public Result<CaseDashboardStatsDto> getDashboardStats() {
        LoginUser user = currentUser();
        Long uid = user != null ? user.getId() : null;
        List<String> roles = user != null ? user.getRoles() : null;
        return Result.success(caseService.getDashboardStats(uid, roles));
    }

    @Operation(summary = "工作台待办事项（按角色聚合案件待办）")
    @GetMapping("/dashboard/todos")
    public Result<CaseDashboardTodosDto> getDashboardTodos(
            @RequestParam(defaultValue = "10") Integer limit) {
        LoginUser user = currentUser();
        Long uid = user != null ? user.getId() : null;
        List<String> roles = user != null ? user.getRoles() : null;
        return Result.success(caseService.getDashboardTodos(uid, roles, limit));
    }

    @Operation(summary = "获取流程记录")
    @GetMapping("/{id:\\d+}/flow")
    public Result<List<CaseFlowRecord>> getFlowRecords(@PathVariable Long id) {
        return Result.success(caseService.getFlowRecords(id));
    }

    @Operation(summary = "获取附件列表")
    @GetMapping("/{id:\\d+}/attachments")
    public Result<List<CaseAttachment>> getAttachments(@PathVariable Long id) {
        return Result.success(caseService.getAttachments(id));
    }

    @Operation(summary = "案件核查任务记录（含采集员现场照片）")
    @GetMapping("/{id:\\d+}/check-task-records")
    public Result<java.util.List<com.cityguard.task.dto.CheckTaskRecordView>> listCheckTaskRecords(
            @PathVariable Long id) {
        LoginUser user = currentUser();
        Long uid = user != null ? user.getId() : null;
        List<String> roles = user != null ? user.getRoles() : null;
        return Result.success(caseService.listCheckTaskRecords(id, uid, roles));
    }

    @Operation(summary = "案件核实任务记录（含采集员现场照片）")
    @GetMapping("/{id:\\d+}/verify-task-records")
    public Result<java.util.List<com.cityguard.task.dto.VerifyTaskRecordView>> listVerifyTaskRecords(
            @PathVariable Long id) {
        LoginUser user = currentUser();
        Long uid = user != null ? user.getId() : null;
        List<String> roles = user != null ? user.getRoles() : null;
        return Result.success(caseService.listVerifyTaskRecords(id, uid, roles));
    }

    @Operation(summary = "获取案件详情")
    @GetMapping("/{id:\\d+}")
    public Result<CaseInfo> getCaseDetail(@PathVariable Long id) {
        LoginUser user = currentUser();
        Long uid = user != null ? user.getId() : null;
        List<String> roles = user != null ? user.getRoles() : null;
        return Result.success(caseService.getCaseDetail(id, uid, roles));
    }

    private Long currentUserId() {
        LoginUser u = currentUser();
        return u != null ? u.getId() : null;
    }

    private LoginUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    @Operation(summary = "立案（可修正案件信息；携带 clientUpdateTime 防并发覆盖）")
    @PostMapping("/register")
    public Result<CaseInfo> registerCase(@RequestBody CaseRegisterRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
        return Result.success(caseService.registerCase(request, user.getId(), opName));
    }

    @Operation(summary = "派遣（携带 clientUpdateTime 防并发）")
    @PostMapping("/dispatch")
    public Result<CaseInfo> dispatchCase(@RequestBody DispatchRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        return Result.success(caseService.dispatchCase(
                request.getCaseId(),
                request.getDepartmentId(),
                request.getRemark(),
                request.getClientUpdateTime(),
                user.getId(),
                user.getRoles()));
    }

    @Operation(summary = "处置部门指派具体处置人员")
    @PostMapping("/assign-handler")
    public Result<CaseInfo> assignHandler(@RequestBody CaseAssignHandlerRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
        return Result.success(caseService.assignHandler(request, user.getId(), opName, user.getRoles()));
    }

    @Operation(summary = "处置人员提交处置结果")
    @PostMapping("/handle")
    public Result<CaseInfo> handleCase(@RequestBody HandleRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        return Result.success(caseService.handleCase(
                request.getCaseId(),
                request.getRemark(),
                request.getAttachments(),
                user.getId(),
                user.getRoles()));
    }

    @Operation(summary = "处置部门确认处置结果并批转派遣员")
    @PostMapping("/dept-confirm")
    public Result<CaseInfo> deptConfirmCase(@RequestBody CaseDeptConfirmRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
        return Result.success(caseService.deptConfirmCase(request, user.getId(), opName, user.getRoles()));
    }

    @Operation(summary = "处置部门撤销指派")
    @PostMapping("/dept-revoke-assign")
    public Result<CaseInfo> deptRevokeAssign(@RequestBody CaseRevokeAssignRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
        return Result.success(caseService.deptRevokeAssign(request, user.getId(), opName, user.getRoles()));
    }

    @Operation(summary = "处置部门打回处置人员再处置")
    @PostMapping("/dept-return-handler")
    public Result<CaseInfo> deptReturnHandler(@RequestBody CaseReturnRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
        return Result.success(caseService.deptReturnHandler(request, user.getId(), opName, user.getRoles()));
    }

    @Operation(summary = "处置人员回退至处置部门（提交前）")
    @PostMapping("/handler-return-dept")
    public Result<CaseInfo> handlerReturnDept(@RequestBody CaseReturnRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        return Result.success(caseService.handlerReturnDept(request, user.getId(), user.getRoles()));
    }

    @Operation(summary = "派遣员回退受理员（非本局）")
    @PostMapping("/dispatcher-return-acceptor")
    public Result<CaseInfo> dispatcherReturnAcceptor(@RequestBody CaseDispatcherReturnAcceptorRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
        return Result.success(caseService.dispatcherReturnAcceptor(
                request, user.getId(), opName, user.getRoles()));
    }

    @Operation(summary = "派遣员打回处置部门返工")
    @PostMapping("/dispatcher-return-dept")
    public Result<CaseInfo> dispatcherReturnDept(@RequestBody CaseReturnRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
        return Result.success(caseService.dispatcherReturnDept(request, user.getId(), opName, user.getRoles()));
    }

    @Operation(summary = "受理员回退派遣员（处置不达标，原部门返工）")
    @PostMapping("/acceptor-return-dispatcher")
    public Result<CaseInfo> acceptorReturnDispatcher(@RequestBody CaseAcceptorReturnDispatcherRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
        return Result.success(caseService.acceptorReturnDispatcher(
                request, user.getId(), opName, user.getRoles()));
    }

    @Operation(summary = "处置部门回退案件至原派遣员")
    @PostMapping("/dept-return")
    public Result<CaseInfo> deptReturnCase(@RequestBody CaseDeptReturnRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
        return Result.success(caseService.deptReturnCase(request, user.getId(), opName, user.getRoles()));
    }

    @Operation(summary = "派遣员把关通过，批转指定受理员（受理员岗位均可核实/结案）")
    @PostMapping("/dispatcher-forward-acceptor")
    public Result<CaseInfo> dispatcherForwardToAcceptor(@RequestBody CaseDispatcherForwardRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
        return Result.success(caseService.dispatcherForwardToAcceptor(
                request, user.getId(), opName, user.getRoles()));
    }

    @Operation(summary = "可选采集员列表（推荐距离案发地最近，含最近上报位置）")
    @GetMapping("/{id:\\d+}/collector-candidates")
    public Result<java.util.List<com.cityguard.caseinfo.dto.CollectorCandidateDto>> listCollectorCandidates(
            @PathVariable Long id) {
        LoginUser user = currentUser();
        Long uid = user != null ? user.getId() : null;
        List<String> roles = user != null ? user.getRoles() : null;
        return Result.success(caseService.listCollectorCandidates(id, uid, roles));
    }

    @Operation(summary = "可选：立案前下发核查任务（非必经）")
    @PostMapping("/send-check")
    public Result<CaseInfo> sendCheckTask(@RequestBody CaseSendTaskRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
        return Result.success(caseService.sendCheckTask(request, user.getId(), opName, user.getRoles()));
    }

    @Operation(summary = "可选：结案前下发核实任务（非必经）")
    @PostMapping("/send-verify")
    public Result<CaseInfo> sendVerifyTask(@RequestBody CaseSendTaskRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        String opName = user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
        return Result.success(caseService.sendVerifyTask(request, user.getId(), opName, user.getRoles()));
    }

    @Operation(summary = "核查（已废弃，请用 send-check）")
    @PostMapping("/verify")
    public Result<CaseInfo> verifyCase(@RequestBody VerifyRequest request) {
        return Result.success(caseService.verifyCase(request.getCaseId(), request.getResult(), request.getRemark(), request.getAttachments()));
    }

    @Operation(summary = "核实")
    @PostMapping("/check")
    public Result<CaseInfo> checkCase(@RequestBody CheckRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        return Result.success(caseService.checkCase(
                request.getCaseId(),
                request.getResult(),
                request.getRemark(),
                request.getAttachments(),
                user.getId(),
                user.getRoles()));
    }

    @Operation(summary = "结案")
    @PostMapping("/close")
    public Result<CaseInfo> closeCase(@RequestBody ProcessRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        return Result.success(caseService.closeCase(
                request.getCaseId(),
                request.getRemark(),
                user.getId(),
                user.getRoles()));
    }

    @Operation(summary = "不受理")
    @PostMapping("/reject")
    public Result<CaseInfo> rejectCase(@RequestBody RejectRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        return Result.success(caseService.rejectCase(
                request.getCaseId(),
                request.getReason(),
                user.getId(),
                user.getRealName() != null ? user.getRealName() : user.getUsername()));
    }

    @Operation(summary = "管理员删除案件（逻辑删除，支持批量）")
    @PostMapping("/delete")
    public Result<Integer> deleteCases(@RequestBody CaseDeleteRequest request) {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        List<Long> ids = request != null ? request.getCaseIds() : null;
        int count = caseService.deleteCases(ids, user.getRoles());
        return Result.success(count);
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
        /** 打开详情时的 updateTime，与立案一致，用于多派遣员并发 */
        private LocalDateTime clientUpdateTime;
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
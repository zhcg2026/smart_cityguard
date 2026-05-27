package com.cityguard.task.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.auth.entity.LoginUser;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.common.result.Result;
import com.cityguard.task.entity.CheckTask;
import com.cityguard.task.entity.VerifyTask;
import com.cityguard.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "任务管理", description = "核查/核实任务台账与采集员执行")
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "核实任务列表（结案前可选分支）")
    @GetMapping("/verify/list")
    public Result<Page<VerifyTask>> getVerifyTaskList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long collectorId,
            @RequestParam(required = false) Long caseId,
            @RequestParam(required = false) String caseCode,
            @RequestParam(required = false, defaultValue = "false") Boolean allScope) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        applyCollectorScope(params, collectorId, allScope);
        params.put("caseId", caseId);
        params.put("caseCode", caseCode);
        return Result.success(taskService.getVerifyTaskList(pageNum, pageSize, params));
    }

    @Operation(summary = "核查任务列表（立案前可选分支）")
    @GetMapping("/check/list")
    public Result<Page<CheckTask>> getCheckTaskList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long collectorId,
            @RequestParam(required = false) Long caseId,
            @RequestParam(required = false) String caseCode,
            @RequestParam(required = false, defaultValue = "false") Boolean allScope) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        applyCollectorScope(params, collectorId, allScope);
        params.put("caseId", caseId);
        params.put("caseCode", caseCode);
        return Result.success(taskService.getCheckTaskList(pageNum, pageSize, params));
    }

    @Operation(summary = "核实任务详情")
    @GetMapping("/verify/{id:\\d+}")
    public Result<VerifyTask> getVerifyTaskDetail(@PathVariable Long id) {
        return Result.success(taskService.getVerifyTaskDetail(id));
    }

    @Operation(summary = "核查任务详情")
    @GetMapping("/check/{id:\\d+}")
    public Result<CheckTask> getCheckTaskDetail(@PathVariable Long id) {
        return Result.success(taskService.getCheckTaskDetail(id));
    }

    @Operation(summary = "提交核实结果（采集员）")
    @PostMapping("/verify/execute")
    public Result<VerifyTask> executeVerifyTask(@RequestBody ExecuteRequest request) {
        LoginUser user = requireUser();
        return Result.success(taskService.executeVerifyTask(
                request.getTaskId(), request.getResult(), request.getRemark(),
                request.getAttachments(), user.getId()));
    }

    @Operation(summary = "提交核查结果（采集员）")
    @PostMapping("/check/execute")
    public Result<CheckTask> executeCheckTask(@RequestBody ExecuteRequest request) {
        LoginUser user = requireUser();
        return Result.success(taskService.executeCheckTask(
                request.getTaskId(), request.getResult(), request.getRemark(),
                request.getAttachments(), user.getId()));
    }

    private static void applyCollectorScope(Map<String, Object> params, Long collectorId, Boolean allScope) {
        if (collectorId != null) {
            params.put("collectorId", collectorId);
            return;
        }
        if (Boolean.TRUE.equals(allScope)) {
            return;
        }
        Long uid = currentUserId();
        if (uid != null) {
            params.put("collectorId", uid);
        }
    }

    private static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getId();
        }
        return null;
    }

    private static LoginUser requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser && loginUser.getId() != null) {
            return loginUser;
        }
        throw new BusinessException("未登录");
    }

    @Data
    public static class ExecuteRequest {
        private Long taskId;
        /** 字符串结果码，见 TaskServiceImpl 映射 */
        private String result;
        private String remark;
        private List<String> attachments;
        private Long caseId;
    }
}

package com.cityguard.task.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.task.entity.VerifyTask;
import com.cityguard.task.entity.CheckTask;
import com.cityguard.task.service.TaskService;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "任务管理", description = "核查任务、核实任务")
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "获取核查任务详情")
    @GetMapping("/verify/{id}")
    public Result<VerifyTask> getVerifyTaskDetail(@PathVariable Long id) {
        return Result.success(taskService.getVerifyTaskDetail(id));
    }

    @Operation(summary = "获取核实任务详情")
    @GetMapping("/check/{id}")
    public Result<CheckTask> getCheckTaskDetail(@PathVariable Long id) {
        return Result.success(taskService.getCheckTaskDetail(id));
    }

    @Operation(summary = "获取核查任务列表")
    @GetMapping("/verify/list")
    public Result<Page<VerifyTask>> getVerifyTaskList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long collectorId) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("collectorId", collectorId);
        return Result.success(taskService.getVerifyTaskList(pageNum, pageSize, params));
    }

    @Operation(summary = "获取核实任务列表")
    @GetMapping("/check/list")
    public Result<Page<CheckTask>> getCheckTaskList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long collectorId) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("collectorId", collectorId);
        return Result.success(taskService.getCheckTaskList(pageNum, pageSize, params));
    }

    @Operation(summary = "执行核查任务")
    @PostMapping("/verify/execute")
    public Result<VerifyTask> executeVerifyTask(@RequestBody ExecuteRequest request) {
        return Result.success(taskService.executeVerifyTask(
            request.getTaskId(), request.getResult(), request.getRemark(), request.getAttachments()));
    }

    @Operation(summary = "执行核实任务")
    @PostMapping("/check/execute")
    public Result<CheckTask> executeCheckTask(@RequestBody ExecuteRequest request) {
        return Result.success(taskService.executeCheckTask(
            request.getTaskId(), request.getResult(), request.getRemark(), request.getAttachments()));
    }

    @Data
    public static class ExecuteRequest {
        private Long taskId;
        private Integer result;
        private String remark;
        private List<String> attachments;
    }
}
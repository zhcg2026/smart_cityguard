package com.cityguard.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.auth.entity.SysUser;
import com.cityguard.auth.entity.SysDepartment;
import com.cityguard.auth.entity.SysRole;
import com.cityguard.auth.service.SystemService;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "系统管理", description = "用户、角色、部门管理")
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    // ========== 用户管理 ==========

    @Operation(summary = "获取用户列表")
    @GetMapping("/user/list")
    public Result<Page<SysUser>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean unassignedOnly,
            @RequestParam(required = false) String roleCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("realName", realName);
        params.put("status", status);
        params.put("departmentId", departmentId);
        params.put("unassignedOnly", unassignedOnly);
        params.put("roleCode", roleCode);
        return Result.success(systemService.getUserList(pageNum, pageSize, params));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/user/{id}")
    public Result<SysUser> getUserDetail(@PathVariable Long id) {
        SysUser user = systemService.getUserDetail(id);
        return Result.success(user);
    }

    @Operation(summary = "获取用户角色")
    @GetMapping("/user/{id}/roles")
    public Result<List<SysRole>> getUserRoles(@PathVariable Long id) {
        return Result.success(systemService.getUserRoles(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping("/user")
    public Result<SysUser> createUser(@RequestBody UserRequest request) {
        return Result.success(systemService.createUser(request.getUser(), request.getRoleIds()));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/user")
    public Result<SysUser> updateUser(@RequestBody UserRequest request) {
        return Result.success(systemService.updateUser(request.getUser(), request.getRoleIds()));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/user/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        systemService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PostMapping("/user/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam(required = false) String password) {
        systemService.resetPassword(id, password);
        return Result.success();
    }

    // ========== 角色管理 ==========

    @Operation(summary = "获取角色列表")
    @GetMapping("/role/list")
    public Result<List<SysRole>> getRoleList() {
        return Result.success(systemService.getRoleList());
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/role/{id}")
    public Result<SysRole> getRoleDetail(@PathVariable Long id) {
        return Result.success(systemService.getRoleDetail(id));
    }

    @Operation(summary = "创建角色")
    @PostMapping("/role")
    public Result<SysRole> createRole(@RequestBody SysRole role) {
        return Result.success(systemService.createRole(role));
    }

    @Operation(summary = "更新角色")
    @PutMapping("/role")
    public Result<SysRole> updateRole(@RequestBody SysRole role) {
        return Result.success(systemService.updateRole(role));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/role/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        systemService.deleteRole(id);
        return Result.success();
    }

    // ========== 部门管理 ==========

    @Operation(summary = "获取部门树")
    @GetMapping("/dept/tree")
    public Result<List<SysDepartment>> getDeptTree() {
        return Result.success(systemService.getDeptTree());
    }

    @Operation(summary = "获取部门详情")
    @GetMapping("/dept/{id}")
    public Result<SysDepartment> getDeptDetail(@PathVariable Long id) {
        return Result.success(systemService.getDeptDetail(id));
    }

    @Operation(summary = "创建部门")
    @PostMapping("/dept")
    public Result<SysDepartment> createDept(@RequestBody SysDepartment dept) {
        return Result.success(systemService.createDept(dept));
    }

    @Operation(summary = "更新部门")
    @PutMapping("/dept")
    public Result<SysDepartment> updateDept(@RequestBody SysDepartment dept) {
        return Result.success(systemService.updateDept(dept));
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/dept/{id}")
    public Result<Void> deleteDept(@PathVariable Long id) {
        systemService.deleteDept(id);
        return Result.success();
    }

    @Operation(summary = "创建或补全部门登录账号")
    @PostMapping("/dept/{id}/ensure-login")
    public Result<SysDepartment> ensureDeptLogin(@PathVariable Long id) {
        return Result.success(systemService.ensureDeptLogin(id));
    }

    @Operation(summary = "重置部门登录账号密码")
    @PostMapping("/dept/{id}/reset-login-password")
    public Result<Void> resetDeptLoginPassword(
            @PathVariable Long id,
            @RequestParam(required = false) String password) {
        systemService.resetDeptLoginPassword(id, password);
        return Result.success();
    }

    @Data
    public static class UserRequest {
        private SysUser user;
        private List<Long> roleIds;
    }
}
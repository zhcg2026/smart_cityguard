package com.cityguard.auth.controller;

import com.cityguard.auth.entity.LoginUser;
import com.cityguard.auth.entity.SysUser;
import com.cityguard.auth.mapper.SysUserMapper;
import com.cityguard.auth.service.AuthService;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "登录、登出、获取用户信息")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class InitUserRequest {
        private String username;
        private String password;
        private String realName;
    }

    @Operation(summary = "初始化管理员用户")
    @PostMapping("/init")
    public Result<?> initAdmin(@RequestBody InitUserRequest request) {
        // 检查是否已存在用户
        SysUser existing = sysUserMapper.selectByUsername(request.getUsername());
        if (existing != null) {
            return Result.fail("用户已存在");
        }

        // 确保管理员角色存在
        sysUserMapper.ensureAdminRoleExists();

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone("13800138000");
        user.setEmail(request.getUsername() + "@cityguard.com");
        user.setDepartmentId(1L);
        user.setDepartmentName("运城市城管监督中心");
        user.setStatus(1);

        sysUserMapper.insert(user);

        // 自动分配管理员角色（角色ID=1）
        sysUserMapper.insertUserRole(user.getId(), 1L);

        return Result.success("用户创建成功，请登录");
    }

    @Operation(summary = "修复用户角色关联")
    @PostMapping("/fix-role")
    public Result<?> fixUserRole() {
        // 确保管理员角色存在
        sysUserMapper.ensureAdminRoleExists();

        // 为admin用户（ID=1）添加管理员角色
        sysUserMapper.insertUserRole(1L, 1L);

        return Result.success("角色关联已修复，请重新登录");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest request) {
        return authService.login(request.getUsername(), request.getPassword());
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<?> logout() {
        return authService.logout();
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public Result<LoginUser> getUserInfo() {
        return authService.getUserInfo();
    }
}
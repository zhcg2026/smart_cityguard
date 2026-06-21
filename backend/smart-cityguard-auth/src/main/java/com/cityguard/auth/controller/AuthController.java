package com.cityguard.auth.controller;

import com.cityguard.auth.entity.LoginUser;
import com.cityguard.auth.entity.SysUser;
import com.cityguard.auth.mapper.SysUserMapper;
import com.cityguard.auth.service.AuthService;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
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
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }

    @Data
    public static class UpdateAvatarRequest {
        private String avatar;
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

    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        SysUser user = authService.getCurrentUser();
        if (user == null) {
            throw new BusinessException("未登录");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);
        return Result.success();
    }

    @Operation(summary = "修改头像")
    @PutMapping("/avatar")
    public Result<Void> updateAvatar(@RequestBody UpdateAvatarRequest request) {
        SysUser user = authService.getCurrentUser();
        if (user == null) {
            throw new BusinessException("未登录");
        }
        user.setAvatar(request.getAvatar());
        sysUserMapper.updateById(user);
        return Result.success();
    }
}
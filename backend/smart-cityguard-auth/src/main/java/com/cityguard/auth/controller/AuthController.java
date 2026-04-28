package com.cityguard.auth.controller;

import com.cityguard.auth.entity.LoginUser;
import com.cityguard.auth.service.AuthService;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "登录、登出、获取用户信息")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
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
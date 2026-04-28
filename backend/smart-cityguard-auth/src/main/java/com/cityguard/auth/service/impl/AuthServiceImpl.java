package com.cityguard.auth.service.impl;

import com.cityguard.auth.entity.LoginUser;
import com.cityguard.auth.entity.SysUser;
import com.cityguard.auth.mapper.SysUserMapper;
import com.cityguard.auth.security.JwtUtils;
import com.cityguard.auth.service.AuthService;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Result<?> login(String username, String password) {
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        if (user.getStatus() != 1) {
            throw new BusinessException("用户已被禁用");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());

        return Result.success(data);
    }

    @Override
    public Result<?> logout() {
        SecurityContextHolder.clearContext();
        return Result.success("退出成功");
    }

    @Override
    public Result<LoginUser> getUserInfo() {
        SysUser user = getCurrentUser();
        if (user == null) {
            throw new BusinessException("用户未登录");
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setPhone(user.getPhone());
        loginUser.setEmail(user.getEmail());
        loginUser.setDepartmentId(user.getDepartmentId());
        loginUser.setDepartmentName(user.getDepartmentName());
        loginUser.setGridId(user.getGridId());
        loginUser.setGridName(user.getGridName());

        List<String> roles = sysUserMapper.selectRoleCodesByUserId(user.getId());
        loginUser.setRoles(roles);

        return Result.success(loginUser);
    }

    @Override
    public SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            return null;
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return sysUserMapper.selectById(loginUser.getId());
    }
}
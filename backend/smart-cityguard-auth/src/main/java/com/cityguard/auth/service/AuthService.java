package com.cityguard.auth.service;

import com.cityguard.auth.entity.LoginUser;
import com.cityguard.auth.entity.SysUser;
import com.cityguard.common.result.Result;

public interface AuthService {

    Result<?> login(String username, String password);

    Result<?> logout();

    Result<LoginUser> getUserInfo();

    SysUser getCurrentUser();
}
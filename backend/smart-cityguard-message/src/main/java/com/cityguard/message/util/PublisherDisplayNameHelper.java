package com.cityguard.message.util;

import com.cityguard.auth.entity.SysUser;

/**
 * 内容发布人展示名：优先真实姓名，与案件流程 operator 展示规则一致。
 */
public final class PublisherDisplayNameHelper {

    private PublisherDisplayNameHelper() {
    }

    public static String fromUser(SysUser user) {
        if (user == null) {
            return "系统";
        }
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName().trim();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername().trim();
        }
        return "系统";
    }
}

package com.cityguard.message.util;

import com.cityguard.auth.entity.LoginUser;

import java.util.List;

public final class ContentVisibilityHelper {

    private ContentVisibilityHelper() {
    }

    /**
     * 管理端监管账号查看已发布内容时不做受众过滤，与「内容发布」列表一致，避免仅面向采集员的内容在工作台不可见。
     */
    public static boolean canViewAllPublished(LoginUser user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().contains("ADMIN") || user.getRoles().contains("SUPERVISOR");
    }

    public static boolean isVisible(String receiverType, String receiverIds, LoginUser user) {
        if (canViewAllPublished(user)) {
            return true;
        }
        String type = receiverType == null ? "" : receiverType.trim();
        if (type.isEmpty() || "all".equalsIgnoreCase(type)) {
            return true;
        }
        if (user == null) {
            return false;
        }
        if ("user".equalsIgnoreCase(type)) {
            return user.getId() != null && containsToken(receiverIds, String.valueOf(user.getId()));
        }
        if ("role".equalsIgnoreCase(type)) {
            List<String> roles = user.getRoles();
            if (roles == null || roles.isEmpty()) {
                return false;
            }
            for (String role : roles) {
                if (containsToken(receiverIds, role)) {
                    return true;
                }
            }
            return false;
        }
        if ("collector".equalsIgnoreCase(type)) {
            return hasRole(user, "COLLECTOR");
        }
        if ("admin".equalsIgnoreCase(type)) {
            return hasRole(user, "ADMIN") || hasRole(user, "SUPERVISOR");
        }
        return false;
    }

    private static boolean hasRole(LoginUser user, String roleCode) {
        return user.getRoles() != null && user.getRoles().contains(roleCode);
    }

    private static boolean containsToken(String ids, String token) {
        if (ids == null || ids.isBlank() || token == null || token.isBlank()) {
            return false;
        }
        for (String part : ids.split(",")) {
            if (token.equals(part.trim())) {
                return true;
            }
        }
        return false;
    }
}

package com.cityguard.common.spi;

import java.util.Collection;

/**
 * 站内业务消息（案件/任务待办提醒），由 message 模块实现。
 */
public interface UserNotificationSender {

    void notifyUser(Long userId, String title, String content,
                    String bizType, Long bizId, String bizCode);

    default void notifyUsers(Collection<Long> userIds, String title, String content,
                             String bizType, Long bizId, String bizCode) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            if (userId != null) {
                notifyUser(userId, title, content, bizType, bizId, bizCode);
            }
        }
    }
}

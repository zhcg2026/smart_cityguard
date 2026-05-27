package com.cityguard.message.service;

import com.cityguard.common.spi.UserNotificationSender;
import com.cityguard.message.entity.UserMessage;
import com.cityguard.message.mapper.UserMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserNotificationSenderImpl implements UserNotificationSender {

    private final UserMessageMapper userMessageMapper;

    @Override
    public void notifyUser(Long userId, String title, String content,
                           String bizType, Long bizId, String bizCode) {
        if (userId == null || title == null || title.isBlank()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        UserMessage message = new UserMessage();
        message.setUserId(userId);
        message.setMsgRecordId(bizId != null ? bizId : 0L);
        message.setMsgType(bizType != null ? bizType : "system");
        message.setMsgTitle(title.trim());
        message.setMsgContent(content != null ? content : "");
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setBizCode(bizCode);
        message.setMsgStatus("unread");
        message.setMsgTime(now);
        message.setCreateTime(now);
        userMessageMapper.insert(message);
    }
}

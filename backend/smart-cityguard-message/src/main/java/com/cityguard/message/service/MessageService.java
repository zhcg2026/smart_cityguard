package com.cityguard.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.message.entity.UserMessage;
import com.cityguard.message.entity.Announcement;
import com.cityguard.message.entity.DailyTip;

import java.util.List;

public interface MessageService {

    List<UserMessage> getUserMessages(Long userId);

    List<UserMessage> getUnreadMessages(Long userId);

    int getUnreadCount(Long userId);

    void markAsRead(Long messageId);

    void markAllAsRead(Long userId);

    void sendMessage(Long userId, String title, String content, Integer messageType);

    List<Announcement> getAnnouncementList();

    Announcement getAnnouncementDetail(Long id);

    Announcement publishAnnouncement(Announcement announcement);

    List<DailyTip> getDailyTips(String date);

    List<DailyTip> getLatestTips(int limit);
}
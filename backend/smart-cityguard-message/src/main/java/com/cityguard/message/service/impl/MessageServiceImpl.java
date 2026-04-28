package com.cityguard.message.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.message.entity.UserMessage;
import com.cityguard.message.entity.Announcement;
import com.cityguard.message.entity.DailyTip;
import com.cityguard.message.mapper.UserMessageMapper;
import com.cityguard.message.mapper.AnnouncementMapper;
import com.cityguard.message.mapper.DailyTipMapper;
import com.cityguard.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final UserMessageMapper userMessageMapper;
    private final AnnouncementMapper announcementMapper;
    private final DailyTipMapper dailyTipMapper;

    @Override
    public List<UserMessage> getUserMessages(Long userId) {
        return userMessageMapper.selectByUserId(userId);
    }

    @Override
    public List<UserMessage> getUnreadMessages(Long userId) {
        return userMessageMapper.selectUnreadByUserId(userId);
    }

    @Override
    public int getUnreadCount(Long userId) {
        return userMessageMapper.countUnread(userId);
    }

    @Override
    public void markAsRead(Long messageId) {
        userMessageMapper.markAsRead(messageId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        userMessageMapper.markAllAsRead(userId);
    }

    @Override
    public void sendMessage(Long userId, String title, String content, Integer messageType) {
        UserMessage message = new UserMessage();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        userMessageMapper.insert(message);
    }

    @Override
    public List<Announcement> getAnnouncementList() {
        return announcementMapper.selectPublished();
    }

    @Override
    public Announcement getAnnouncementDetail(Long id) {
        return announcementMapper.selectById(id);
    }

    @Override
    @Transactional
    public Announcement publishAnnouncement(Announcement announcement) {
        announcement.setPublishStatus(1);
        announcement.setPublishTime(LocalDateTime.now());
        announcement.setCreateTime(LocalDateTime.now());
        announcementMapper.insert(announcement);
        return announcement;
    }

    @Override
    public List<DailyTip> getDailyTips(String date) {
        return dailyTipMapper.selectByDate(date);
    }

    @Override
    public List<DailyTip> getLatestTips(int limit) {
        return dailyTipMapper.selectLatest(limit);
    }
}
package com.cityguard.message.service;

import com.cityguard.auth.entity.LoginUser;
import com.cityguard.message.entity.Announcement;
import com.cityguard.message.entity.DailyTip;
import com.cityguard.message.entity.UserMessage;

import java.util.List;

public interface MessageService {

    List<UserMessage> getUserMessages(Long userId);

    List<UserMessage> getUnreadMessages(Long userId);

    int getUnreadCount(Long userId);

    void markAsRead(Long messageId, Long userId);

    void markAllAsRead(Long userId);

    List<Announcement> getVisibleAnnouncements(LoginUser user, Integer limit);

    List<Announcement> listAnnouncementsForAdmin(LoginUser operator);

    Announcement getAnnouncementDetail(Long id);

    Announcement saveAnnouncement(Announcement announcement, LoginUser operator, boolean publish);

    void deleteAnnouncement(Long id, LoginUser operator);

    List<DailyTip> getVisibleDailyTips(LoginUser user, Integer limit);

    List<DailyTip> listDailyTipsForAdmin(LoginUser operator);

    DailyTip getDailyTipDetail(Long id);

    DailyTip saveDailyTip(DailyTip dailyTip, LoginUser operator, boolean publish);

    void deleteDailyTip(Long id, LoginUser operator);
}

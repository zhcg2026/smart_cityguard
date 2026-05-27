package com.cityguard.message.service.impl;

import com.cityguard.auth.entity.LoginUser;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.message.entity.Announcement;
import com.cityguard.message.entity.DailyTip;
import com.cityguard.message.entity.UserMessage;
import com.cityguard.message.mapper.AnnouncementMapper;
import com.cityguard.message.mapper.DailyTipMapper;
import com.cityguard.message.mapper.UserMessageMapper;
import com.cityguard.message.service.MessageService;
import com.cityguard.message.util.ContentVisibilityHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final UserMessageMapper userMessageMapper;
    private final AnnouncementMapper announcementMapper;
    private final DailyTipMapper dailyTipMapper;

    @Override
    public List<UserMessage> getUserMessages(Long userId) {
        return userMessageMapper.selectRecentByUserId(userId, 50);
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
    public void markAsRead(Long messageId, Long userId) {
        userMessageMapper.markAsRead(messageId, userId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        userMessageMapper.markAllAsRead(userId);
    }

    @Override
    public List<Announcement> getVisibleAnnouncements(LoginUser user, Integer limit) {
        Stream<Announcement> stream = announcementMapper.selectPublished().stream()
                .filter(item -> ContentVisibilityHelper.isVisible(item.getReceiverType(), item.getReceiverIds(), user));
        if (limit != null && limit > 0) {
            stream = stream.limit(limit);
        }
        return stream.toList();
    }

    @Override
    public List<Announcement> listAnnouncementsForAdmin(LoginUser operator) {
        assertContentAdmin(operator);
        return announcementMapper.selectAllAdmin();
    }

    @Override
    public Announcement getAnnouncementDetail(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null || announcement.getIsDeleted() != null && announcement.getIsDeleted() == 1) {
            throw new BusinessException("通告不存在");
        }
        return announcement;
    }

    @Override
    @Transactional
    public Announcement saveAnnouncement(Announcement announcement, LoginUser operator, boolean publish) {
        assertContentAdmin(operator);
        validateContent(announcement.getTitle(), announcement.getContent());
        normalizeReceiver(announcement);

        LocalDateTime now = LocalDateTime.now();
        if (announcement.getId() == null) {
            if (announcement.getAnnouncementCode() == null || announcement.getAnnouncementCode().isBlank()) {
                announcement.setAnnouncementCode("ANN" + CODE_TIME.format(now));
            }
            announcement.setPublisherId(operator.getId());
            announcement.setPublisherName(operator.getRealName() != null ? operator.getRealName() : operator.getUsername());
            announcement.setPublisherDeptId(operator.getDepartmentId());
            announcement.setPublisherDeptName(operator.getDepartmentName());
            announcement.setPublishTime(now);
            announcement.setCreateTime(now);
            announcement.setUpdateTime(now);
            announcement.setStatus(publish ? "published" : "draft");
            if (announcement.getAnnouncementType() == null || announcement.getAnnouncementType().isBlank()) {
                announcement.setAnnouncementType("business");
            }
            if (announcement.getReadCount() == null) {
                announcement.setReadCount(0);
            }
            if (announcement.getTotalReceiverCount() == null) {
                announcement.setTotalReceiverCount(0);
            }
            if (announcement.getIsTop() == null) {
                announcement.setIsTop(0);
            }
            if (announcement.getTopOrder() == null) {
                announcement.setTopOrder(0);
            }
            announcementMapper.insert(announcement);
            return announcement;
        }

        Announcement existing = getAnnouncementDetail(announcement.getId());
        existing.setTitle(announcement.getTitle());
        existing.setContent(announcement.getContent());
        existing.setAnnouncementType(announcement.getAnnouncementType());
        existing.setDocNumber(announcement.getDocNumber());
        existing.setExpireTime(announcement.getExpireTime());
        existing.setReceiverType(announcement.getReceiverType());
        existing.setReceiverIds(announcement.getReceiverIds());
        existing.setIsTop(announcement.getIsTop());
        existing.setTopOrder(announcement.getTopOrder());
        existing.setUpdateTime(now);
        if (publish) {
            existing.setStatus("published");
            if (existing.getPublishTime() == null) {
                existing.setPublishTime(now);
            }
        } else if (!"published".equals(existing.getStatus())) {
            existing.setStatus("draft");
        }
        announcementMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long id, LoginUser operator) {
        assertContentAdmin(operator);
        Announcement existing = getAnnouncementDetail(id);
        announcementMapper.deleteById(existing.getId());
    }

    @Override
    public List<DailyTip> getVisibleDailyTips(LoginUser user, Integer limit) {
        Stream<DailyTip> stream = dailyTipMapper.selectPublishedActive().stream()
                .filter(item -> ContentVisibilityHelper.isVisible(item.getReceiverType(), item.getReceiverIds(), user));
        if (limit != null && limit > 0) {
            stream = stream.limit(limit);
        }
        return stream.toList();
    }

    @Override
    public List<DailyTip> listDailyTipsForAdmin(LoginUser operator) {
        assertContentAdmin(operator);
        return dailyTipMapper.selectAllAdmin();
    }

    @Override
    public DailyTip getDailyTipDetail(Long id) {
        DailyTip tip = dailyTipMapper.selectById(id);
        if (tip == null || tip.getIsDeleted() != null && tip.getIsDeleted() == 1) {
            throw new BusinessException("今日提示不存在");
        }
        return tip;
    }

    @Override
    @Transactional
    public DailyTip saveDailyTip(DailyTip dailyTip, LoginUser operator, boolean publish) {
        assertContentAdmin(operator);
        validateContent(dailyTip.getTitle(), dailyTip.getContent());
        normalizeReceiver(dailyTip);

        LocalDateTime now = LocalDateTime.now();
        if (dailyTip.getId() == null) {
            if (dailyTip.getTipCode() == null || dailyTip.getTipCode().isBlank()) {
                dailyTip.setTipCode("TIP" + CODE_TIME.format(now));
            }
            dailyTip.setPublisherId(operator.getId());
            dailyTip.setPublisherName(operator.getRealName() != null ? operator.getRealName() : operator.getUsername());
            dailyTip.setPublishTime(now);
            dailyTip.setCreateTime(now);
            dailyTip.setUpdateTime(now);
            dailyTip.setStatus(publish ? "published" : "draft");
            if (dailyTip.getReadCount() == null) {
                dailyTip.setReadCount(0);
            }
            dailyTipMapper.insert(dailyTip);
            return dailyTip;
        }

        DailyTip existing = getDailyTipDetail(dailyTip.getId());
        existing.setTitle(dailyTip.getTitle());
        existing.setContent(dailyTip.getContent());
        existing.setExpireTime(dailyTip.getExpireTime());
        existing.setReceiverType(dailyTip.getReceiverType());
        existing.setReceiverIds(dailyTip.getReceiverIds());
        existing.setUpdateTime(now);
        if (publish) {
            existing.setStatus("published");
            if (existing.getPublishTime() == null) {
                existing.setPublishTime(now);
            }
        } else if (!"published".equals(existing.getStatus())) {
            existing.setStatus("draft");
        }
        dailyTipMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void deleteDailyTip(Long id, LoginUser operator) {
        assertContentAdmin(operator);
        DailyTip existing = getDailyTipDetail(id);
        dailyTipMapper.deleteById(existing.getId());
    }

    private static void assertContentAdmin(LoginUser operator) {
        if (operator == null || operator.getRoles() == null) {
            throw new BusinessException("未登录");
        }
        if (!operator.getRoles().contains("ADMIN") && !operator.getRoles().contains("SUPERVISOR")) {
            throw new BusinessException("无权管理内容发布");
        }
    }

    private static void validateContent(String title, String content) {
        if (title == null || title.isBlank()) {
            throw new BusinessException("标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException("内容不能为空");
        }
    }

    private static void normalizeReceiver(Announcement announcement) {
        if (announcement.getReceiverType() == null || announcement.getReceiverType().isBlank()) {
            announcement.setReceiverType("all");
        }
        if ("all".equalsIgnoreCase(announcement.getReceiverType())) {
            announcement.setReceiverIds(null);
        } else if (announcement.getReceiverIds() == null || announcement.getReceiverIds().isBlank()) {
            throw new BusinessException("请指定可见用户或角色");
        }
    }

    private static void normalizeReceiver(DailyTip dailyTip) {
        if (dailyTip.getReceiverType() == null || dailyTip.getReceiverType().isBlank()) {
            dailyTip.setReceiverType("all");
        }
        if ("all".equalsIgnoreCase(dailyTip.getReceiverType())) {
            dailyTip.setReceiverIds(null);
        } else if (dailyTip.getReceiverIds() == null || dailyTip.getReceiverIds().isBlank()) {
            throw new BusinessException("请指定可见用户或角色");
        }
    }
}

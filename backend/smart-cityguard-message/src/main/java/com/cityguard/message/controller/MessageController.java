package com.cityguard.message.controller;

import com.cityguard.auth.entity.LoginUser;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.common.result.Result;
import com.cityguard.message.entity.Announcement;
import com.cityguard.message.entity.DailyTip;
import com.cityguard.message.entity.UserMessage;
import com.cityguard.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "消息通知", description = "用户消息、公文通告、每日提示")
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "获取当前用户消息列表")
    @GetMapping("/list")
    public Result<List<UserMessage>> getUserMessages() {
        return Result.success(messageService.getUserMessages(requireUserId()));
    }

    @Operation(summary = "获取当前用户未读消息")
    @GetMapping("/unread")
    public Result<List<UserMessage>> getUnreadMessages() {
        return Result.success(messageService.getUnreadMessages(requireUserId()));
    }

    @Operation(summary = "获取当前用户未读消息数量")
    @GetMapping("/unread/count")
    public Result<Integer> getUnreadCount() {
        return Result.success(messageService.getUnreadCount(requireUserId()));
    }

    @Operation(summary = "标记消息已读")
    @PostMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        messageService.markAsRead(id, requireUserId());
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read/all")
    public Result<Void> markAllAsRead() {
        messageService.markAllAsRead(requireUserId());
        return Result.success();
    }

    @Operation(summary = "获取当前用户可见的公文通告")
    @GetMapping("/announcement/list")
    public Result<List<Announcement>> getAnnouncementList(@RequestParam(required = false) Integer limit) {
        return Result.success(messageService.getVisibleAnnouncements(currentUser(), limit));
    }

    @Operation(summary = "管理端：通告列表")
    @GetMapping("/announcement/admin/list")
    public Result<List<Announcement>> listAnnouncementsForAdmin() {
        return Result.success(messageService.listAnnouncementsForAdmin(requireAdmin()));
    }

    @Operation(summary = "获取通告详情")
    @GetMapping("/announcement/{id}")
    public Result<Announcement> getAnnouncementDetail(@PathVariable Long id) {
        return Result.success(messageService.getAnnouncementDetail(id));
    }

    @Operation(summary = "发布/保存通告")
    @PostMapping("/announcement")
    public Result<Announcement> publishAnnouncement(@RequestBody PublishAnnouncementRequest request) {
        return Result.success(messageService.saveAnnouncement(
                request.getAnnouncement(), requireAdmin(), request.isPublish()));
    }

    @Operation(summary = "更新通告")
    @PutMapping("/announcement/{id}")
    public Result<Announcement> updateAnnouncement(@PathVariable Long id,
                                                   @RequestBody PublishAnnouncementRequest request) {
        request.getAnnouncement().setId(id);
        return Result.success(messageService.saveAnnouncement(
                request.getAnnouncement(), requireAdmin(), request.isPublish()));
    }

    @Operation(summary = "删除通告")
    @DeleteMapping("/announcement/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        messageService.deleteAnnouncement(id, requireAdmin());
        return Result.success();
    }

    @Operation(summary = "获取当前用户可见的今日提示")
    @GetMapping("/dailytip/latest")
    public Result<List<DailyTip>> getLatestTips(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(messageService.getVisibleDailyTips(currentUser(), limit));
    }

    @Operation(summary = "兼容旧路径：获取今日提示")
    @GetMapping("/dailytip/list")
    public Result<List<DailyTip>> getDailyTips(@RequestParam(required = false) Integer limit) {
        int size = limit != null && limit > 0 ? limit : 20;
        return Result.success(messageService.getVisibleDailyTips(currentUser(), size));
    }

    @Operation(summary = "管理端：今日提示列表")
    @GetMapping("/dailytip/admin/list")
    public Result<List<DailyTip>> listDailyTipsForAdmin() {
        return Result.success(messageService.listDailyTipsForAdmin(requireAdmin()));
    }

    @Operation(summary = "获取今日提示详情")
    @GetMapping("/dailytip/{id}")
    public Result<DailyTip> getDailyTipDetail(@PathVariable Long id) {
        return Result.success(messageService.getDailyTipDetail(id));
    }

    @Operation(summary = "发布/保存今日提示")
    @PostMapping("/dailytip")
    public Result<DailyTip> publishDailyTip(@RequestBody PublishDailyTipRequest request) {
        return Result.success(messageService.saveDailyTip(
                request.getDailyTip(), requireAdmin(), request.isPublish()));
    }

    @Operation(summary = "更新今日提示")
    @PutMapping("/dailytip/{id}")
    public Result<DailyTip> updateDailyTip(@PathVariable Long id,
                                           @RequestBody PublishDailyTipRequest request) {
        request.getDailyTip().setId(id);
        return Result.success(messageService.saveDailyTip(
                request.getDailyTip(), requireAdmin(), request.isPublish()));
    }

    @Operation(summary = "删除今日提示")
    @DeleteMapping("/dailytip/{id}")
    public Result<Void> deleteDailyTip(@PathVariable Long id) {
        messageService.deleteDailyTip(id, requireAdmin());
        return Result.success();
    }

    @Data
    public static class PublishAnnouncementRequest {
        private Announcement announcement;
        private boolean publish = true;
    }

    @Data
    public static class PublishDailyTipRequest {
        private DailyTip dailyTip;
        private boolean publish = true;
    }

    private static Long requireUserId() {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        return user.getId();
    }

    private static LoginUser requireAdmin() {
        LoginUser user = currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        if (user.getRoles() == null
                || (!user.getRoles().contains("ADMIN") && !user.getRoles().contains("SUPERVISOR"))) {
            throw new BusinessException("无权管理内容发布");
        }
        return user;
    }

    private static LoginUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }
}

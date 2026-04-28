package com.cityguard.message.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.message.entity.UserMessage;
import com.cityguard.message.entity.Announcement;
import com.cityguard.message.entity.DailyTip;
import com.cityguard.message.service.MessageService;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "消息通知", description = "用户消息、公文通告、每日提示")
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "获取用户消息列表")
    @GetMapping("/list")
    public Result<List<UserMessage>> getUserMessages(@RequestParam Long userId) {
        return Result.success(messageService.getUserMessages(userId));
    }

    @Operation(summary = "获取未读消息")
    @GetMapping("/unread")
    public Result<List<UserMessage>> getUnreadMessages(@RequestParam Long userId) {
        return Result.success(messageService.getUnreadMessages(userId));
    }

    @Operation(summary = "获取未读消息数量")
    @GetMapping("/unread/count")
    public Result<Integer> getUnreadCount(@RequestParam Long userId) {
        return Result.success(messageService.getUnreadCount(userId));
    }

    @Operation(summary = "标记消息已读")
    @PostMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        messageService.markAsRead(id);
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read/all")
    public Result<Void> markAllAsRead(@RequestParam Long userId) {
        messageService.markAllAsRead(userId);
        return Result.success();
    }

    @Operation(summary = "获取公文通告列表")
    @GetMapping("/announcement/list")
    public Result<List<Announcement>> getAnnouncementList() {
        return Result.success(messageService.getAnnouncementList());
    }

    @Operation(summary = "获取通告详情")
    @GetMapping("/announcement/{id}")
    public Result<Announcement> getAnnouncementDetail(@PathVariable Long id) {
        return Result.success(messageService.getAnnouncementDetail(id));
    }

    @Operation(summary = "发布通告")
    @PostMapping("/announcement")
    public Result<Announcement> publishAnnouncement(@RequestBody Announcement announcement) {
        return Result.success(messageService.publishAnnouncement(announcement));
    }

    @Operation(summary = "获取每日提示")
    @GetMapping("/dailytip/list")
    public Result<List<DailyTip>> getDailyTips(@RequestParam(required = false) String date) {
        if (date == null) {
            date = java.time.LocalDate.now().toString();
        }
        return Result.success(messageService.getDailyTips(date));
    }

    @Operation(summary = "获取最新提示")
    @GetMapping("/dailytip/latest")
    public Result<List<DailyTip>> getLatestTips(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(messageService.getLatestTips(limit));
    }
}
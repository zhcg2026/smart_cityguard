package com.cityguard.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_message")
public class UserMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private Integer messageType;

    private Integer isRead;

    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
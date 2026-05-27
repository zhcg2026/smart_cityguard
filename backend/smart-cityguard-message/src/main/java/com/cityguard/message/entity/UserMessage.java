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

    private Long msgRecordId;

    private String msgCode;

    private String msgType;

    private String msgTitle;

    private String msgContent;

    private String bizType;

    private Long bizId;

    private String bizCode;

    private String msgStatus;

    private LocalDateTime readTime;

    private LocalDateTime msgTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

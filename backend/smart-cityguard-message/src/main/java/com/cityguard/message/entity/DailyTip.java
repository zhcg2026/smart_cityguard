package com.cityguard.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("daily_tip")
public class DailyTip {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tipCode;

    private String title;

    private String content;

    private Long publisherId;

    private String publisherName;

    private LocalDateTime publishTime;

    private LocalDateTime expireTime;

    private String receiverType;

    private String receiverIds;

    private String status;

    private Integer readCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
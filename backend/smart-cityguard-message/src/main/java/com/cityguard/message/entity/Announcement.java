package com.cityguard.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String announcementCode;

    private String title;

    private String content;

    private String announcementType;

    private String docNumber;

    private Long publisherId;

    private String publisherName;

    private Long publisherDeptId;

    private String publisherDeptName;

    private LocalDateTime publishTime;

    private LocalDateTime expireTime;

    private String receiverType;

    private String receiverIds;

    private String status;

    private Integer readCount;

    private Integer totalReceiverCount;

    private String attachments;

    private Integer isTop;

    private Integer topOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
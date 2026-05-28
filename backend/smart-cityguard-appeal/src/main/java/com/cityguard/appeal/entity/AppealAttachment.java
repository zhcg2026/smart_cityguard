package com.cityguard.appeal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appeal_attachment")
public class AppealAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appealId;
    private Long reviewId;

    private String fileType;
    private String fileName;
    private String filePath;
    private Integer fileSize;
    private String fileExt;

    private String useType;

    private Long uploaderId;
    private String uploaderName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}

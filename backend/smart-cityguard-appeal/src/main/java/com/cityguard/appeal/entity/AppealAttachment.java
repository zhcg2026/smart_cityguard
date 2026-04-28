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

    private String fileUrl;

    private String fileName;

    private String fileType;

    private LocalDateTime uploadTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
package com.cityguard.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("check_attachment")
public class CheckAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long checkTaskId;

    private String fileType;

    private String fileName;

    private String filePath;

    private Integer fileSize;

    private String photoType;

    private Double longitude;

    private Double latitude;

    private LocalDateTime shootTime;

    private Long uploaderId;

    private String uploaderName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}

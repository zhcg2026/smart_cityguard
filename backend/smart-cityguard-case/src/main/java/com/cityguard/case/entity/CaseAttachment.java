package com.cityguard.case.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("case_attachment")
public class CaseAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long caseId;

    private Integer attachmentType;

    private String fileType;

    private String fileUrl;

    private String fileName;

    private Long fileSize;

    private Integer flowType;

    private LocalDateTime uploadTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
package com.cityguard.caseinfo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("case_attachment")
public class CaseAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long caseId;
    private Long flowRecordId;

    // 附件信息
    private String fileType;
    private String fileName;
    private String filePath;
    private Integer fileSize;
    private String fileExt;

    // 图片特有字段
    private String photoType;
    private Double longitude;
    private Double latitude;
    private LocalDateTime shootTime;

    // 上传人
    private Long uploaderId;
    private String uploaderName;

    // 关联节点
    private String nodeCode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
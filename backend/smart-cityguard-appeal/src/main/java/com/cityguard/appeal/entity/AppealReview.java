package com.cityguard.appeal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appeal_review")
public class AppealReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appealId;
    private String appealCode;

    private String reviewNode;
    private String reviewNodeName;

    private String reviewResult;
    private String reviewOpinion;

    private Long reviewerId;
    private String reviewerName;
    private Long reviewerDeptId;
    private String reviewerDeptName;
    private String reviewerPosition;

    private LocalDateTime reviewTime;
    private Integer timeUsed;

    private String attachments;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

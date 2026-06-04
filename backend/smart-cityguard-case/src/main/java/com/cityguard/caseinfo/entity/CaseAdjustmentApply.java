package com.cityguard.caseinfo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("case_adjustment_apply")
public class CaseAdjustmentApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long caseId;
    private String caseCode;
    private String applyType;
    private String applyStatus;
    private String reason;
    private LocalDateTime suspendUntil;
    private LocalDateTime oldDeadlineTime;
    private LocalDateTime newDeadlineTime;
    private Long applicantId;
    private String applicantName;
    private Long applicantDeptId;
    private Long deptReviewerId;
    private String deptReviewerName;
    private String deptReviewRemark;
    private LocalDateTime deptReviewTime;
    private Long reviewerId;
    private String reviewerName;
    private String reviewRemark;
    private LocalDateTime reviewTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private String applyTypeLabel;

    @TableField(exist = false)
    private String applyStatusLabel;

    @TableField(exist = false)
    private String handleDeptName;
}

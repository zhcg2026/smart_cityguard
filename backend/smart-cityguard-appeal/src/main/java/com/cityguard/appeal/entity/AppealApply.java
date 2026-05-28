package com.cityguard.appeal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("appeal_apply")
public class AppealApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appealCode;

    private Long caseId;
    private String caseCode;

    private String applyType;
    private String appealType;
    private String appealDesc;
    private String declareType;
    private String declareDesc;

    private String deductionType;
    private BigDecimal deductionValue;
    private LocalDateTime deductionTime;

    private Long applyDeptId;
    private String applyDeptName;
    private Long applyUserId;
    private String applyUserName;
    private String applyUserPhone;

    private LocalDateTime applyTime;
    private LocalDateTime deadlineTime;

    private String appealStatus;
    private String finalResult;
    private String finalOpinion;
    private BigDecimal adjustValue;

    private String attachments;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}

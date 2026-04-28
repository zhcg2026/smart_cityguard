package com.cityguard.appeal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("appeal_apply")
public class AppealApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appealType;

    private Long caseId;

    private String caseNo;

    private Long applicantId;

    private String applicantName;

    private String reason;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime reviewTime;

    private Long reviewerId;

    private String reviewerName;

    private String reviewResult;

    private String reviewRemark;

    @TableLogic
    private Integer isDeleted;
}
package com.cityguard.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 核实任务（结案前可选分支，verify_task 表）
 */
@Data
@TableName("verify_task")
public class VerifyTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskCode;

    private Long caseId;

    private String caseCode;

    private String sourceType;

    private String sourceDesc;

    private String bigCode;

    private String bigName;

    private String smallCode;

    private String smallName;

    private String description;

    private Double longitude;

    private Double latitude;

    private String address;

    private Long respGridId;

    private String taskStatus;

    private LocalDateTime assignTime;

    private LocalDateTime finishTime;

    private LocalDateTime deadlineTime;

    private Long collectorId;

    private String collectorName;

    private String collectorPhone;

    private String verifyResult;

    private String verifyOpinion;

    private Long creatorId;

    private String creatorName;

    /** 受理员下发核实时的要求/备注 */
    private String assignRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private String handleDeptName;

    @TableField(exist = false)
    private String timeRemaining;

    @TableField(exist = false)
    private Boolean timedOut;

    @JsonProperty("taskNo")
    public String getTaskNo() {
        return taskCode;
    }

    @JsonProperty("caseNo")
    public String getCaseNo() {
        return caseCode;
    }

    @JsonProperty("categoryBigName")
    public String getCategoryBigName() {
        return bigName;
    }

    @JsonProperty("categorySmallName")
    public String getCategorySmallName() {
        return smallName;
    }
}

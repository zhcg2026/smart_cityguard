package com.cityguard.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 核查任务（立案前可选分支，check_task 表）
 */
@Data
@TableName("check_task")
public class CheckTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskCode;

    private Long caseId;

    private String caseCode;

    private String smallName;

    private String address;

    private Double longitude;

    private Double latitude;

    private Long handleDeptId;

    private String handleDeptName;

    private String handleOpinion;

    private LocalDateTime handleFinishTime;

    private String taskStatus;

    private LocalDateTime assignTime;

    private LocalDateTime finishTime;

    private LocalDateTime deadlineTime;

    private Long collectorId;

    private String collectorName;

    private String collectorPhone;

    private String checkResult;

    private String checkOpinion;

    private Long assignerId;

    private String assignerName;

    /** 受理员下发核查时的要求/备注 */
    private String assignRemark;

    private Integer reworkCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private String description;

    @TableField(exist = false)
    private String bigName;

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

    @JsonProperty("categorySmallName")
    public String getCategorySmallName() {
        return smallName;
    }
}

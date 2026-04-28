package com.cityguard.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("verify_task")
public class VerifyTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;

    private Long caseId;

    private String caseNo;

    private Long collectorId;

    private String collectorName;

    private String address;

    private Double longitude;

    private Double latitude;

    private String categorySmallName;

    private Integer status;

    private Integer result;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime executeTime;

    private LocalDateTime deadline;

    private Integer isOverdue;

    @TableLogic
    private Integer isDeleted;
}
package com.cityguard.case.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("case_info")
public class CaseInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String caseNo;

    private Integer caseSource;

    private Long categoryBigId;

    private String categoryBigName;

    private Long categorySmallId;

    private String categorySmallName;

    private Long conditionId;

    private String conditionName;

    private String address;

    private Double longitude;

    private Double latitude;

    private String description;

    private Integer status;

    private Long collectorId;

    private String collectorName;

    private Long gridId;

    private String gridName;

    private Long departmentId;

    private String departmentName;

    private Long handlerId;

    private String handlerName;

    private LocalDateTime reportTime;

    private LocalDateTime registerTime;

    private LocalDateTime dispatchTime;

    private LocalDateTime handleTime;

    private LocalDateTime checkTime;

    private LocalDateTime closeTime;

    private LocalDateTime deadline;

    private Integer isOverdue;

    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
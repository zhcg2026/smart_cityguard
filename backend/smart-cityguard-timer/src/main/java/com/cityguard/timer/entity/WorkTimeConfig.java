package com.cityguard.timer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("work_time_config")
public class WorkTimeConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configName;

    private String amStartTime;

    private String amEndTime;

    private String pmStartTime;

    private String pmEndTime;

    private Integer isDefault;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}

package com.cityguard.config.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 与 {@code database/init.sql} 表 {@code time_limit_rule} 一致（全局时限类型规则）。
 */
@Data
@TableName("time_limit_rule")
public class TimeLimitRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String timeLimitType;

    private String typeName;

    private Integer isContinuous;

    private Integer includeHoliday;

    private Integer includeWeekend;

    private Integer useWorkTimeConfig;

    private String calcDesc;

    private Integer sortOrder;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}

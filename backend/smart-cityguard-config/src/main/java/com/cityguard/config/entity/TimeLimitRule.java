package com.cityguard.config.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("time_limit_rule")
public class TimeLimitRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categorySmallId;

    private Integer timeLimitType;

    private Integer handleHours;

    private Integer checkHours;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Integer status;
}
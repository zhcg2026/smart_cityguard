package com.cityguard.config.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小类处置时限覆盖（已停用；处置时限以 case_standard 立案条件为准，表保留兼容）。
 */
@Data
@TableName("category_time_limit_override")
public class CategoryTimeLimitOverride {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long smallId;

    private String timeLimitType;

    private Integer timeLimitValue;

    private String remark;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}

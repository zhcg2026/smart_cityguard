package com.cityguard.config.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 与 {@code database/init.sql} 表 {@code case_standard} 一致；{@code condition_desc} 对外 JSON 使用 {@code conditionContent} 与采集端一致。
 */
@Data
@TableName("case_standard")
public class CaseStandard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String standardCode;

    @TableField("small_id")
    private Long smallId;

    private String bigCode;

    private String smallCode;

    private String categoryType;

    /** 库列 condition_desc；JSON 对外字段名 conditionContent 与采集端一致 */
    @TableField("condition_desc")
    @JsonProperty("conditionContent")
    private String conditionDesc;

    private String handleTimeLimit;

    private Integer handleTimeValue;

    private String handleTimeType;

    private String closeCondition;

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

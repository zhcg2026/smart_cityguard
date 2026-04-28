package com.cityguard.config.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("case_standard")
public class CaseStandard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categorySmallId;

    private String conditionContent;

    private Integer sort;

    private String handleRequirement;

    private String checkRequirement;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Integer status;
}
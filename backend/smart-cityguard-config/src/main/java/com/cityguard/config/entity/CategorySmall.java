package com.cityguard.config.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("category_small")
public class CategorySmall {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryBigId;

    private String name;

    private String code;

    private Integer sort;

    private String description;

    private Integer handleDays;

    private Integer checkDays;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Integer status;
}
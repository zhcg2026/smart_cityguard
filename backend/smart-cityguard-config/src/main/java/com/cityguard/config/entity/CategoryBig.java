package com.cityguard.config.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("category_big")
public class CategoryBig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bigCode;

    private String bigName;

    private String categoryType;

    private Integer sortOrder;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Integer status;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
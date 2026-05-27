package com.cityguard.config.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("category_small")
public class CategorySmall {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bigId;

    private String smallCode;

    private String smallName;

    private String bigCode;

    /** 与库字段 category_type 一致：component / event */
    private String categoryType;

    /** 完整编码，库字段 full_code */
    private String fullCode;

    /** 监管主体（模板「主管部门」） */
    private String superviseSubject;

    /** 责任主体（模板「处置单位」） */
    private String responsibilitySubject;

    private String legalBasis;

    private String collectRequirement;

    private Integer isExtended;

    private Integer sortOrder;

    private String description;

    /** 库表无此列，保留给旧代码/前端扩展用 */
    @TableField(exist = false)
    private Integer handleDays;

    /** 库表无此列 */
    @TableField(exist = false)
    private Integer checkDays;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Integer status;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}

package com.cityguard.config.dto;

import lombok.Data;

@Data
public class CategoryBigSaveRequest {

    private Long id;

    /** component / event */
    private String categoryType;

    private String bigCode;

    private String bigName;

    private String description;

    private Integer sortOrder;

    /** 1 启用 0 停用 */
    private Integer status;
}

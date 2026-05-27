package com.cityguard.config.dto;

import lombok.Data;

/**
 * 小类时限一览：标准默认值 + 可选覆盖 + 实际生效值。
 */
@Data
public class SmallTimeLimitRowVO {

    private Long smallId;

    private String smallCode;

    private String smallName;

    private Long bigId;

    private String bigName;

    private String categoryType;

    /** 立案标准中的默认时限类型 */
    private String defaultTimeLimitType;

    private String defaultTimeLimitTypeName;

    private Integer defaultTimeLimitValue;

    private Long overrideId;

    private String overrideTimeLimitType;

    private String overrideTimeLimitTypeName;

    private Integer overrideTimeLimitValue;

    private String overrideRemark;

    /** 实际生效（有覆盖用覆盖，否则用标准默认，再否则系统默认 4 工作时） */
    private String effectiveTimeLimitType;

    private String effectiveTimeLimitTypeName;

    private Integer effectiveTimeLimitValue;

    private boolean overridden;
}

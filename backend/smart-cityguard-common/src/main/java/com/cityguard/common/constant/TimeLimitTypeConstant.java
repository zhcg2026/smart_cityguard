package com.cityguard.common.constant;

/**
 * 时限类型常量
 */
public class TimeLimitTypeConstant {
    public static final String URGENT_HOUR = "urgent_hour";     // 紧急工作时（自然时间连续）
    public static final String WORK_HOUR = "work_hour";         // 工作时（仅工作时间）
    public static final String WORK_DAY = "work_day";           // 工作日（不含周末）
    public static final String NATURAL_DAY = "natural_day";     // 自然日（连续计算）
}
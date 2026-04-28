package com.cityguard.common.enums;

import lombok.Getter;

@Getter
public enum TimeLimitTypeEnum {
    URGENT_HOUR("urgent_hour", "紧急工作时", true, true, true),
    WORK_HOUR("work_hour", "工作时", false, false, false),
    WORK_DAY("work_day", "工作日", false, false, false),
    NATURAL_DAY("natural_day", "自然日", true, true, true);

    private final String code;
    private final String name;
    private final boolean continuous;      // 是否连续计算
    private final boolean includeHoliday;  // 是否包含节假日
    private final boolean includeWeekend;  // 是否包含周末

    TimeLimitTypeEnum(String code, String name, boolean continuous, boolean includeHoliday, boolean includeWeekend) {
        this.code = code;
        this.name = name;
        this.continuous = continuous;
        this.includeHoliday = includeHoliday;
        this.includeWeekend = includeWeekend;
    }

    public static TimeLimitTypeEnum fromCode(String code) {
        for (TimeLimitTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
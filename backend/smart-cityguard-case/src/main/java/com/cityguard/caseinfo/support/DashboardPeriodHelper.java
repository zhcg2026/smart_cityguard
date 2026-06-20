package com.cityguard.caseinfo.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cityguard.caseinfo.entity.CaseInfo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

/**
 * 工作台统计周期：日 / 周（周一至今日）/ 月 / 年
 */
public final class DashboardPeriodHelper {

    public record Range(LocalDateTime start, LocalDateTime end) {
    }

    private DashboardPeriodHelper() {
    }

    public static Range resolve(String period) {
        if (period == null || period.isBlank()) {
            return null;
        }
        LocalDate today = LocalDate.now();
        LocalDate startDate = switch (period.trim().toLowerCase(Locale.ROOT)) {
            case "day" -> today;
            case "week" -> today.with(DayOfWeek.MONDAY);
            case "month" -> today.withDayOfMonth(1);
            case "year" -> today.withDayOfYear(1);
            default -> null;
        };
        if (startDate == null) {
            return null;
        }
        return new Range(startDate.atStartOfDay(), today.atTime(LocalTime.of(23, 59, 59)));
    }

    /** 已完成按结案时间，待处理不限时间（只要没完成就一直显示），其余按上报时间 */
    public static void applyPeriodFilter(LambdaQueryWrapper<CaseInfo> wrapper, String statGroup, Range range) {
        if (range == null) {
            return;
        }
        if ("pending".equals(statGroup)) {
            return;
        }
        if ("completed".equals(statGroup)) {
            wrapper.ge(CaseInfo::getCloseTime, range.start());
            wrapper.le(CaseInfo::getCloseTime, range.end());
        } else {
            wrapper.ge(CaseInfo::getReportTime, range.start());
            wrapper.le(CaseInfo::getReportTime, range.end());
        }
    }
}

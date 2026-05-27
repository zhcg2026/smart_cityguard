package com.cityguard.timer.service;

import com.cityguard.common.enums.TimeLimitTypeEnum;
import com.cityguard.timer.model.TimerContext;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 案件时限计算：含「紧急」连续计时；不含「紧急」仅计工作时段（默认 8–12、14–18，1天=8工作小时）。
 */
public final class DeadlineCalculator {

    private static final int MAX_STEPS = 500_000;

    private DeadlineCalculator() {
    }

    public static boolean isContinuous(String timeLimitType) {
        TimeLimitTypeEnum e = TimeLimitTypeEnum.fromCode(timeLimitType);
        if (e != null) {
            return e.isContinuous();
        }
        return "urgent_hour".equals(timeLimitType) || "natural_day".equals(timeLimitType)
                || "urgent_minute".equals(timeLimitType);
    }

    public static LocalDateTime calculateDeadline(LocalDateTime start, String timeLimitType, int value,
                                                   TimerContext ctx) {
        if (start == null || value <= 0) {
            return start;
        }
        return switch (timeLimitType) {
            case "urgent_minute" -> start.plusMinutes(value);
            case "urgent_hour" -> start.plusHours(value);
            case "natural_day" -> start.plusDays(value);
            case "work_hour" -> addWorkingSeconds(start, value * 3600L, ctx);
            case "work_day" -> addWorkingSeconds(start, value * 8L * 3600L, ctx);
            default -> start.plusHours(value);
        };
    }

    public static int calculateTotalSeconds(String timeLimitType, int value) {
        return switch (timeLimitType) {
            case "urgent_minute" -> value * 60;
            case "urgent_hour" -> value * 3600;
            case "natural_day" -> (int) Duration.ofDays(value).getSeconds();
            case "work_hour" -> value * 3600;
            case "work_day" -> value * 8 * 3600;
            default -> value * 3600;
        };
    }

    public static int calculateUsedSeconds(LocalDateTime start, LocalDateTime end, String timeLimitType,
                                           TimerContext ctx, int totalPausedSeconds) {
        if (start == null || end == null || !end.isAfter(start)) {
            return 0;
        }
        int paused = Math.max(0, totalPausedSeconds);
        if (isContinuous(timeLimitType)) {
            return (int) Math.max(0, Duration.between(start, end).getSeconds() - paused);
        }
        return (int) Math.max(0, countWorkingSeconds(start, end, ctx) - paused);
    }

    public static LocalDateTime addWorkingSeconds(LocalDateTime start, long secondsNeeded, TimerContext ctx) {
        if (secondsNeeded <= 0) {
            return start;
        }
        LocalDateTime current = start;
        long remaining = secondsNeeded;
        int steps = 0;
        while (remaining > 0 && steps++ < MAX_STEPS) {
            current = alignToWorkTime(current, ctx);
            LocalDateTime slotEnd = endOfCurrentWorkSlot(current, ctx);
            long available = Duration.between(current, slotEnd).getSeconds();
            if (available <= 0) {
                current = current.plusMinutes(1);
                continue;
            }
            if (available >= remaining) {
                return current.plusSeconds(remaining);
            }
            remaining -= available;
            current = slotEnd;
        }
        return current;
    }

    private static long countWorkingSeconds(LocalDateTime start, LocalDateTime end, TimerContext ctx) {
        LocalDateTime current = start;
        long total = 0;
        int steps = 0;
        while (current.isBefore(end) && steps++ < MAX_STEPS) {
            current = alignToWorkTime(current, ctx);
            if (!current.isBefore(end) && current.equals(end)) {
                break;
            }
            if (!current.isBefore(end)) {
                break;
            }
            LocalDateTime slotEnd = endOfCurrentWorkSlot(current, ctx);
            LocalDateTime segmentEnd = slotEnd.isBefore(end) ? slotEnd : end;
            if (segmentEnd.isAfter(current)) {
                total += Duration.between(current, segmentEnd).getSeconds();
            }
            current = segmentEnd;
            if (current.equals(end)) {
                break;
            }
        }
        return total;
    }

    private static LocalDateTime alignToWorkTime(LocalDateTime time, TimerContext ctx) {
        LocalDateTime current = time;
        int steps = 0;
        while (steps++ < MAX_STEPS) {
            if (!isWorkDay(current.toLocalDate(), ctx)) {
                current = current.toLocalDate().plusDays(1).atTime(ctx.getAmStart());
                continue;
            }
            LocalTime t = current.toLocalTime();
            if (t.isBefore(ctx.getAmStart())) {
                return current.toLocalDate().atTime(ctx.getAmStart());
            }
            if (t.isBefore(ctx.getAmEnd())) {
                return current;
            }
            if (t.isBefore(ctx.getPmStart())) {
                return current.toLocalDate().atTime(ctx.getPmStart());
            }
            if (t.isBefore(ctx.getPmEnd())) {
                return current;
            }
            current = current.toLocalDate().plusDays(1).atTime(ctx.getAmStart());
        }
        return current;
    }

    private static LocalDateTime endOfCurrentWorkSlot(LocalDateTime time, TimerContext ctx) {
        LocalTime t = time.toLocalTime();
        if (!t.isBefore(ctx.getAmStart()) && t.isBefore(ctx.getAmEnd())) {
            return time.toLocalDate().atTime(ctx.getAmEnd());
        }
        if (!t.isBefore(ctx.getPmStart()) && t.isBefore(ctx.getPmEnd())) {
            return time.toLocalDate().atTime(ctx.getPmEnd());
        }
        return alignToWorkTime(time, ctx);
    }

    private static boolean isWorkDay(LocalDate date, TimerContext ctx) {
        String key = date.toString();
        String type = ctx.getHolidayByDate().get(key);
        if ("holiday".equals(type)) {
            return false;
        }
        if ("workday".equals(type)) {
            return true;
        }
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }
}

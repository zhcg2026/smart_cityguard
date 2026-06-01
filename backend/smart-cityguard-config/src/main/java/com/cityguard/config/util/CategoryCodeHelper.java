package com.cityguard.config.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 案件分类编码工具（与 muban 导入规则一致）
 */
public final class CategoryCodeHelper {

    private static final Pattern PAT_URGENT_WORK_DAY = Pattern.compile("(\\d+)\\s*紧急工作日");
    private static final Pattern PAT_URGENT_WORK_HOUR = Pattern.compile("(\\d+)\\s*紧急工作时");
    private static final Pattern PAT_WORK_DAY = Pattern.compile("(\\d+)\\s*工作日");
    private static final Pattern PAT_DAY = Pattern.compile("(\\d+)\\s*天");
    private static final Pattern PAT_HOUR = Pattern.compile("(\\d+)\\s*小时");

    public record ParsedHandleTime(int value, String type) {
    }

    public static String buildFullCode(String bigCode, String smallCode) {
        String b = padCodePart(bigCode);
        String s = padCodePart(smallCode);
        String fc = b + s;
        if (fc.length() <= 4) {
            return fc;
        }
        return fc.substring(0, 4);
    }

    public static String buildStandardCode(Long smallId, int seq) {
        return "S" + smallId + "_" + seq;
    }

    public static String normalizeCode(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        String s = raw.trim();
        if (s.length() > 20) {
            throw new IllegalArgumentException(label + "过长（最多20字符）");
        }
        return s;
    }

    public static String formatHandleTimeLimit(String handleTimeType, Integer handleTimeValue) {
        if (handleTimeValue == null || handleTimeValue <= 0) {
            return "1自然日";
        }
        return switch (handleTimeType != null ? handleTimeType : "") {
            case "urgent_hour" -> handleTimeValue + "紧急工作时";
            case "work_hour" -> handleTimeValue + "工作时";
            case "work_day" -> handleTimeValue + "工作日";
            case "natural_day" -> handleTimeValue + "自然日";
            default -> handleTimeValue + "自然日";
        };
    }

    /** 从处置时限文案解析类型与数值（与 muban 导入、计时引擎规则一致） */
    public static ParsedHandleTime parseHandleTimeLimitText(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        Matcher m;
        m = PAT_URGENT_WORK_DAY.matcher(s);
        if (m.find()) {
            return new ParsedHandleTime(Integer.parseInt(m.group(1)), "natural_day");
        }
        m = PAT_URGENT_WORK_HOUR.matcher(s);
        if (m.find()) {
            return new ParsedHandleTime(Integer.parseInt(m.group(1)), "urgent_hour");
        }
        m = PAT_WORK_DAY.matcher(s);
        if (m.find()) {
            return new ParsedHandleTime(Integer.parseInt(m.group(1)), "work_day");
        }
        m = PAT_DAY.matcher(s);
        if (m.find()) {
            return new ParsedHandleTime(Integer.parseInt(m.group(1)), "work_day");
        }
        m = PAT_HOUR.matcher(s);
        if (m.find()) {
            return new ParsedHandleTime(Integer.parseInt(m.group(1)), "work_hour");
        }
        return null;
    }

    /** 文案与 type 不一致时，以文案为准（兼容旧库数据） */
    public static ParsedHandleTime resolveHandleTime(String handleTimeType, Integer handleTimeValue,
                                                     String handleTimeLimit) {
        ParsedHandleTime parsed = parseHandleTimeLimitText(handleTimeLimit);
        if (parsed != null) {
            return parsed;
        }
        if (handleTimeValue != null && handleTimeValue > 0 && handleTimeType != null && !handleTimeType.isBlank()) {
            return new ParsedHandleTime(handleTimeValue, handleTimeType);
        }
        return null;
    }

    public static String formatHandleTimeLimitLabel(String handleTimeType, Integer handleTimeValue) {
        return formatHandleTimeLimit(handleTimeType, handleTimeValue);
    }

    private CategoryCodeHelper() {
    }

    private static String padCodePart(String code) {
        if (code == null || code.isEmpty()) {
            return "00";
        }
        if (code.matches("\\d+")) {
            int n = Integer.parseInt(code);
            int capped = Math.min(Math.max(n, 0), 99);
            return String.format(Locale.ROOT, "%02d", capped);
        }
        if (code.length() >= 2) {
            return code.substring(0, 2);
        }
        return "0" + code;
    }
}

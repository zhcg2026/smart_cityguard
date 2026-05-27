package com.cityguard.config.util;

import java.util.Locale;

/**
 * 案件分类编码工具（与 muban 导入规则一致）
 */
public final class CategoryCodeHelper {

    private CategoryCodeHelper() {
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

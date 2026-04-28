package com.cityguard.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 地理距离计算工具
 */
public class GeoUtils {

    private static final BigDecimal EARTH_RADIUS = new BigDecimal("6371000"); // 地球半径（米）

    /**
     * 计算两点之间的距离（米）
     * 使用Haversine公式
     */
    public static BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double deltaLat = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double deltaLon = Math.toRadians(lon2.subtract(lon1).doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS.multiply(new BigDecimal(c)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 判断两点是否在指定距离范围内
     */
    public static boolean isWithinDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2, int meters) {
        BigDecimal distance = calculateDistance(lat1, lon1, lat2, lon2);
        return distance.compareTo(new BigDecimal(meters)) <= 0;
    }

    /**
     * 计算GeoHash编码（简化版）
     */
    public static String geoHash(BigDecimal lat, BigDecimal lon, int precision) {
        // 简化实现，实际应使用专业GeoHash库
        return String.format("%.6f,%.6f", lat, lon);
    }
}
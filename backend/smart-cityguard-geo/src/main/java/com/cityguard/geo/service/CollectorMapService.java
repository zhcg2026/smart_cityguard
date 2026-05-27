package com.cityguard.geo.service;

import com.cityguard.geo.dto.CollectorMapOverviewDto;

public interface CollectorMapService {

    /**
     * 采集员地图总览：采集员列表、责任片区、近期有坐标的案件点位
     *
     * @param caseDays 案件时间范围（天），默认 30
     * @param caseLimit 案件点位上限，默认 300
     */
    CollectorMapOverviewDto loadOverview(int caseDays, int caseLimit);
}

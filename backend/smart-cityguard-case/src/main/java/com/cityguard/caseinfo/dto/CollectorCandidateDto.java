package com.cityguard.caseinfo.dto;

import lombok.Data;

/**
 * 下发核查/核实任务时可选采集员（含与案发地距离、最近活动位置）
 */
@Data
public class CollectorCandidateDto {

    private Long userId;

    private String username;

    private String realName;

    private String phone;

    /** 最近活动经度（采集员最近一次上报案件坐标，非实时 GPS） */
    private Double longitude;

    /** 最近活动纬度 */
    private Double latitude;

    /** 与案发地距离（公里），无坐标时为 null */
    private Double distanceKm;

    /** 是否系统推荐（距离最近且在责任片区优先） */
    private Boolean recommended;

    /** 是否绑定在案件所在责任片区 */
    private Boolean onRespGrid;

    private String locationHint;
}

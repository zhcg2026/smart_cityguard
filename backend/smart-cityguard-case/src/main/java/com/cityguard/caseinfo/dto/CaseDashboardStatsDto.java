package com.cityguard.caseinfo.dto;

import lombok.Data;

@Data
public class CaseDashboardStatsDto {

    /** 待处理：上报/立案/派遣队列 + 待指派 + 回退待重派 */
    private long pendingCases;

    /** 处置中：已指派处置人员及后续核实结案阶段 */
    private long processingCases;

    /** 已完成：已结案（含强制结案） */
    private long completedCases;

    /** 超时：未结案且当前阶段计时已超时 */
    private long overdueCases;

    /** 作废：not_accepted（受理员作废）及 cancelled */
    private long cancelledCases;
}

package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CaseDeptStatisticsRow {

    private Long handleDeptId;
    private String handleDeptName;

    private long registeredCount;
    private long dispatchCount;
    private long shouldHandleCount;
    private long handledCount;
    private long pendingHandleCount;
    private long onTimePendingCount;
    private long overduePendingCount;
    private long onTimeHandleCount;
    private long overdueHandleCount;
    private long extensionCount;
    private long suspendCount;
    private long reworkCount;
    private long shouldCloseCount;
    private long onTimeCloseCount;
    private long closedCount;
    private long overdueCloseCount;
    private long appealOnTimeCloseCount;
    private long appealOverdueCloseCount;

    /** 本部门案件占全部统计行案件数百分比（0–100，保留两位） */
    private BigDecimal caseRatio;

    /** 本部门参与统计的案件总数（分母为各行之和） */
    private long deptCaseTotal;
}

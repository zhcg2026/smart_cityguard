package com.cityguard.caseinfo.constant;

/**
 * 考核统计指标键（与反查 SQL 片段对应）
 */
public final class CaseReportMetric {

    private CaseReportMetric() {
    }

    public static final String REGISTERED = "registered";
    public static final String DISPATCH = "dispatch";
    public static final String SHOULD_HANDLE = "shouldHandle";
    public static final String HANDLED = "handled";
    public static final String PENDING_HANDLE = "pendingHandle";
    public static final String ON_TIME_PENDING = "onTimePending";
    public static final String OVERDUE_PENDING = "overduePending";
    public static final String ON_TIME_HANDLE = "onTimeHandle";
    public static final String OVERDUE_HANDLE = "overdueHandle";
    public static final String EXTENSION = "extension";
    public static final String SUSPEND = "suspend";
    public static final String REWORK = "rework";
    public static final String SHOULD_CLOSE = "shouldClose";
    public static final String ON_TIME_CLOSE = "onTimeClose";
    public static final String CLOSED = "closed";
    public static final String OVERDUE_CLOSE = "overdueClose";
    public static final String APPEAL_ON_TIME_CLOSE = "appealOnTimeClose";
    public static final String APPEAL_OVERDUE_CLOSE = "appealOverdueClose";
}

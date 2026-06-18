package com.cityguard.caseinfo.support;

import com.cityguard.caseinfo.constant.CaseReportMetric;
import com.cityguard.common.exception.BusinessException;

/**
 * 考核统计各指标条件（反查 MyBatis 单表，勿使用表别名）
 */
public final class CaseReportMetricSql {

    private static final String CANCELLED = "'not_accepted','cancelled'";

    private static final String PENDING_HANDLE_COND =
            " (case_status IN ('pending_handle','handling') AND handle_finish_time IS NULL "
                    + "AND case_status NOT IN (" + CANCELLED + ")) ";

    private static final String CLOSED_COND =
            " (close_time IS NOT NULL AND case_status IN ('closed','forced_close')) ";

    /** 处置超时申诉通过后不计入超时类指标 */
    private static final String NOT_HANDLE_TIMEOUT_EXEMPT =
            " AND (handle_timeout_exempt IS NULL OR handle_timeout_exempt = 0) ";

    private CaseReportMetricSql() {
    }

    /** 反查列表（case_info 无别名） */
    public static String metricCondition(String metricKey) {
        if (metricKey == null || metricKey.isBlank()) {
            throw new BusinessException("缺少反查指标");
        }
        return switch (metricKey) {
            case CaseReportMetric.REGISTERED -> " AND accept_time IS NOT NULL ";
            case CaseReportMetric.DISPATCH -> " AND dispatch_time IS NOT NULL ";
            case CaseReportMetric.SHOULD_HANDLE ->
                    " AND dispatch_time IS NOT NULL AND case_status NOT IN (" + CANCELLED + ") ";
            case CaseReportMetric.HANDLED ->
                    " AND (handle_finish_time IS NOT NULL OR case_status IN "
                            + "('handle_finish','pending_check','checking','check_pass','closed','forced_close')) ";
            case CaseReportMetric.PENDING_HANDLE -> " AND " + PENDING_HANDLE_COND;
            case CaseReportMetric.ON_TIME_PENDING ->
                    " AND " + PENDING_HANDLE_COND + " AND (deadline_time IS NULL OR deadline_time >= NOW()) ";
            case CaseReportMetric.OVERDUE_PENDING ->
                    " AND " + PENDING_HANDLE_COND
                            + " AND deadline_time IS NOT NULL AND deadline_time < NOW() "
                            + NOT_HANDLE_TIMEOUT_EXEMPT;
            case CaseReportMetric.ON_TIME_HANDLE ->
                    " AND handle_finish_time IS NOT NULL "
                            + "AND (deadline_time IS NULL OR handle_finish_time <= deadline_time) ";
            case CaseReportMetric.OVERDUE_HANDLE ->
                    " AND handle_finish_time IS NOT NULL AND deadline_time IS NOT NULL "
                            + "AND handle_finish_time > deadline_time "
                            + NOT_HANDLE_TIMEOUT_EXEMPT;
            case CaseReportMetric.EXTENSION -> " AND IFNULL(extension_approved_count, 0) > 0 ";
            case CaseReportMetric.SUSPEND ->
                    " AND EXISTS (SELECT 1 FROM case_adjustment_apply ca WHERE ca.case_id = case_info.id "
                            + "AND ca.apply_type = 'suspend' AND ca.apply_status = 'approved') ";
            case CaseReportMetric.REWORK ->
                    " AND EXISTS (SELECT 1 FROM case_flow_record f WHERE f.case_id = case_info.id "
                            + "AND f.node_name IN ('派遣员返工部门','受理员回退返工')) ";
            case CaseReportMetric.SHOULD_CLOSE ->
                    " AND (handle_finish_time IS NOT NULL OR case_status IN "
                            + "('handle_finish','pending_check','checking','pending_close','closed','forced_close')) ";
            case CaseReportMetric.CLOSED -> " AND " + CLOSED_COND;
            case CaseReportMetric.ON_TIME_CLOSE ->
                    " AND " + CLOSED_COND + " AND (deadline_time IS NULL OR close_time <= deadline_time) ";
            case CaseReportMetric.OVERDUE_CLOSE ->
                    " AND " + CLOSED_COND
                            + " AND deadline_time IS NOT NULL AND close_time > deadline_time "
                            + NOT_HANDLE_TIMEOUT_EXEMPT;
            case CaseReportMetric.APPEAL_ON_TIME_CLOSE ->
                    " AND " + CLOSED_COND + " AND handle_timeout_exempt = 1 "
                            + "AND (deadline_time IS NULL OR close_time <= deadline_time) ";
            case CaseReportMetric.APPEAL_OVERDUE_CLOSE ->
                    " AND " + CLOSED_COND + " AND handle_timeout_exempt = 1 "
                            + " AND deadline_time IS NOT NULL AND close_time > deadline_time ";
            default -> throw new BusinessException("未知反查指标: " + metricKey);
        };
    }
}

package com.cityguard.caseinfo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.caseinfo.dto.CaseDeptStatisticsRow;
import com.cityguard.caseinfo.dto.CaseReportCriteria;
import com.cityguard.caseinfo.dto.CaseReportStatisticsResult;
import com.cityguard.caseinfo.entity.CaseInfo;
import com.cityguard.caseinfo.mapper.CaseInfoMapper;
import com.cityguard.caseinfo.service.CaseReportService;
import com.cityguard.caseinfo.support.CaseDynamicWhereBuilder;
import com.cityguard.caseinfo.support.CaseReportMetricSql;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.geo.service.RespGridService;
import com.cityguard.timer.constant.TimerStageConstant;
import com.cityguard.timer.model.CaseTimerDisplayInfo;
import com.cityguard.timer.service.CaseTimerService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseReportServiceImpl implements CaseReportService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SUPERVISOR = "SUPERVISOR";
    private static final String ROLE_EVALUATOR = "EVALUATOR";

    private final JdbcTemplate jdbcTemplate;
    private final CaseInfoMapper caseInfoMapper;
    private final CaseTimerService caseTimerService;
    private final RespGridService respGridService;

    private static final String STATS_SELECT = """
            SELECT c.handle_dept_id AS handle_dept_id,
                   MAX(c.handle_dept_name) AS handle_dept_name,
                   SUM(CASE WHEN c.accept_time IS NOT NULL THEN 1 ELSE 0 END) AS registered_count,
                   SUM(CASE WHEN c.dispatch_time IS NOT NULL THEN 1 ELSE 0 END) AS dispatch_count,
                   SUM(CASE WHEN c.dispatch_time IS NOT NULL AND c.case_status NOT IN ('not_accepted','cancelled') THEN 1 ELSE 0 END) AS should_handle_count,
                   SUM(CASE WHEN (c.handle_finish_time IS NOT NULL OR c.case_status IN ('handle_finish','pending_check','checking','check_pass','closed','forced_close')) THEN 1 ELSE 0 END) AS handled_count,
                   SUM(CASE WHEN c.case_status IN ('pending_handle','handling') AND c.handle_finish_time IS NULL AND c.case_status NOT IN ('not_accepted','cancelled') THEN 1 ELSE 0 END) AS pending_handle_count,
                   SUM(CASE WHEN c.case_status IN ('pending_handle','handling') AND c.handle_finish_time IS NULL AND c.case_status NOT IN ('not_accepted','cancelled') AND (c.deadline_time IS NULL OR c.deadline_time >= NOW()) THEN 1 ELSE 0 END) AS on_time_pending_count,
                   SUM(CASE WHEN c.case_status IN ('pending_handle','handling') AND c.handle_finish_time IS NULL AND c.case_status NOT IN ('not_accepted','cancelled') AND c.deadline_time IS NOT NULL AND c.deadline_time < NOW() AND (c.handle_timeout_exempt IS NULL OR c.handle_timeout_exempt = 0) THEN 1 ELSE 0 END) AS overdue_pending_count,
                   SUM(CASE WHEN c.handle_finish_time IS NOT NULL AND (c.deadline_time IS NULL OR c.handle_finish_time <= c.deadline_time) THEN 1 ELSE 0 END) AS on_time_handle_count,
                   SUM(CASE WHEN c.handle_finish_time IS NOT NULL AND c.deadline_time IS NOT NULL AND c.handle_finish_time > c.deadline_time AND (c.handle_timeout_exempt IS NULL OR c.handle_timeout_exempt = 0) THEN 1 ELSE 0 END) AS overdue_handle_count,
                   SUM(CASE WHEN IFNULL(c.extension_approved_count, 0) > 0 THEN 1 ELSE 0 END) AS extension_count,
                   SUM(CASE WHEN EXISTS (SELECT 1 FROM case_adjustment_apply ca WHERE ca.case_id = c.id AND ca.apply_type = 'suspend' AND ca.apply_status = 'approved') THEN 1 ELSE 0 END) AS suspend_count,
                   SUM(CASE WHEN (c.case_status IN ('returned','check_not_pass') OR EXISTS (SELECT 1 FROM case_flow_record f WHERE f.case_id = c.id AND (f.node_name LIKE '%回退%' OR f.operate_type = 'return'))) THEN 1 ELSE 0 END) AS rework_count,
                   SUM(CASE WHEN (c.handle_finish_time IS NOT NULL OR c.case_status IN ('handle_finish','pending_check','checking','pending_close','closed','forced_close')) THEN 1 ELSE 0 END) AS should_close_count,
                   SUM(CASE WHEN c.close_time IS NOT NULL AND c.case_status IN ('closed','forced_close') AND (c.deadline_time IS NULL OR c.close_time <= c.deadline_time) THEN 1 ELSE 0 END) AS on_time_close_count,
                   SUM(CASE WHEN c.close_time IS NOT NULL AND c.case_status IN ('closed','forced_close') THEN 1 ELSE 0 END) AS closed_count,
                   SUM(CASE WHEN c.close_time IS NOT NULL AND c.case_status IN ('closed','forced_close') AND c.deadline_time IS NOT NULL AND c.close_time > c.deadline_time AND (c.handle_timeout_exempt IS NULL OR c.handle_timeout_exempt = 0) THEN 1 ELSE 0 END) AS overdue_close_count,
                   SUM(CASE WHEN c.close_time IS NOT NULL AND c.case_status IN ('closed','forced_close') AND c.handle_timeout_exempt = 1 AND (c.deadline_time IS NULL OR c.close_time <= c.deadline_time) THEN 1 ELSE 0 END) AS appeal_on_time_close_count,
                   SUM(CASE WHEN c.close_time IS NOT NULL AND c.case_status IN ('closed','forced_close') AND c.handle_timeout_exempt = 1 AND c.deadline_time IS NOT NULL AND c.close_time > c.deadline_time THEN 1 ELSE 0 END) AS appeal_overdue_close_count,
                   COUNT(*) AS dept_case_total
            FROM case_info c
            WHERE c.handle_dept_id IS NOT NULL
            """;

    @Override
    public CaseReportStatisticsResult statistics(CaseReportCriteria criteria, Long userId, List<String> roles) {
        assertReportRole(roles);
        CaseReportCriteria q = criteria != null ? criteria : new CaseReportCriteria();
        CaseDynamicWhereBuilder.WhereParts where = CaseDynamicWhereBuilder.build(q);
        String sql = STATS_SELECT + where.sql() + " GROUP BY c.handle_dept_id ORDER BY handle_dept_name ";
        List<CaseDeptStatisticsRow> rows = jdbcTemplate.query(sql, rowMapper(), where.params().toArray());
        long grandTotal = rows.stream().mapToLong(CaseDeptStatisticsRow::getDeptCaseTotal).sum();
        for (CaseDeptStatisticsRow row : rows) {
            row.setCaseRatio(calcRatio(row.getDeptCaseTotal(), grandTotal));
        }
        CaseReportStatisticsResult result = new CaseReportStatisticsResult();
        result.setRows(rows);
        result.setTotalRow(sumTotalRow(rows, grandTotal));
        return result;
    }

    @Override
    public Page<CaseInfo> drillDown(CaseReportCriteria criteria, Long userId, List<String> roles) {
        assertReportRole(roles);
        CaseReportCriteria q = criteria != null ? criteria : new CaseReportCriteria();
        if (q.getMetricKey() == null || q.getMetricKey().isBlank()) {
            throw new BusinessException("缺少反查指标");
        }
        if (q.getDrillHandleDeptId() == null) {
            throw new BusinessException("缺少处置部门");
        }
        int pageNum = q.getPageNum() != null && q.getPageNum() > 0 ? q.getPageNum() : 1;
        int pageSize = q.getPageSize() != null && q.getPageSize() > 0 ? Math.min(q.getPageSize(), 100) : 10;
        Page<CaseInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CaseInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseInfo::getHandleDeptId, q.getDrillHandleDeptId());
        CaseDynamicWhereBuilder.applyToWrapper(wrapper, q);
        String metricSql = CaseReportMetricSql.metricCondition(q.getMetricKey()).trim();
        if (metricSql.regionMatches(true, 0, "AND ", 0, 4)) {
            metricSql = metricSql.substring(4).trim();
        }
        wrapper.apply(metricSql);
        wrapper.orderByDesc(CaseInfo::getReportTime);
        Page<CaseInfo> pageResult = caseInfoMapper.selectPage(page, wrapper);
        applyTimerDisplay(pageResult.getRecords());
        for (CaseInfo row : pageResult.getRecords()) {
            enrichRespGridName(row);
        }
        return pageResult;
    }

    private static void assertReportRole(List<String> roles) {
        if (roles == null) {
            throw new BusinessException("无权查看考核统计");
        }
        if (roles.contains(ROLE_ADMIN) || roles.contains(ROLE_SUPERVISOR) || roles.contains(ROLE_EVALUATOR)) {
            return;
        }
        throw new BusinessException("无权查看考核统计");
    }

    private RowMapper<CaseDeptStatisticsRow> rowMapper() {
        return (rs, rowNum) -> mapRow(rs);
    }

    private static CaseDeptStatisticsRow mapRow(ResultSet rs) throws SQLException {
        CaseDeptStatisticsRow r = new CaseDeptStatisticsRow();
        r.setHandleDeptId(rs.getLong("handle_dept_id"));
        r.setHandleDeptName(rs.getString("handle_dept_name"));
        r.setRegisteredCount(rs.getLong("registered_count"));
        r.setDispatchCount(rs.getLong("dispatch_count"));
        r.setShouldHandleCount(rs.getLong("should_handle_count"));
        r.setHandledCount(rs.getLong("handled_count"));
        r.setPendingHandleCount(rs.getLong("pending_handle_count"));
        r.setOnTimePendingCount(rs.getLong("on_time_pending_count"));
        r.setOverduePendingCount(rs.getLong("overdue_pending_count"));
        r.setOnTimeHandleCount(rs.getLong("on_time_handle_count"));
        r.setOverdueHandleCount(rs.getLong("overdue_handle_count"));
        r.setExtensionCount(rs.getLong("extension_count"));
        r.setSuspendCount(rs.getLong("suspend_count"));
        r.setReworkCount(rs.getLong("rework_count"));
        r.setShouldCloseCount(rs.getLong("should_close_count"));
        r.setOnTimeCloseCount(rs.getLong("on_time_close_count"));
        r.setClosedCount(rs.getLong("closed_count"));
        r.setOverdueCloseCount(rs.getLong("overdue_close_count"));
        r.setAppealOnTimeCloseCount(rs.getLong("appeal_on_time_close_count"));
        r.setAppealOverdueCloseCount(rs.getLong("appeal_overdue_close_count"));
        r.setDeptCaseTotal(rs.getLong("dept_case_total"));
        return r;
    }

    private static BigDecimal calcRatio(long part, long total) {
        if (total <= 0 || part <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(part * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private static CaseDeptStatisticsRow sumTotalRow(List<CaseDeptStatisticsRow> rows, long grandTotal) {
        CaseDeptStatisticsRow t = new CaseDeptStatisticsRow();
        t.setHandleDeptName("合计");
        for (CaseDeptStatisticsRow r : rows) {
            t.setRegisteredCount(t.getRegisteredCount() + r.getRegisteredCount());
            t.setDispatchCount(t.getDispatchCount() + r.getDispatchCount());
            t.setShouldHandleCount(t.getShouldHandleCount() + r.getShouldHandleCount());
            t.setHandledCount(t.getHandledCount() + r.getHandledCount());
            t.setPendingHandleCount(t.getPendingHandleCount() + r.getPendingHandleCount());
            t.setOnTimePendingCount(t.getOnTimePendingCount() + r.getOnTimePendingCount());
            t.setOverduePendingCount(t.getOverduePendingCount() + r.getOverduePendingCount());
            t.setOnTimeHandleCount(t.getOnTimeHandleCount() + r.getOnTimeHandleCount());
            t.setOverdueHandleCount(t.getOverdueHandleCount() + r.getOverdueHandleCount());
            t.setExtensionCount(t.getExtensionCount() + r.getExtensionCount());
            t.setSuspendCount(t.getSuspendCount() + r.getSuspendCount());
            t.setReworkCount(t.getReworkCount() + r.getReworkCount());
            t.setShouldCloseCount(t.getShouldCloseCount() + r.getShouldCloseCount());
            t.setOnTimeCloseCount(t.getOnTimeCloseCount() + r.getOnTimeCloseCount());
            t.setClosedCount(t.getClosedCount() + r.getClosedCount());
            t.setOverdueCloseCount(t.getOverdueCloseCount() + r.getOverdueCloseCount());
            t.setAppealOnTimeCloseCount(t.getAppealOnTimeCloseCount() + r.getAppealOnTimeCloseCount());
            t.setAppealOverdueCloseCount(t.getAppealOverdueCloseCount() + r.getAppealOverdueCloseCount());
            t.setDeptCaseTotal(t.getDeptCaseTotal() + r.getDeptCaseTotal());
        }
        t.setCaseRatio(grandTotal > 0 ? new BigDecimal("100.00") : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        return t;
    }

    private void applyTimerDisplay(List<CaseInfo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (CaseInfo c : records) {
            CaseTimerDisplayInfo display = caseTimerService.buildCaseTimerDisplay(c.getId());
            if (display == null) {
                continue;
            }
            c.setTimerStage(display.getTimerStage());
            c.setTimerStageName(display.getStageName());
            c.setStageDeadlineTime(display.getDeadlineTime());
            if (display.getTimeRemaining() != null) {
                c.setTimeRemaining(display.getTimeRemaining());
            }
            if (display.getHandleRemainingSeconds() != null) {
                c.setHandleRemainingSeconds(display.getHandleRemainingSeconds());
            }
            if (display.getStageTimeout() != null) {
                c.setStageTimeout(display.getStageTimeout());
            }
            if (display.getHandleTimeout() != null) {
                c.setHandleTimeout(display.getHandleTimeout());
            } else if (!TimerStageConstant.HANDLE.equals(display.getTimerStage())) {
                c.setHandleTimeout(null);
            }
        }
    }

    private void enrichRespGridName(CaseInfo caseInfo) {
        if (caseInfo.getRespGridId() == null) {
            return;
        }
        var grid = respGridService.getById(caseInfo.getRespGridId());
        if (grid != null) {
            caseInfo.setRespGridName(grid.getRespGridName());
        }
    }
}

package com.cityguard.caseinfo.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cityguard.caseinfo.dto.CaseDateFilter;
import com.cityguard.caseinfo.dto.CaseQueryCriteria;
import com.cityguard.caseinfo.dto.CaseReportCriteria;
import com.cityguard.caseinfo.entity.CaseInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 将 CaseReportCriteria / CaseQueryCriteria 转为 SQL WHERE 片段（表别名 c）
 */
public final class CaseDynamicWhereBuilder {

    private CaseDynamicWhereBuilder() {
    }

    public static final class WhereParts {
        private final StringBuilder sql = new StringBuilder();
        private final List<Object> params = new ArrayList<>();

        public void append(String fragment, Object... values) {
            sql.append(fragment);
            for (Object v : values) {
                params.add(v);
            }
        }

        public String sql() {
            return sql.toString();
        }

        public List<Object> params() {
            return params;
        }
    }

    public static WhereParts build(CaseReportCriteria q) {
        WhereParts w = new WhereParts();
        w.append(" AND c.is_deleted = 0 ");
        if (q == null) {
            return w;
        }
        if (q.getCaseCode() != null && !q.getCaseCode().isBlank()) {
            String code = q.getCaseCode().trim();
            String match = q.getCaseCodeMatch() != null ? q.getCaseCodeMatch().trim() : "exact";
            if ("prefix".equalsIgnoreCase(match)) {
                w.append(" AND c.case_code LIKE ? ", code + "%");
            } else {
                w.append(" AND c.case_code = ? ", code);
            }
        }
        appendDateFilterSql(w, "c.report_time", q.getReportTime(), false);
        appendDateFilterSql(w, "c.close_time", q.getCloseTime(), true);
        appendDateFilterSql(w, "c.deadline_time", q.getDeadlineTime(), false);
        appendCaseOriginSql(w, CaseQueryFilterSupport.resolveCaseOrigins(q));
        if (q.getRespGridIds() != null && !q.getRespGridIds().isEmpty()) {
            w.append(" AND c.resp_grid_id IN (" + inPlaceholders(q.getRespGridIds().size()) + ") ",
                    q.getRespGridIds().toArray());
        }
        if (q.getCaseStatuses() != null && !q.getCaseStatuses().isEmpty()) {
            w.append(" AND c.case_status IN (" + inPlaceholders(q.getCaseStatuses().size()) + ") ",
                    q.getCaseStatuses().toArray());
        }
        if (q.getSmallIds() != null && !q.getSmallIds().isEmpty()) {
            w.append(" AND c.small_id IN (" + inPlaceholders(q.getSmallIds().size()) + ") ",
                    q.getSmallIds().toArray());
        }
        if (q.getHandleDeptId() != null) {
            w.append(" AND c.handle_dept_id = ? ", q.getHandleDeptId());
        }
        if (q.getReporterId() != null) {
            w.append(" AND c.reporter_id = ? ", q.getReporterId());
        }
        if (q.getRegisterOperatorId() != null) {
            w.append(" AND c.register_operator_id = ? ", q.getRegisterOperatorId());
        }
        if (q.getDispatchOperatorId() != null) {
            w.append(" AND c.dispatch_operator_id = ? ", q.getDispatchOperatorId());
        }
        if (q.getAddress() != null && !q.getAddress().isBlank()) {
            String addr = q.getAddress().trim();
            String match = q.getAddressMatch() != null ? q.getAddressMatch().trim() : "contains";
            if ("eq".equalsIgnoreCase(match)) {
                w.append(" AND c.address = ? ", addr);
            } else {
                w.append(" AND c.address LIKE ? ", "%" + addr + "%");
            }
        }
        if (q.getCategoryType() != null && !q.getCategoryType().isBlank()) {
            w.append(" AND c.category_type = ? ", q.getCategoryType().trim());
        }
        if (q.getDescription() != null && !q.getDescription().isBlank()) {
            String desc = q.getDescription().trim();
            String match = q.getDescriptionMatch() != null ? q.getDescriptionMatch().trim() : "contains";
            if ("eq".equalsIgnoreCase(match)) {
                w.append(" AND c.description = ? ", desc);
            } else {
                w.append(" AND c.description LIKE ? ", "%" + desc + "%");
            }
        }
        return w;
    }

    private static void appendCaseOriginSql(WhereParts w, List<String> origins) {
        if (origins == null || origins.isEmpty()) {
            return;
        }
        StringBuilder clause = new StringBuilder(" AND (");
        boolean first = true;
        for (String raw : origins) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            if (!first) {
                clause.append(" OR ");
            }
            first = false;
            String key = raw.trim().toLowerCase(Locale.ROOT);
            switch (key) {
                case "collector" -> clause.append("c.source_type = ?");
                case "public" -> clause.append("c.source_type = ?");
                case "video" -> clause.append("c.source_type = ?");
                case "leader" -> clause.append("(c.source_type = ? OR (c.source_type = ? AND c.source_desc LIKE ?))");
                case "phone" -> clause.append("(c.source_type = ? AND c.source_desc LIKE ?)");
                case "citizen" -> clause.append("(c.source_type = ? AND c.source_desc LIKE ?)");
                default -> clause.append("c.source_type = ?");
            }
        }
        if (first) {
            return;
        }
        clause.append(") ");
        w.append(clause.toString(), caseOriginSqlParams(origins));
    }

    private static Object[] caseOriginSqlParams(List<String> origins) {
        List<Object> params = new ArrayList<>();
        for (String raw : origins) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String key = raw.trim().toLowerCase(Locale.ROOT);
            switch (key) {
                case "collector", "public", "video", "default" -> params.add(key);
                case "leader" -> {
                    params.add("leader");
                    params.add("register");
                    params.add("%领导%");
                }
                case "phone" -> {
                    params.add("register");
                    params.add("%电话%");
                }
                case "citizen" -> {
                    params.add("register");
                    params.add("%市民%");
                }
            }
        }
        return params.toArray();
    }

    public static void applyToWrapper(LambdaQueryWrapper<CaseInfo> wrapper, CaseReportCriteria q) {
        wrapper.eq(CaseInfo::getIsDeleted, 0);
        if (q == null) {
            return;
        }
        if (q.getCaseCode() != null && !q.getCaseCode().isBlank()) {
            String code = q.getCaseCode().trim();
            String match = q.getCaseCodeMatch() != null ? q.getCaseCodeMatch().trim() : "exact";
            if ("prefix".equalsIgnoreCase(match)) {
                wrapper.likeRight(CaseInfo::getCaseCode, code);
            } else {
                wrapper.eq(CaseInfo::getCaseCode, code);
            }
        }
        applyDateFilterWrapper(wrapper, CaseInfo::getReportTime, q.getReportTime(), false);
        applyDateFilterWrapper(wrapper, CaseInfo::getCloseTime, q.getCloseTime(), true);
        applyDateFilterWrapper(wrapper, CaseInfo::getDeadlineTime, q.getDeadlineTime(), false);
        if (q.getCategoryType() != null && !q.getCategoryType().isBlank()) {
            wrapper.eq(CaseInfo::getCategoryType, q.getCategoryType().trim());
        }
        CaseQueryFilterSupport.applyCaseOriginFilter(wrapper, CaseQueryFilterSupport.resolveCaseOrigins(q));
        if (q.getRespGridIds() != null && !q.getRespGridIds().isEmpty()) {
            wrapper.in(CaseInfo::getRespGridId, q.getRespGridIds());
        }
        if (q.getCaseStatuses() != null && !q.getCaseStatuses().isEmpty()) {
            wrapper.in(CaseInfo::getCaseStatus, q.getCaseStatuses());
        }
        if (q.getSmallIds() != null && !q.getSmallIds().isEmpty()) {
            wrapper.in(CaseInfo::getSmallId, q.getSmallIds());
        }
        if (q.getHandleDeptId() != null) {
            wrapper.eq(CaseInfo::getHandleDeptId, q.getHandleDeptId());
        }
        if (q.getReporterId() != null) {
            wrapper.eq(CaseInfo::getReporterId, q.getReporterId());
        }
        if (q.getRegisterOperatorId() != null) {
            wrapper.eq(CaseInfo::getRegisterOperatorId, q.getRegisterOperatorId());
        }
        if (q.getDispatchOperatorId() != null) {
            wrapper.eq(CaseInfo::getDispatchOperatorId, q.getDispatchOperatorId());
        }
        if (q.getAddress() != null && !q.getAddress().isBlank()) {
            String addr = q.getAddress().trim();
            String match = q.getAddressMatch() != null ? q.getAddressMatch().trim() : "contains";
            if ("eq".equalsIgnoreCase(match)) {
                wrapper.eq(CaseInfo::getAddress, addr);
            } else {
                wrapper.like(CaseInfo::getAddress, addr);
            }
        }
        if (q.getDescription() != null && !q.getDescription().isBlank()) {
            String desc = q.getDescription().trim();
            String match = q.getDescriptionMatch() != null ? q.getDescriptionMatch().trim() : "contains";
            if ("eq".equalsIgnoreCase(match)) {
                wrapper.eq(CaseInfo::getDescription, desc);
            } else {
                wrapper.like(CaseInfo::getDescription, desc);
            }
        }
    }

    private static String inPlaceholders(int n) {
        return String.join(",", java.util.Collections.nCopies(n, "?"));
    }

    private static void appendDateFilterSql(WhereParts w, String column, CaseDateFilter filter,
                                            boolean requireNonNullForEq) {
        if (filter == null || filter.getOp() == null || filter.getOp().isBlank()) {
            return;
        }
        String op = filter.getOp().trim().toLowerCase(Locale.ROOT);
        if ("between".equals(op)) {
            LocalDateTime start = parseDateTimeParam(filter.getStart(), false);
            LocalDateTime end = parseDateTimeParam(filter.getEnd(), true);
            if (start != null) {
                w.append(" AND " + column + " >= ? ", start);
            }
            if (end != null) {
                w.append(" AND " + column + " <= ? ", end);
            }
            return;
        }
        LocalDateTime dayStart = parseDateTimeParam(filter.getStart(), false);
        LocalDateTime dayEnd = parseDateTimeParam(filter.getStart(), true);
        switch (op) {
            case "eq" -> {
                if (dayStart == null) {
                    return;
                }
                if (requireNonNullForEq) {
                    w.append(" AND " + column + " IS NOT NULL ");
                }
                w.append(" AND " + column + " >= ? ", dayStart);
                if (dayEnd != null) {
                    w.append(" AND " + column + " <= ? ", dayEnd);
                }
            }
            case "gt" -> {
                if (dayEnd != null) {
                    w.append(" AND " + column + " > ? ", dayEnd);
                }
            }
            case "lt" -> {
                if (dayStart != null) {
                    w.append(" AND " + column + " < ? ", dayStart);
                }
            }
            default -> {
            }
        }
    }

    private static void applyDateFilterWrapper(
            LambdaQueryWrapper<CaseInfo> wrapper,
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<CaseInfo, LocalDateTime> column,
            CaseDateFilter filter,
            boolean requireNonNullForEq) {
        if (filter == null || filter.getOp() == null || filter.getOp().isBlank()) {
            return;
        }
        String op = filter.getOp().trim().toLowerCase(Locale.ROOT);
        if ("between".equals(op)) {
            LocalDateTime start = parseDateTimeParam(filter.getStart(), false);
            LocalDateTime end = parseDateTimeParam(filter.getEnd(), true);
            if (start != null) {
                wrapper.ge(column, start);
            }
            if (end != null) {
                wrapper.le(column, end);
            }
            return;
        }
        LocalDateTime dayStart = parseDateTimeParam(filter.getStart(), false);
        LocalDateTime dayEnd = parseDateTimeParam(filter.getStart(), true);
        switch (op) {
            case "eq" -> {
                if (dayStart == null) {
                    return;
                }
                if (requireNonNullForEq) {
                    wrapper.isNotNull(column);
                }
                wrapper.ge(column, dayStart);
                if (dayEnd != null) {
                    wrapper.le(column, dayEnd);
                }
            }
            case "gt" -> {
                if (dayEnd != null) {
                    wrapper.gt(column, dayEnd);
                }
            }
            case "lt" -> {
                if (dayStart != null) {
                    wrapper.lt(column, dayStart);
                }
            }
            default -> {
            }
        }
    }

    private static LocalDateTime parseDateTimeParam(String raw, boolean endOfDay) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        try {
            if (s.length() == 10 && s.charAt(4) == '-') {
                LocalDate d = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
                return endOfDay ? LocalDateTime.of(d, LocalTime.of(23, 59, 59)) : LocalDateTime.of(d, LocalTime.MIN);
            }
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
}

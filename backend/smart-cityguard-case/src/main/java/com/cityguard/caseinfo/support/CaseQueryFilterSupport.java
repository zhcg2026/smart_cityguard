package com.cityguard.caseinfo.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cityguard.caseinfo.dto.CaseQueryCriteria;
import com.cityguard.caseinfo.entity.CaseInfo;

import java.util.List;
import java.util.Locale;

/**
 * 综合查询共用条件：业务来源、问题描述等
 */
public final class CaseQueryFilterSupport {

    private CaseQueryFilterSupport() {
    }

    public static void applyCommonFilters(LambdaQueryWrapper<CaseInfo> wrapper, CaseQueryCriteria q) {
        if (q == null) {
            return;
        }
        if (q.getCategoryType() != null && !q.getCategoryType().isBlank()) {
            wrapper.eq(CaseInfo::getCategoryType, q.getCategoryType().trim());
        }
        applyCaseOriginFilter(wrapper, resolveCaseOrigins(q));
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

    /** 新字段 caseOrigins 优先；兼容旧 sourceTypes（仅渠道码，不含 register/transfer） */
    public static List<String> resolveCaseOrigins(CaseQueryCriteria q) {
        if (q.getCaseOrigins() != null && !q.getCaseOrigins().isEmpty()) {
            return q.getCaseOrigins();
        }
        if (q.getSourceTypes() == null || q.getSourceTypes().isEmpty()) {
            return List.of();
        }
        return q.getSourceTypes().stream()
                .map(s -> s != null ? s.trim().toLowerCase(Locale.ROOT) : "")
                .filter(s -> !s.isEmpty() && !"register".equals(s) && !"transfer".equals(s))
                .distinct()
                .toList();
    }

    public static void applyCaseOriginFilter(LambdaQueryWrapper<CaseInfo> wrapper, List<String> origins) {
        if (origins == null || origins.isEmpty()) {
            return;
        }
        wrapper.and(outer -> {
            boolean first = true;
            for (String raw : origins) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String key = raw.trim().toLowerCase(Locale.ROOT);
                if (!first) {
                    outer.or();
                }
                first = false;
                switch (key) {
                    case "collector" -> outer.eq(CaseInfo::getSourceType, "collector");
                    case "public" -> outer.eq(CaseInfo::getSourceType, "public");
                    case "video" -> outer.eq(CaseInfo::getSourceType, "video");
                    case "leader" -> outer.and(g -> g.eq(CaseInfo::getSourceType, "leader")
                            .or(h -> h.eq(CaseInfo::getSourceType, "register")
                                    .and(i -> i.like(CaseInfo::getSourceDesc, "领导"))));
                    case "phone" -> outer.and(g -> g.eq(CaseInfo::getSourceType, "register")
                            .and(h -> h.like(CaseInfo::getSourceDesc, "电话")));
                    case "citizen" -> outer.and(g -> g.eq(CaseInfo::getSourceType, "register")
                            .and(h -> h.like(CaseInfo::getSourceDesc, "市民")));
                    default -> outer.eq(CaseInfo::getSourceType, key);
                }
            }
        });
    }
}

package com.cityguard.geo.service.impl;

import com.cityguard.geo.dto.CollectorMapCasePointDto;
import com.cityguard.geo.dto.CollectorMapCollectorDto;
import com.cityguard.geo.dto.CollectorMapOverviewDto;
import com.cityguard.geo.entity.ResponsibilityGrid;
import com.cityguard.geo.service.CollectorMapService;
import com.cityguard.geo.service.RespGridService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CollectorMapServiceImpl implements CollectorMapService {

    private static final String ROLE_COLLECTOR = "COLLECTOR";

    private final JdbcTemplate jdbcTemplate;
    private final RespGridService respGridService;

    @Override
    public CollectorMapOverviewDto loadOverview(int caseDays, int caseLimit) {
        int days = Math.max(1, Math.min(caseDays, 365));
        int limit = Math.max(1, Math.min(caseLimit, 500));

        CollectorMapOverviewDto dto = new CollectorMapOverviewDto();
        dto.setGrids(respGridService.listAll());
        dto.setCollectors(loadCollectors());
        dto.setCases(loadCasePoints(days, limit));
        attachGridNames(dto.getCases(), dto.getGrids());
        return dto;
    }

    private List<CollectorMapCollectorDto> loadCollectors() {
        String sql = """
                SELECT u.id, u.username, u.real_name
                FROM sys_user u
                INNER JOIN sys_role_user ru ON ru.user_id = u.id
                INNER JOIN sys_role r ON r.id = ru.role_id AND r.deleted = 0
                WHERE r.role_code = ? AND u.deleted = 0 AND u.status = 1
                ORDER BY u.real_name, u.username
                """;
        List<CollectorMapCollectorDto> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            CollectorMapCollectorDto row = new CollectorMapCollectorDto();
            row.setId(rs.getLong("id"));
            row.setUsername(rs.getString("username"));
            row.setRealName(rs.getString("real_name"));
            return row;
        }, ROLE_COLLECTOR);

        Map<Long, List<Long>> gridByCollector = loadCollectorGridMap();
        for (CollectorMapCollectorDto c : list) {
            c.setRespGridIds(gridByCollector.getOrDefault(c.getId(), List.of()));
        }
        return list;
    }

    private Map<Long, List<Long>> loadCollectorGridMap() {
        String sql = """
                SELECT user_id, resp_grid_id
                FROM responsibility_grid_collector
                ORDER BY user_id, resp_grid_id
                """;
        Map<Long, List<Long>> map = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            long userId = rs.getLong("user_id");
            long gridId = rs.getLong("resp_grid_id");
            map.computeIfAbsent(userId, k -> new ArrayList<>()).add(gridId);
        });
        return map;
    }

    private List<CollectorMapCasePointDto> loadCasePoints(int days, int limit) {
        String sql = """
                SELECT c.id, c.case_code, c.case_status, c.longitude, c.latitude,
                       c.resp_grid_id, c.reporter_id, c.report_time, g.resp_grid_name
                FROM case_info c
                LEFT JOIN responsibility_grid g ON g.id = c.resp_grid_id AND g.is_deleted = 0
                WHERE c.is_deleted = 0
                  AND c.longitude IS NOT NULL AND c.latitude IS NOT NULL
                  AND c.report_time >= DATE_SUB(NOW(), INTERVAL ? DAY)
                ORDER BY c.report_time DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CollectorMapCasePointDto p = new CollectorMapCasePointDto();
            p.setId(rs.getLong("id"));
            p.setCaseCode(rs.getString("case_code"));
            p.setCaseStatus(rs.getString("case_status"));
            p.setLongitude(rs.getDouble("longitude"));
            p.setLatitude(rs.getDouble("latitude"));
            long gridId = rs.getLong("resp_grid_id");
            if (!rs.wasNull()) {
                p.setRespGridId(gridId);
            }
            long reporterId = rs.getLong("reporter_id");
            if (!rs.wasNull()) {
                p.setReporterId(reporterId);
            }
            p.setRespGridName(rs.getString("resp_grid_name"));
            Timestamp ts = rs.getTimestamp("report_time");
            if (ts != null) {
                p.setReportTime(ts.toLocalDateTime());
            }
            return p;
        }, days, limit);
    }

    private void attachGridNames(List<CollectorMapCasePointDto> cases, List<ResponsibilityGrid> grids) {
        if (cases == null || grids == null) {
            return;
        }
        Map<Long, String> nameMap = new HashMap<>();
        for (ResponsibilityGrid g : grids) {
            if (g.getId() != null) {
                nameMap.put(g.getId(), g.getRespGridName());
            }
        }
        for (CollectorMapCasePointDto p : cases) {
            if ((p.getRespGridName() == null || p.getRespGridName().isBlank())
                    && p.getRespGridId() != null) {
                p.setRespGridName(nameMap.get(p.getRespGridId()));
            }
        }
    }
}

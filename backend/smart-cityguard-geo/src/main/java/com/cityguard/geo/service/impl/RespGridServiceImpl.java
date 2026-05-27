package com.cityguard.geo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cityguard.geo.entity.RespGridCollector;
import com.cityguard.geo.entity.ResponsibilityGrid;
import com.cityguard.geo.mapper.RespGridCollectorMapper;
import com.cityguard.geo.mapper.ResponsibilityGridMapper;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.geo.service.RespGridService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 责任网格（片区）服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RespGridServiceImpl implements RespGridService {

    private final ResponsibilityGridMapper respGridMapper;
    private final RespGridCollectorMapper collectorMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ResponsibilityGrid> listAll() {
        List<ResponsibilityGrid> list = respGridMapper.selectAllActive();
        fillCollectors(list);
        return list;
    }

    @Override
    public ResponsibilityGrid getById(Long id) {
        ResponsibilityGrid grid = respGridMapper.selectById(id);
        if (grid != null) {
            fillCollectors(Collections.singletonList(grid));
        }
        return grid;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(ResponsibilityGrid respGrid) {
        if (respGrid.getRespGridCode() == null || respGrid.getRespGridCode().isEmpty()) {
            respGrid.setRespGridCode(generateCode());
        }
        if (respGrid.getBoundary() != null && (respGrid.getCenterLng() == null || respGrid.getCenterLat() == null)) {
            calculateCenter(respGrid);
        }
        respGrid.setStatus(1);
        respGrid.setUserId(null);
        respGridMapper.insert(respGrid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ResponsibilityGrid respGrid) {
        if (respGrid.getBoundary() != null) {
            calculateCenter(respGrid);
        }
        respGridMapper.updateById(respGrid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        collectorMapper.delete(new LambdaQueryWrapper<RespGridCollector>().eq(RespGridCollector::getRespGridId, id));
        respGridMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importGeoJson(MultipartFile file) {
        int total = 0, success = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String content = reader.lines().collect(Collectors.joining("\n"));
            JsonNode geoJson = objectMapper.readTree(content);

            if (!"FeatureCollection".equals(geoJson.path("type").asText())) {
                throw new IllegalArgumentException("文件格式错误：需要 FeatureCollection 类型");
            }

            JsonNode features = geoJson.path("features");
            if (!features.isArray() || features.size() == 0) {
                throw new IllegalArgumentException("文件中没有要素（Feature）");
            }

            total = features.size();
            int nextAreaSeq = Optional.ofNullable(respGridMapper.selectMaxAreaCodeNumeric()).orElse(0);

            for (int i = 0; i < features.size(); i++) {
                try {
                    JsonNode feature = features.get(i);
                    String geometryType = feature.path("geometry").path("type").asText();

                    if (!"Polygon".equals(geometryType)) {
                        skipped++;
                        errors.add("第" + (i + 1) + "个要素：不支持的几何类型 " + geometryType + "，已跳过");
                        continue;
                    }

                    JsonNode properties = feature.path("properties");
                    String name = properties.has("name") ? properties.get("name").asText() : null;
                    if (name == null || name.isEmpty()) {
                        name = "片区" + (i + 1);
                    }

                    String boundary = objectMapper.writeValueAsString(feature.get("geometry"));

                    ResponsibilityGrid respGrid = new ResponsibilityGrid();
                    respGrid.setRespGridCode(String.format("AREA-%04d", ++nextAreaSeq));
                    respGrid.setRespGridName(name);
                    respGrid.setBoundary(boundary);
                    calculateCenter(respGrid);

                    respGridMapper.insert(respGrid);
                    success++;
                } catch (Exception e) {
                    skipped++;
                    errors.add("第" + (i + 1) + "个要素导入失败：" + e.getMessage());
                    log.error("导入第{}个要素失败", i + 1, e);
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("文件解析失败：" + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setGridCollectors(Long respGridId, List<Long> userIds) {
        ResponsibilityGrid respGrid = respGridMapper.selectById(respGridId);
        if (respGrid == null) {
            throw new RuntimeException("片区不存在");
        }
        collectorMapper.delete(new LambdaQueryWrapper<RespGridCollector>().eq(RespGridCollector::getRespGridId, respGridId));

        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        if (userIds != null) {
            for (Long uid : userIds) {
                if (uid != null && uid > 0) {
                    unique.add(uid);
                }
            }
        }
        for (Long uid : unique) {
            RespGridCollector row = new RespGridCollector();
            row.setRespGridId(respGridId);
            row.setUserId(uid);
            collectorMapper.insert(row);
        }

        respGridMapper.update(null, new LambdaUpdateWrapper<ResponsibilityGrid>()
                .eq(ResponsibilityGrid::getId, respGridId)
                .set(ResponsibilityGrid::getUserId, null));
    }

    @Override
    public List<ResponsibilityGrid> listGridsByCollectorUserId(Long userId) {
        Set<Long> gridIds = new LinkedHashSet<>();
        List<RespGridCollector> links = collectorMapper.selectList(
                new LambdaQueryWrapper<RespGridCollector>().eq(RespGridCollector::getUserId, userId));
        for (RespGridCollector c : links) {
            gridIds.add(c.getRespGridId());
        }
        List<ResponsibilityGrid> legacy = respGridMapper.selectList(
                new LambdaQueryWrapper<ResponsibilityGrid>().eq(ResponsibilityGrid::getUserId, userId));
        for (ResponsibilityGrid g : legacy) {
            gridIds.add(g.getId());
        }
        if (gridIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ResponsibilityGrid> list = respGridMapper.selectList(
                new LambdaQueryWrapper<ResponsibilityGrid>().in(ResponsibilityGrid::getId, gridIds));
        fillCollectors(list);
        list.sort(Comparator.comparing(ResponsibilityGrid::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    @Override
    public boolean checkPointInArea(Long respGridId, Double lng, Double lat) {
        ResponsibilityGrid respGrid = respGridMapper.selectById(respGridId);
        if (respGrid == null || respGrid.getBoundary() == null) {
            return false;
        }
        return pointInPolygon(lng, lat, respGrid.getBoundary());
    }

    @Override
    public ResponsibilityGrid validateCollectorReportLocation(Long collectorUserId, Double lng, Double lat) {
        if (collectorUserId == null) {
            throw new BusinessException("上报人信息无效");
        }
        if (lng == null || lat == null) {
            throw new BusinessException("请先在地图上选择上报位置");
        }
        List<ResponsibilityGrid> assigned = listGridsByCollectorUserId(collectorUserId);
        if (assigned == null || assigned.isEmpty()) {
            throw new BusinessException("您尚未绑定责任片区，无法上报问题，请联系管理员在「地理信息-网格管理」中配置");
        }
        for (ResponsibilityGrid grid : assigned) {
            if (grid.getBoundary() != null && pointInPolygon(lng, lat, grid.getBoundary())) {
                return grid;
            }
        }
        List<String> assignedNames = assigned.stream()
                .map(ResponsibilityGrid::getRespGridName)
                .filter(Objects::nonNull)
                .toList();
        for (ResponsibilityGrid grid : listAll()) {
            if (grid.getBoundary() != null && pointInPolygon(lng, lat, grid.getBoundary())) {
                throw new BusinessException(String.format(
                        "上报位置位于「%s」，不在您的责任片区（%s）内，无法提交",
                        grid.getRespGridName(),
                        String.join("、", assignedNames)));
            }
        }
        throw new BusinessException(String.format(
                "上报位置不在您的责任片区（%s）范围内，无法提交",
                String.join("、", assignedNames)));
    }

    private void fillCollectors(List<ResponsibilityGrid> grids) {
        if (grids == null || grids.isEmpty()) {
            return;
        }
        List<Long> ids = grids.stream().map(ResponsibilityGrid::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return;
        }
        List<RespGridCollector> links = collectorMapper.selectList(
                new LambdaQueryWrapper<RespGridCollector>().in(RespGridCollector::getRespGridId, ids));
        Map<Long, List<Long>> byGrid = links.stream()
                .collect(Collectors.groupingBy(RespGridCollector::getRespGridId,
                        Collectors.mapping(RespGridCollector::getUserId, Collectors.toList())));
        for (ResponsibilityGrid g : grids) {
            List<Long> uids = new ArrayList<>(byGrid.getOrDefault(g.getId(), Collections.emptyList()));
            if (g.getUserId() != null && !uids.contains(g.getUserId())) {
                uids.add(g.getUserId());
            }
            g.setCollectorUserIds(uids);
        }
    }

    private String generateCode() {
        int max = Optional.ofNullable(respGridMapper.selectMaxAreaCodeNumeric()).orElse(0);
        return String.format("AREA-%04d", max + 1);
    }

    private void calculateCenter(ResponsibilityGrid respGrid) {
        try {
            JsonNode geometry = objectMapper.readTree(respGrid.getBoundary());
            JsonNode coordinates = geometry.path("coordinates");
            JsonNode ring = coordinates.get(0);

            double sumLng = 0, sumLat = 0;
            int count = ring.size() - 1;
            for (int i = 0; i < count; i++) {
                JsonNode point = ring.get(i);
                sumLng += point.get(0).asDouble();
                sumLat += point.get(1).asDouble();
            }
            respGrid.setCenterLng(BigDecimal.valueOf(sumLng / count).setScale(6, RoundingMode.HALF_UP));
            respGrid.setCenterLat(BigDecimal.valueOf(sumLat / count).setScale(6, RoundingMode.HALF_UP));
        } catch (Exception e) {
            log.warn("计算中心点失败: {}", e.getMessage());
        }
    }

    private boolean pointInPolygon(Double lng, Double lat, String boundary) {
        try {
            JsonNode geometry = objectMapper.readTree(boundary);
            JsonNode coordinates = geometry.path("coordinates");
            JsonNode ring = coordinates.get(0);

            int n = ring.size() - 1;
            boolean inside = false;

            for (int i = 0, j = n - 1; i < n; j = i++) {
                double xi = ring.get(i).get(0).asDouble();
                double yi = ring.get(i).get(1).asDouble();
                double xj = ring.get(j).get(0).asDouble();
                double yj = ring.get(j).get(1).asDouble();

                boolean intersect = ((yi > lat) != (yj > lat))
                        && (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi);
                if (intersect) {
                    inside = !inside;
                }
            }
            return inside;
        } catch (Exception e) {
            log.warn("点在多边形内判断失败: {}", e.getMessage());
            return false;
        }
    }
}

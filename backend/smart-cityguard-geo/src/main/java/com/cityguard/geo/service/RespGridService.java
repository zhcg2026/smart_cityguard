package com.cityguard.geo.service;

import com.cityguard.geo.entity.ResponsibilityGrid;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 责任网格（片区）服务接口
 */
public interface RespGridService {

    List<ResponsibilityGrid> listAll();

    ResponsibilityGrid getById(Long id);

    void create(ResponsibilityGrid respGrid);

    void update(ResponsibilityGrid respGrid);

    void delete(Long id);

    Map<String, Object> importGeoJson(MultipartFile file);

    /**
     * 设置片区绑定的采集员（全量替换，可传空列表表示全部解绑）
     */
    void setGridCollectors(Long respGridId, List<Long> userIds);

    /**
     * 查询某采集员所属的片区列表（含关联表与历史 user_id）
     */
    List<ResponsibilityGrid> listGridsByCollectorUserId(Long userId);

    boolean checkPointInArea(Long respGridId, Double lng, Double lat);

    /**
     * 校验采集员是否可在该坐标上报，返回匹配的责任片区；不通过时抛出业务异常。
     */
    ResponsibilityGrid validateCollectorReportLocation(Long collectorUserId, Double lng, Double lat);
}

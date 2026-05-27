package com.cityguard.geo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 责任网格（片区）实体
 */
@Data
@TableName("responsibility_grid")
public class ResponsibilityGrid {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 责任网格编码 */
    private String respGridCode;

    /** 责任网格名称（如：东片区、西片区） */
    private String respGridName;

    /** 绑定的采集员用户ID（历史字段，已迁移至多对多表，勿再写入业务） */
    private Long userId;

    /** 绑定的采集员用户ID列表（关联表 responsibility_grid_collector，查询时填充） */
    @TableField(exist = false)
    private List<Long> collectorUserIds;

    /** 包含的单元网格ID列表（预留扩展） */
    private String gridIds;

    /** 责任网格总面积（平方米） */
    private BigDecimal area;

    /** 网格边界（GeoJSON Polygon） */
    private String boundary;

    /** 中心经度 */
    private BigDecimal centerLng;

    /** 中心纬度 */
    private BigDecimal centerLat;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：1=正常，0=停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
package com.cityguard.geo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 片区与采集员多对多关联
 */
@Data
@TableName("responsibility_grid_collector")
public class RespGridCollector {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long respGridId;

    private Long userId;

    private LocalDateTime createTime;
}

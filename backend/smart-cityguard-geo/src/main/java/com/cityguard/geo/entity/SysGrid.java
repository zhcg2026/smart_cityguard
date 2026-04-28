package com.cityguard.geo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_grid")
public class SysGrid {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String gridName;

    private String gridCode;

    private Long communityId;

    private Long streetId;

    private String boundary;

    private Double centerLongitude;

    private Double centerLatitude;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
package com.cityguard.geo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CollectorMapCollectorDto {
    private Long id;
    private String username;
    private String realName;
    /** 所属责任片区 ID 列表 */
    private List<Long> respGridIds = new ArrayList<>();
}

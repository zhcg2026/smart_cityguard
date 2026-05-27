package com.cityguard.geo.dto;

import com.cityguard.geo.entity.ResponsibilityGrid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CollectorMapOverviewDto {
    private List<CollectorMapCollectorDto> collectors = new ArrayList<>();
    private List<ResponsibilityGrid> grids = new ArrayList<>();
    private List<CollectorMapCasePointDto> cases = new ArrayList<>();
}

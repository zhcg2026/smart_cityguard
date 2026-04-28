package com.cityguard.geo.service.impl;

import com.cityguard.geo.entity.SysStreet;
import com.cityguard.geo.entity.SysCommunity;
import com.cityguard.geo.entity.SysGrid;
import com.cityguard.geo.mapper.SysStreetMapper;
import com.cityguard.geo.mapper.SysCommunityMapper;
import com.cityguard.geo.mapper.SysGridMapper;
import com.cityguard.geo.service.GeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeoServiceImpl implements GeoService {

    private final SysStreetMapper streetMapper;
    private final SysCommunityMapper communityMapper;
    private final SysGridMapper gridMapper;

    @Override
    public List<SysStreet> getStreetList() {
        return streetMapper.selectAll();
    }

    @Override
    public List<SysCommunity> getCommunityList(Long streetId) {
        return communityMapper.selectByStreetId(streetId);
    }

    @Override
    public List<SysGrid> getGridList(Long communityId) {
        return gridMapper.selectByCommunityId(communityId);
    }

    @Override
    public SysGrid getGridInfo(Long gridId) {
        return gridMapper.selectById(gridId);
    }

    @Override
    public SysGrid findNearestGrid(Double lng, Double lat) {
        return gridMapper.findNearestGrid(lng, lat);
    }
}
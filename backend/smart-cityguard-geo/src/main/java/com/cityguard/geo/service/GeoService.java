package com.cityguard.geo.service;

import com.cityguard.geo.entity.SysStreet;
import com.cityguard.geo.entity.SysCommunity;
import com.cityguard.geo.entity.SysGrid;

import java.util.List;

public interface GeoService {

    List<SysStreet> getStreetList();

    List<SysCommunity> getCommunityList(Long streetId);

    List<SysGrid> getGridList(Long communityId);

    SysGrid getGridInfo(Long gridId);

    SysGrid findNearestGrid(Double lng, Double lat);
}
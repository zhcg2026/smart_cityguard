package com.cityguard.geo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.geo.entity.SysGrid;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysGridMapper extends BaseMapper<SysGrid> {

    @Select("SELECT * FROM sys_grid WHERE community_id = #{communityId} AND is_deleted = 0 ORDER BY sort")
    List<SysGrid> selectByCommunityId(Long communityId);

    @Select("SELECT g.* FROM sys_grid g " +
            "WHERE g.is_deleted = 0 " +
            "AND ST_Distance(ST_MakePoint(g.center_longitude, g.center_latitude)::geography, " +
            "ST_MakePoint(#{lng}, #{lat})::geography) < 1000 " +
            "ORDER BY ST_Distance(ST_MakePoint(g.center_longitude, g.center_latitude)::geography, " +
            "ST_MakePoint(#{lng}, #{lat})::geography) " +
            "LIMIT 1")
    SysGrid findNearestGrid(@Param("lng") Double lng, @Param("lat") Double lat);
}
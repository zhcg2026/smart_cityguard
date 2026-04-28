package com.cityguard.geo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.geo.entity.SysCommunity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysCommunityMapper extends BaseMapper<SysCommunity> {

    @Select("SELECT * FROM sys_community WHERE street_id = #{streetId} AND is_deleted = 0 ORDER BY sort")
    List<SysCommunity> selectByStreetId(Long streetId);
}
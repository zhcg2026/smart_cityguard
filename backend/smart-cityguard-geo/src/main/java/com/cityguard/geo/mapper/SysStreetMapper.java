package com.cityguard.geo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.geo.entity.SysStreet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysStreetMapper extends BaseMapper<SysStreet> {

    @Select("SELECT * FROM sys_street WHERE is_deleted = 0 ORDER BY sort")
    List<SysStreet> selectAll();
}
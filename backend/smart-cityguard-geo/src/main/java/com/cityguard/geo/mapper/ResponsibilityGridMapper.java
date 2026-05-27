package com.cityguard.geo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.geo.entity.ResponsibilityGrid;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ResponsibilityGridMapper extends BaseMapper<ResponsibilityGrid> {

    /**
     * 查询所有有效片区
     */
    @Select("SELECT * FROM responsibility_grid WHERE is_deleted = 0 ORDER BY sort_order")
    List<ResponsibilityGrid> selectAllActive();

    /**
     * 当前库中 AREA- 数字后缀的最大值（按数值，非字符串序），用于生成不重复编码
     */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(resp_grid_code, 6) AS UNSIGNED)), 0) "
            + "FROM responsibility_grid WHERE is_deleted = 0 AND resp_grid_code REGEXP '^AREA-[0-9]+$'")
    Integer selectMaxAreaCodeNumeric();
}
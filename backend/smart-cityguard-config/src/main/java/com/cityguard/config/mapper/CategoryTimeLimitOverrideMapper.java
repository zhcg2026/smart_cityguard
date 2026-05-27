package com.cityguard.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.config.entity.CategoryTimeLimitOverride;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CategoryTimeLimitOverrideMapper extends BaseMapper<CategoryTimeLimitOverride> {

    @Select("SELECT * FROM category_time_limit_override WHERE small_id = #{smallId} AND status = 1 AND is_deleted = 0 LIMIT 1")
    CategoryTimeLimitOverride selectBySmallId(Long smallId);
}

package com.cityguard.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.config.entity.TimeLimitRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TimeLimitRuleMapper extends BaseMapper<TimeLimitRule> {

    @Select("SELECT * FROM time_limit_rule WHERE category_small_id = #{categorySmallId} AND status = 1")
    TimeLimitRule selectByCategorySmallId(Long categorySmallId);
}
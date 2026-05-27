package com.cityguard.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.config.entity.TimeLimitRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TimeLimitRuleMapper extends BaseMapper<TimeLimitRule> {

    @Select("SELECT * FROM time_limit_rule WHERE time_limit_type = #{timeLimitType} AND status = 1 AND is_deleted = 0 LIMIT 1")
    TimeLimitRule selectByType(String timeLimitType);

    @Select("SELECT * FROM time_limit_rule WHERE status = 1 AND is_deleted = 0 ORDER BY sort_order, id")
    java.util.List<TimeLimitRule> selectAllActive();
}

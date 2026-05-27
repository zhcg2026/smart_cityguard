package com.cityguard.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.message.entity.DailyTip;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DailyTipMapper extends BaseMapper<DailyTip> {

    @Select("SELECT * FROM daily_tip WHERE status = 'published' AND is_deleted = 0 AND (expire_time IS NULL OR expire_time > NOW()) ORDER BY publish_time DESC")
    List<DailyTip> selectPublishedActive();

    @Select("SELECT * FROM daily_tip WHERE is_deleted = 0 ORDER BY publish_time DESC, id DESC")
    List<DailyTip> selectAllAdmin();
}
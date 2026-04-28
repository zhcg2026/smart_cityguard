package com.cityguard.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.message.entity.DailyTip;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DailyTipMapper extends BaseMapper<DailyTip> {

    @Select("SELECT * FROM daily_tip WHERE status = 1 AND publish_date = #{date} ORDER BY sort")
    List<DailyTip> selectByDate(@Param("date") String date);

    @Select("SELECT * FROM daily_tip WHERE status = 1 ORDER BY sort LIMIT #{limit}")
    List<DailyTip> selectLatest(@Param("limit") int limit);
}
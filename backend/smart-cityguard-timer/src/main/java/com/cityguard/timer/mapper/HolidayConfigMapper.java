package com.cityguard.timer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.timer.entity.HolidayConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HolidayConfigMapper extends BaseMapper<HolidayConfig> {

    @Select("SELECT * FROM holiday_config WHERE holiday_date = #{date} AND is_deleted = 0 LIMIT 1")
    HolidayConfig selectByDate(LocalDate date);

    @Select("SELECT * FROM holiday_config WHERE year = #{year} AND is_deleted = 0")
    List<HolidayConfig> selectByYear(int year);
}

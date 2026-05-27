package com.cityguard.timer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.timer.entity.WorkTimeConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorkTimeConfigMapper extends BaseMapper<WorkTimeConfig> {

    @Select("SELECT * FROM work_time_config WHERE is_default = 1 AND status = 1 AND is_deleted = 0 LIMIT 1")
    WorkTimeConfig selectDefault();
}

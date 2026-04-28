package com.cityguard.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.task.entity.CheckTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CheckTaskMapper extends BaseMapper<CheckTask> {

    @Select("SELECT * FROM check_task WHERE collector_id = #{collectorId} AND status = #{status} AND is_deleted = 0 ORDER BY deadline")
    List<CheckTask> selectByCollectorAndStatus(Long collectorId, Integer status);

    @Select("SELECT * FROM check_task WHERE is_deleted = 0 ORDER BY create_time DESC")
    List<CheckTask> selectAll();
}
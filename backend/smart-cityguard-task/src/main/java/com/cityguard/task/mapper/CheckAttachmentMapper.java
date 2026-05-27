package com.cityguard.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.task.entity.CheckAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CheckAttachmentMapper extends BaseMapper<CheckAttachment> {

    @Select("SELECT * FROM check_attachment WHERE check_task_id = #{taskId} AND is_deleted = 0 ORDER BY id")
    List<CheckAttachment> selectByCheckTaskId(@Param("taskId") Long taskId);
}

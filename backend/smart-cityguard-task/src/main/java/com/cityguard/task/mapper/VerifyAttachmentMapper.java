package com.cityguard.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.task.entity.VerifyAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VerifyAttachmentMapper extends BaseMapper<VerifyAttachment> {

    @Select("SELECT * FROM verify_attachment WHERE verify_task_id = #{taskId} AND is_deleted = 0 ORDER BY id")
    List<VerifyAttachment> selectByVerifyTaskId(@Param("taskId") Long taskId);
}

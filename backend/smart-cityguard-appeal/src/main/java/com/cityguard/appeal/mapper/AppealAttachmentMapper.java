package com.cityguard.appeal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.appeal.entity.AppealAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AppealAttachmentMapper extends BaseMapper<AppealAttachment> {

    @Select("SELECT * FROM appeal_attachment WHERE appeal_id = #{appealId} ORDER BY upload_time")
    List<AppealAttachment> selectByAppealId(Long appealId);
}
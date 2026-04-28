package com.cityguard.caseinfo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.caseinfo.entity.CaseAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CaseAttachmentMapper extends BaseMapper<CaseAttachment> {

    @Select("SELECT * FROM case_attachment WHERE case_id = #{caseId} ORDER BY upload_time")
    List<CaseAttachment> selectByCaseId(Long caseId);
}
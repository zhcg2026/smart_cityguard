package com.cityguard.appeal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.appeal.entity.AppealApply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AppealApplyMapper extends BaseMapper<AppealApply> {

    @Select("SELECT * FROM appeal_apply WHERE applicant_id = #{applicantId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<AppealApply> selectByApplicantId(Long applicantId);

    @Select("SELECT * FROM appeal_apply WHERE status = #{status} AND is_deleted = 0 ORDER BY create_time DESC")
    List<AppealApply> selectByStatus(Integer status);

    @Select("SELECT * FROM appeal_apply WHERE is_deleted = 0 ORDER BY create_time DESC")
    List<AppealApply> selectAll();
}
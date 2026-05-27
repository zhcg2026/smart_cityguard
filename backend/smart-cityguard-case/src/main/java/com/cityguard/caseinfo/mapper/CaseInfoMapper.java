package com.cityguard.caseinfo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.caseinfo.entity.CaseInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CaseInfoMapper extends BaseMapper<CaseInfo> {

    @Select("SELECT * FROM case_info WHERE case_status = #{status} AND is_deleted = 0 ORDER BY report_time DESC")
    List<CaseInfo> selectByStatus(String status);

    @Select("SELECT * FROM case_info WHERE is_deleted = 0 ORDER BY report_time DESC")
    List<CaseInfo> selectAll();

    @Select("SELECT * FROM case_info WHERE reporter_id = #{reporterId} AND is_deleted = 0 ORDER BY report_time DESC")
    List<CaseInfo> selectByReporterId(Long reporterId);
}
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

    @Select("SELECT * FROM case_info WHERE status = #{status} AND is_deleted = 0 ORDER BY report_time DESC")
    List<CaseInfo> selectByStatus(Integer status);

    @Select("SELECT * FROM case_info WHERE is_deleted = 0 ORDER BY report_time DESC")
    List<CaseInfo> selectAll();

    @Select("SELECT * FROM case_info WHERE collector_id = #{collectorId} AND is_deleted = 0 ORDER BY report_time DESC")
    List<CaseInfo> selectByCollectorId(Long collectorId);

    String generateCaseNo();
}
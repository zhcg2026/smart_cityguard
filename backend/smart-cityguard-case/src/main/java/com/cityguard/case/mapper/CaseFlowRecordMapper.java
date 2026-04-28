package com.cityguard.case.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.case.entity.CaseFlowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CaseFlowRecordMapper extends BaseMapper<CaseFlowRecord> {

    @Select("SELECT * FROM case_flow_record WHERE case_id = #{caseId} ORDER BY operate_time")
    List<CaseFlowRecord> selectByCaseId(Long caseId);
}
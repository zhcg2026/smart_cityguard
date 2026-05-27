package com.cityguard.caseinfo.mapper;

import com.cityguard.caseinfo.entity.CaseFlowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 仅查询；新增流转记录由 {@link com.cityguard.caseinfo.service.impl.CaseServiceImpl} 用 JdbcTemplate 写入，避免 MP 生成不含 case_code 的 insert。
 */
@Mapper
public interface CaseFlowRecordMapper {

    @Select("SELECT * FROM case_flow_record WHERE case_id = #{caseId} ORDER BY operate_time")
    List<CaseFlowRecord> selectByCaseId(Long caseId);
}
package com.cityguard.caseinfo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.caseinfo.entity.CaseAdjustmentApply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CaseAdjustmentApplyMapper extends BaseMapper<CaseAdjustmentApply> {

    @Select("""
            SELECT COUNT(1) FROM case_adjustment_apply
            WHERE case_id = #{caseId} AND apply_type = #{applyType}
              AND apply_status = 'approved' AND is_deleted = 0
            """)
    int countApproved(@Param("caseId") Long caseId, @Param("applyType") String applyType);

    @Select("""
            SELECT COUNT(1) FROM case_adjustment_apply
            WHERE case_id = #{caseId} AND apply_type = #{applyType}
              AND apply_status = 'pending' AND is_deleted = 0
            """)
    int countPending(@Param("caseId") Long caseId, @Param("applyType") String applyType);

    @Select("""
            SELECT c.id FROM case_info c
            WHERE c.is_deleted = 0 AND c.is_suspended = 1
              AND c.suspend_until IS NOT NULL AND c.suspend_until <= NOW()
            """)
    List<Long> selectCaseIdsDueForSuspendResume();
}

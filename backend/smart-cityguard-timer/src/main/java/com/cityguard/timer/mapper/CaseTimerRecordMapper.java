package com.cityguard.timer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.timer.entity.CaseTimerRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CaseTimerRecordMapper extends BaseMapper<CaseTimerRecord> {

    @Select("""
            SELECT * FROM case_timer_record
            WHERE case_id = #{caseId} AND timer_stage = #{stage}
              AND timer_status IN ('running', 'paused')
            ORDER BY id DESC LIMIT 1
            """)
    CaseTimerRecord selectActiveByCaseAndStage(@Param("caseId") Long caseId, @Param("stage") String stage);

    @Select("""
            SELECT * FROM case_timer_record
            WHERE case_id = #{caseId} AND timer_stage = #{stage}
            ORDER BY id DESC LIMIT 1
            """)
    CaseTimerRecord selectLatestByCaseAndStage(@Param("caseId") Long caseId, @Param("stage") String stage);
}

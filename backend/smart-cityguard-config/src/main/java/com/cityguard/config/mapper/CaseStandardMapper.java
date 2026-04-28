package com.cityguard.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.config.entity.CaseStandard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CaseStandardMapper extends BaseMapper<CaseStandard> {

    @Select("SELECT * FROM case_standard WHERE category_small_id = #{categorySmallId} AND status = 1 ORDER BY sort")
    List<CaseStandard> selectByCategorySmallId(Long categorySmallId);
}
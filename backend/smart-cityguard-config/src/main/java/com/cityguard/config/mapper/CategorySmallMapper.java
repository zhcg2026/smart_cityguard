package com.cityguard.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.config.entity.CategorySmall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategorySmallMapper extends BaseMapper<CategorySmall> {

    @Select("SELECT * FROM category_small WHERE category_big_id = #{categoryBigId} AND status = 1 ORDER BY sort")
    List<CategorySmall> selectByCategoryBigId(Long categoryBigId);

    @Select("SELECT * FROM category_small WHERE status = 1 ORDER BY category_big_id, sort")
    List<CategorySmall> selectAllActive();
}
package com.cityguard.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.config.entity.CategorySmall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategorySmallMapper extends BaseMapper<CategorySmall> {

    @Select("SELECT * FROM category_small WHERE big_id = #{categoryBigId} AND status = 1 AND is_deleted = 0 ORDER BY sort_order")
    List<CategorySmall> selectByCategoryBigId(Long categoryBigId);

    @Select("SELECT * FROM category_small WHERE status = 1 AND is_deleted = 0 ORDER BY big_id, sort_order")
    List<CategorySmall> selectAllActive();
}
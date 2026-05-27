package com.cityguard.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.config.entity.CategoryBig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryBigMapper extends BaseMapper<CategoryBig> {

    @Select("SELECT * FROM category_big WHERE category_type = #{type} AND status = 1 AND is_deleted = 0 ORDER BY sort_order")
    List<CategoryBig> selectByType(String type);

    @Select("SELECT * FROM category_big WHERE status = 1 AND is_deleted = 0 ORDER BY sort_order")
    List<CategoryBig> selectAllActive();
}
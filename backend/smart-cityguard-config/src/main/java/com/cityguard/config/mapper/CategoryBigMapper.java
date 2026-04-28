package com.cityguard.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.config.entity.CategoryBig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryBigMapper extends BaseMapper<CategoryBig> {

    @Select("SELECT * FROM category_big WHERE type = #{type} AND status = 1 ORDER BY sort")
    List<CategoryBig> selectByType(Integer type);

    @Select("SELECT * FROM category_big WHERE status = 1 ORDER BY sort")
    List<CategoryBig> selectAllActive();
}
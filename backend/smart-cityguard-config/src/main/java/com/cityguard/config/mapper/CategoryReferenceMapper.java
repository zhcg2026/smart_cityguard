package com.cityguard.config.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CategoryReferenceMapper {

    @Select("SELECT COUNT(*) FROM case_info WHERE small_id = #{smallId} AND is_deleted = 0")
    long countCasesBySmallId(@Param("smallId") Long smallId);

    @Select("SELECT COUNT(*) FROM case_info WHERE standard_id = #{standardId} AND is_deleted = 0")
    long countCasesByStandardId(@Param("standardId") Long standardId);

    @Select("SELECT COUNT(*) FROM category_small WHERE big_id = #{bigId} AND is_deleted = 0")
    long countSmallsByBigId(@Param("bigId") Long bigId);

    @Select("SELECT COUNT(*) FROM case_standard WHERE small_id = #{smallId} AND is_deleted = 0")
    long countStandardsBySmallId(@Param("smallId") Long smallId);
}

package com.cityguard.config.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 立结案标准 Excel 全量导入前，按 category_type 物理清理关联数据（避免与唯一索引、逻辑删冲突）。
 */
@Mapper
public interface StandardCatalogCleanupMapper {

    @Delete("DELETE FROM responsibility_config WHERE small_id IN (SELECT id FROM category_small WHERE category_type = #{type})")
    int deleteResponsibilityByCategoryType(@Param("type") String type);

    @Delete("DELETE FROM category_extend_field WHERE small_id IN (SELECT id FROM category_small WHERE category_type = #{type})")
    int deleteExtendByCategoryType(@Param("type") String type);

    @Delete("DELETE FROM case_standard WHERE category_type = #{type}")
    int deleteCaseStandardByCategoryType(@Param("type") String type);

    @Delete("DELETE FROM category_small WHERE category_type = #{type}")
    int deleteCategorySmallByCategoryType(@Param("type") String type);

    @Delete("DELETE FROM category_big WHERE category_type = #{type}")
    int deleteCategoryBigByCategoryType(@Param("type") String type);
}

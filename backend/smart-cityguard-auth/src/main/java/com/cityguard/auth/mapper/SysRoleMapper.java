package com.cityguard.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.auth.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("SELECT * FROM sys_role WHERE deleted = 0 ORDER BY id")
    List<SysRole> selectAll();

    @Select("SELECT * FROM sys_role WHERE role_code = #{roleCode} AND deleted = 0 LIMIT 1")
    SysRole selectByRoleCode(@Param("roleCode") String roleCode);

    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_role_user ru ON r.id = ru.role_id " +
            "WHERE ru.user_id = #{userId} AND r.deleted = 0")
    List<SysRole> selectByUserId(Long userId);

    @Delete("DELETE FROM sys_role_user WHERE user_id = #{userId}")
    int deleteUserRoles(@Param("userId") Long userId);

    @Select("SELECT role_id FROM sys_role_user WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(Long userId);

    @Update("UPDATE sys_role SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int logicDeleteById(@Param("id") Long id);
}
package com.cityguard.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.auth.entity.SysUser;
import com.cityguard.auth.entity.SysDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    SysUser selectByUsername(String username);

    // 查询用户名是否存在（包括已删除的）
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser selectByUsernameIncludeDeleted(String username);

    @Select("SELECT r.role_code FROM sys_role r " +
            "INNER JOIN sys_role_user ru ON r.id = ru.role_id " +
            "WHERE ru.user_id = #{userId} AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(Long userId);

    @Select("SELECT DISTINCT ru.user_id FROM sys_role_user ru " +
            "INNER JOIN sys_role r ON r.id = ru.role_id AND r.deleted = 0 " +
            "INNER JOIN sys_user u ON u.id = ru.user_id AND u.deleted = 0 " +
            "WHERE r.role_code = #{roleCode}")
    List<Long> selectUserIdsByRoleCode(@Param("roleCode") String roleCode);

    @Select("SELECT d.* FROM sys_department d WHERE d.id = #{deptId} AND d.deleted = 0")
    SysDepartment selectDeptById(Long deptId);

    @Insert("INSERT INTO sys_role_user (role_id, user_id) VALUES (#{roleId}, #{userId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Insert("INSERT IGNORE INTO sys_role (id, role_name, role_code, description, status, deleted) VALUES (1, '管理员', 'admin', '系统管理员', 1, 0)")
    int ensureAdminRoleExists();

    @Update("UPDATE sys_user SET deleted = 1 WHERE id = #{id}")
    int logicDeleteById(Long id);
}
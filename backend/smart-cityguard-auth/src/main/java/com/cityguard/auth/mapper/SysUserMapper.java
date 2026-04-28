package com.cityguard.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 0")
    SysUser selectByUsername(String username);

    @Select("SELECT r.role_code FROM sys_role r " +
            "INNER JOIN sys_role_user ru ON r.id = ru.role_id " +
            "WHERE ru.user_id = #{userId} AND r.is_deleted = 0")
    List<String> selectRoleCodesByUserId(Long userId);

    @Select("SELECT d.* FROM sys_department d WHERE d.id = #{deptId} AND d.is_deleted = 0")
    SysDepartment selectDeptById(Long deptId);
}
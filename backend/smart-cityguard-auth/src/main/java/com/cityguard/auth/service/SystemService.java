package com.cityguard.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.auth.entity.SysUser;
import com.cityguard.auth.entity.SysDepartment;
import com.cityguard.auth.entity.SysRole;

import java.util.List;
import java.util.Map;

public interface SystemService {

    // 用户管理
    Page<SysUser> getUserList(Integer pageNum, Integer pageSize, Map<String, Object> params);
    SysUser getUserDetail(Long id);
    SysUser createUser(SysUser user, List<Long> roleIds);
    SysUser updateUser(SysUser user, List<Long> roleIds);
    void deleteUser(Long id);
    void resetPassword(Long id, String password);
    List<SysRole> getUserRoles(Long userId);

    // 角色管理
    List<SysRole> getRoleList();
    SysRole getRoleDetail(Long id);
    SysRole createRole(SysRole role);
    SysRole updateRole(SysRole role);
    void deleteRole(Long id);

    // 部门管理
    List<SysDepartment> getDeptTree();
    SysDepartment getDeptDetail(Long id);
    SysDepartment createDept(SysDepartment dept);
    SysDepartment updateDept(SysDepartment dept);
    void deleteDept(Long id);

    /** 为部门创建或补全登录账号（二级及以下处置部门） */
    SysDepartment ensureDeptLogin(Long deptId);

    /** 重置部门登录账号密码 */
    void resetDeptLoginPassword(Long deptId, String password);
}
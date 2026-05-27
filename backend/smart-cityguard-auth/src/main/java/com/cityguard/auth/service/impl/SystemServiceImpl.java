package com.cityguard.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.auth.entity.SysUser;
import com.cityguard.auth.entity.SysDepartment;
import com.cityguard.auth.entity.SysRole;
import com.cityguard.auth.mapper.SysUserMapper;
import com.cityguard.auth.mapper.SysDepartmentMapper;
import com.cityguard.auth.mapper.SysRoleMapper;
import com.cityguard.auth.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private final SysUserMapper userMapper;
    private final SysDepartmentMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_DEPT = "DEPT";
    private static final String DEPT_LOGIN_PREFIX = "dept_";

    // ========== 用户管理 ==========

    @Override
    public Page<SysUser> getUserList(Integer pageNum, Integer pageSize, Map<String, Object> params) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // @TableLogic 会自动添加 deleted=0 条件，不需要手动添加

        if (params.get("username") != null && !params.get("username").toString().isEmpty()) {
            wrapper.like(SysUser::getUsername, params.get("username"));
        }
        if (params.get("realName") != null && !params.get("realName").toString().isEmpty()) {
            wrapper.like(SysUser::getRealName, params.get("realName"));
        }
        if (params.get("status") != null) {
            wrapper.eq(SysUser::getStatus, params.get("status"));
        }
        if (isTruthyParam(params.get("unassignedOnly"))) {
            wrapper.and(w -> w.isNull(SysUser::getDepartmentId)
                    .or()
                    .eq(SysUser::getDepartmentId, 0L));
        } else if (params.get("departmentId") != null) {
            wrapper.eq(SysUser::getDepartmentId, params.get("departmentId"));
        }

        String roleCodeFilter = normalizeRoleCodeParam(params.get("roleCode"));
        if (roleCodeFilter != null) {
            List<Long> userIds = userMapper.selectUserIdsByRoleCode(roleCodeFilter);
            if (userIds.isEmpty()) {
                wrapper.eq(SysUser::getId, -1L);
            } else {
                wrapper.in(SysUser::getId, userIds);
            }
        }

        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = userMapper.selectPage(page, wrapper);
        fillDepartmentNames(result.getRecords());
        fillRoleNames(result.getRecords());
        fillSystemProtectedFlag(result.getRecords());
        fillDeptLoginAccountFlag(result.getRecords());
        return result;
    }

    private void fillRoleNames(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        for (SysUser user : users) {
            if (user.getId() == null) {
                continue;
            }
            List<SysRole> roles = roleMapper.selectByUserId(user.getId());
            if (roles == null || roles.isEmpty()) {
                user.setRoleNames("");
                continue;
            }
            String names = roles.stream()
                    .map(SysRole::getRoleName)
                    .filter(n -> n != null && !n.isBlank())
                    .reduce((a, b) -> a + "、" + b)
                    .orElse("");
            user.setRoleNames(names);
        }
    }

    /** 用户列表批量填充所属部门名称 */
    private void fillSystemProtectedFlag(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        for (SysUser user : users) {
            if (user.getId() == null) {
                continue;
            }
            boolean protectedUser = isSystemProtectedUser(user.getId(), user.getUsername());
            user.setSystemProtected(protectedUser);
            if (protectedUser) {
                user.setDepartmentId(null);
                user.setDepartmentName(null);
            }
        }
    }

    private void fillDeptLoginAccountFlag(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        for (SysUser user : users) {
            if (user.getId() == null || Boolean.TRUE.equals(user.getSystemProtected())) {
                continue;
            }
            user.setDeptLoginAccount(isDeptLoginUser(user.getId()));
        }
    }

    private boolean isSystemProtectedUser(Long userId, String username) {
        if (username != null && "admin".equalsIgnoreCase(username.trim())) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        List<String> roleCodes = userMapper.selectRoleCodesByUserId(userId);
        return roleCodes != null && roleCodes.contains(ROLE_ADMIN);
    }

    private boolean isSystemProtectedUser(Long userId) {
        SysUser user = userId != null ? userMapper.selectById(userId) : null;
        return user != null && isSystemProtectedUser(userId, user.getUsername());
    }

    private boolean isDeptLoginUser(Long userId) {
        if (userId == null) {
            return false;
        }
        List<String> roleCodes = userMapper.selectRoleCodesByUserId(userId);
        return roleCodes != null && roleCodes.contains(ROLE_DEPT);
    }

    private void assertUserMutable(Long userId) {
        if (isSystemProtectedUser(userId)) {
            throw new BusinessException("系统管理员账号不可编辑或删除");
        }
        if (isDeptLoginUser(userId)) {
            throw new BusinessException("部门登录账号请通过「部门管理」维护");
        }
    }

    private boolean hasDeptRoleByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        for (Long roleId : roleIds) {
            SysRole role = roleMapper.selectById(roleId);
            if (role != null && ROLE_DEPT.equals(role.getRoleCode())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAdminRoleByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        for (Long roleId : roleIds) {
            SysRole role = roleMapper.selectById(roleId);
            if (role != null && ROLE_ADMIN.equals(role.getRoleCode())) {
                return true;
            }
        }
        return false;
    }

    private void syncDepartmentName(SysUser user) {
        if (user.getDepartmentId() == null) {
            user.setDepartmentName(null);
            return;
        }
        if (user.getDepartmentId() == null) {
            return;
        }
        SysDepartment dept = deptMapper.selectById(user.getDepartmentId());
        if (dept != null) {
            user.setDepartmentName(dept.getDeptName());
        }
    }

    private void fillDepartmentNames(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Set<Long> deptIds = users.stream()
                .map(SysUser::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return;
        }
        List<SysDepartment> depts = deptMapper.selectBatchIds(deptIds);
        if (depts == null || depts.isEmpty()) {
            return;
        }
        Map<Long, String> nameById = depts.stream()
                .collect(Collectors.toMap(SysDepartment::getId, SysDepartment::getDeptName, (a, b) -> a));
        for (SysUser user : users) {
            Long deptId = user.getDepartmentId();
            if (deptId != null) {
                user.setDepartmentName(nameById.get(deptId));
            }
        }
    }

    /** 仅允许大写字母与下划线，防止 SQL 注入；与 sys_role.role_code 约定一致 */
    private static boolean isTruthyParam(Object raw) {
        if (raw == null) {
            return false;
        }
        if (raw instanceof Boolean b) {
            return b;
        }
        String s = raw.toString().trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    private static String normalizeRoleCodeParam(Object raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.toString().trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty() || !s.matches("^[A-Z_]{1,50}$")) {
            return null;
        }
        return s;
    }

    @Override
    public SysUser getUserDetail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user != null) {
            fillDepartmentNames(List.of(user));
            fillRoleNames(List.of(user));
            fillSystemProtectedFlag(List.of(user));
            fillDeptLoginAccountFlag(List.of(user));
            if (Boolean.TRUE.equals(user.getSystemProtected())) {
                user.setDepartmentId(null);
                user.setDepartmentName(null);
            }
        }
        return user;
    }

    @Override
    @Transactional
    public SysUser createUser(SysUser user, List<Long> roleIds) {
        // 检查用户名是否已存在（只检查未删除的，因为唯一约束是 (username, deleted)）
        SysUser existing = userMapper.selectByUsername(user.getUsername());
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }

        if (hasDeptRoleByRoleIds(roleIds)) {
            throw new BusinessException("部门登录账号请通过「部门管理」创建，勿在用户管理中新建");
        }

        if (hasAdminRoleByRoleIds(roleIds)) {
            user.setDepartmentId(null);
            user.setDepartmentName(null);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword() != null ? user.getPassword() : "123456"));
        user.setStatus(1);
        user.setDeleted(0);
        syncDepartmentName(user);
        userMapper.insert(user);

        // 分配角色
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                userMapper.insertUserRole(user.getId(), roleId);
            }
        }
        return user;
    }

    @Override
    @Transactional
    public SysUser updateUser(SysUser user, List<Long> roleIds) {
        SysUser existing = userMapper.selectById(user.getId());
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        assertUserMutable(existing.getId());

        if (hasDeptRoleByRoleIds(roleIds)) {
            throw new BusinessException("部门登录账号请通过「部门管理」维护");
        }

        // 不更新密码，除非专门调用resetPassword
        user.setPassword(existing.getPassword());
        syncDepartmentName(user);
        userMapper.updateById(user);

        // 更新角色
        roleMapper.deleteUserRoles(user.getId());
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                userMapper.insertUserRole(user.getId(), roleId);
            }
        }
        return user;
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        assertUserMutable(id);
        userMapper.logicDeleteById(id);
        roleMapper.deleteUserRoles(id);
    }

    @Override
    public void resetPassword(Long id, String password) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        assertUserMutable(id);
        user.setPassword(passwordEncoder.encode(password != null ? password : "123456"));
        userMapper.updateById(user);
    }

    @Override
    public List<SysRole> getUserRoles(Long userId) {
        return roleMapper.selectByUserId(userId);
    }

    // ========== 角色管理 ==========

    @Override
    public List<SysRole> getRoleList() {
        return roleMapper.selectAll();
    }

    @Override
    public SysRole getRoleDetail(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public SysRole createRole(SysRole role) {
        role.setIsDeleted(0);
        role.setStatus(1);
        roleMapper.insert(role);
        return role;
    }

    @Override
    public SysRole updateRole(SysRole role) {
        roleMapper.updateById(role);
        return role;
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        int rows = roleMapper.logicDeleteById(id);
        if (rows == 0) {
            throw new BusinessException("角色不存在或已删除");
        }
    }

    // ========== 部门管理 ==========

    @Override
    public List<SysDepartment> getDeptTree() {
        List<SysDepartment> list = deptMapper.selectList(
                new LambdaQueryWrapper<SysDepartment>()
                        .eq(SysDepartment::getIsDeleted, 0)
                        .orderByAsc(SysDepartment::getSortOrder)
                        .orderByAsc(SysDepartment::getId)
        );
        fillDeptLoginInfo(list);
        return list;
    }

    @Override
    public SysDepartment getDeptDetail(Long id) {
        SysDepartment dept = deptMapper.selectById(id);
        if (dept != null) {
            fillDeptLoginInfo(List.of(dept));
        }
        return dept;
    }

    @Override
    @Transactional
    public SysDepartment createDept(SysDepartment dept) {
        dept.setIsDeleted(0);
        dept.setStatus(1);
        deptMapper.insert(dept);
        if (shouldHaveDeptLogin(dept)) {
            ensureDeptLoginInternal(dept.getId(), null);
        }
        return getDeptDetail(dept.getId());
    }

    @Override
    public SysDepartment updateDept(SysDepartment dept) {
        deptMapper.updateById(dept);
        return dept;
    }

    @Override
    @Transactional
    public void deleteDept(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("无效的部门ID");
        }
        SysDepartment dept = deptMapper.selectById(id);
        if (dept == null || dept.getIsDeleted() != null && dept.getIsDeleted() != 0) {
            throw new BusinessException("部门不存在或已删除");
        }
        List<SysDepartment> children = deptMapper.selectByParentId(id);
        if (!children.isEmpty()) {
            throw new BusinessException("存在子部门，无法删除");
        }
        long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDepartmentId, id)
                        .eq(SysUser::getDeleted, 0));
        if (dept.getLoginUserId() != null && userCount > 0) {
            userCount--;
        }
        if (userCount > 0) {
            throw new BusinessException("部门下仍有 " + userCount + " 名人员，请先在用户管理中调整后再删除");
        }

        if (dept.getLoginUserId() != null) {
            roleMapper.deleteUserRoles(dept.getLoginUserId());
            userMapper.logicDeleteById(dept.getLoginUserId());
        }

        int rows = deptMapper.logicDeleteById(id);
        if (rows == 0) {
            throw new BusinessException("部门不存在或已删除");
        }
    }

    @Override
    @Transactional
    public SysDepartment ensureDeptLogin(Long deptId) {
        if (deptId == null || deptId <= 0) {
            throw new BusinessException("无效的部门ID");
        }
        SysDepartment dept = deptMapper.selectById(deptId);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        if (!shouldHaveDeptLogin(dept)) {
            throw new BusinessException("该部门无法创建登录账号");
        }
        ensureDeptLoginInternal(deptId, null);
        return getDeptDetail(deptId);
    }

    @Override
    public void resetDeptLoginPassword(Long deptId, String password) {
        SysDepartment dept = deptMapper.selectById(deptId);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        if (dept.getLoginUserId() == null) {
            throw new BusinessException("该部门尚未创建登录账号，请先创建部门登录账号");
        }
        SysUser loginUser = userMapper.selectById(dept.getLoginUserId());
        if (loginUser == null) {
            throw new BusinessException("部门登录账号不存在");
        }
        loginUser.setPassword(passwordEncoder.encode(password != null && !password.isBlank() ? password : "admin123"));
        userMapper.updateById(loginUser);
    }

    /** 一级部门（中心/队伍）即可作为处置单位登录分派案件 */
    private boolean shouldHaveDeptLogin(SysDepartment dept) {
        return dept != null && dept.getDeptLevel() != null && dept.getDeptLevel() >= 1;
    }

    private void ensureDeptLoginInternal(Long deptId, String plainPassword) {
        SysDepartment dept = deptMapper.selectById(deptId);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        if (dept.getLoginUserId() != null) {
            SysUser existing = userMapper.selectById(dept.getLoginUserId());
            if (existing != null && existing.getDeleted() != null && existing.getDeleted() == 0) {
                return;
            }
        }

        SysRole deptRole = roleMapper.selectByRoleCode(ROLE_DEPT);
        if (deptRole == null) {
            throw new BusinessException("未配置部门账号角色(DEPT)，请先执行数据库补丁");
        }

        String username = pickDeptLoginUsername(dept);
        SysUser dup = userMapper.selectByUsername(username);
        if (dup != null && dup.getDeleted() != null && dup.getDeleted() == 0) {
            throw new BusinessException("登录名 " + username + " 已被占用");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setRealName(dept.getDeptName());
        user.setDepartmentId(deptId);
        user.setDepartmentName(dept.getDeptName());
        user.setPassword(passwordEncoder.encode(plainPassword != null ? plainPassword : "admin123"));
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);
        userMapper.insertUserRole(user.getId(), deptRole.getId());

        dept.setLoginUserId(user.getId());
        deptMapper.updateById(dept);
    }

    /** 优先使用部门名称作为登录名，冲突时退回 dept_{id} */
    private String pickDeptLoginUsername(SysDepartment dept) {
        if (dept.getDeptName() != null && !dept.getDeptName().isBlank()) {
            String name = dept.getDeptName().trim();
            SysUser dup = userMapper.selectByUsername(name);
            if (dup == null) {
                return name;
            }
        }
        return DEPT_LOGIN_PREFIX + dept.getId();
    }

    private void fillDeptLoginInfo(List<SysDepartment> depts) {
        if (depts == null || depts.isEmpty()) {
            return;
        }
        Set<Long> loginUserIds = depts.stream()
                .map(SysDepartment::getLoginUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (loginUserIds.isEmpty()) {
            return;
        }
        List<SysUser> users = userMapper.selectBatchIds(loginUserIds);
        if (users == null || users.isEmpty()) {
            return;
        }
        Map<Long, String> usernameById = users.stream()
                .filter(u -> u.getId() != null)
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername, (a, b) -> a));
        for (SysDepartment dept : depts) {
            if (dept.getLoginUserId() != null) {
                dept.setLoginUsername(usernameById.get(dept.getLoginUserId()));
            }
        }
    }
}
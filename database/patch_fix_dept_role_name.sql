-- 修复 DEPT 角色名称/描述乱码（执行 patch_dept_login 时未指定 utf8mb4 会写入 ????）
-- 执行：mysql --default-character-set=utf8mb4 -u root -p cityguard < database/patch_fix_dept_role_name.sql

USE cityguard;

UPDATE sys_role
SET role_name = '部门账号',
    description = '处置部门登录账号，负责接收派遣并分派处置人员'
WHERE role_code = 'DEPT'
  AND deleted = 0
  AND id > 0;

SELECT id, role_code, role_name, description FROM sys_role WHERE role_code = 'DEPT' AND deleted = 0;

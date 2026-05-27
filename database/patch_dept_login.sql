-- 部门登录账号：DEPT 角色 + sys_department.login_user_id + 示例部门账号
-- 执行：mysql --default-character-set=utf8mb4 -u root -p cityguard < database/patch_dept_login.sql

USE cityguard;

-- 部门表增加登录用户关联
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'cityguard' AND TABLE_NAME = 'sys_department' AND COLUMN_NAME = 'login_user_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_department ADD COLUMN login_user_id BIGINT NULL COMMENT ''部门登录账号用户ID'' AFTER status',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- DEPT 角色
INSERT INTO sys_role (role_name, role_code, description, status, deleted)
SELECT '部门账号', 'DEPT', '处置部门登录账号，负责接收派遣并分派处置人员', 1, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'DEPT' AND deleted = 0);

-- 为已有二级处置部门（10/11/12）创建部门登录账号 dept_10 等，默认密码 admin123
-- admin123 BCrypt: $2a$10$3b.WiFgvOKOT4D956hQbpeJjEyBZ6pVyBpE4lW0IlFl5CqaIavFPW

INSERT INTO sys_user (username, password, real_name, department_id, department_name, status, deleted)
SELECT 'dept_10', '$2a$10$3b.WiFgvOKOT4D956hQbpeJjEyBZ6pVyBpE4lW0IlFl5CqaIavFPW', '市容环卫处置科', 10, '市容环卫处置科', 1, 0
FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_department WHERE id = 10 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'dept_10' AND deleted = 0);

INSERT INTO sys_role_user (role_id, user_id)
SELECT r.id, u.id FROM sys_role r
JOIN sys_user u ON u.username = 'dept_10' AND u.deleted = 0
WHERE r.role_code = 'DEPT' AND r.deleted = 0
ON DUPLICATE KEY UPDATE role_id = role_id;

UPDATE sys_department d
JOIN sys_user u ON u.username = 'dept_10' AND u.deleted = 0
SET d.login_user_id = u.id
WHERE d.id = 10 AND d.deleted = 0;

INSERT INTO sys_user (username, password, real_name, department_id, department_name, status, deleted)
SELECT 'dept_11', '$2a$10$3b.WiFgvOKOT4D956hQbpeJjEyBZ6pVyBpE4lW0IlFl5CqaIavFPW', '市政设施处置科', 11, '市政设施处置科', 1, 0
FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_department WHERE id = 11 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'dept_11' AND deleted = 0);

INSERT INTO sys_role_user (role_id, user_id)
SELECT r.id, u.id FROM sys_role r
JOIN sys_user u ON u.username = 'dept_11' AND u.deleted = 0
WHERE r.role_code = 'DEPT' AND r.deleted = 0
ON DUPLICATE KEY UPDATE role_id = role_id;

UPDATE sys_department d
JOIN sys_user u ON u.username = 'dept_11' AND u.deleted = 0
SET d.login_user_id = u.id
WHERE d.id = 11 AND d.deleted = 0;

INSERT INTO sys_user (username, password, real_name, department_id, department_name, status, deleted)
SELECT 'dept_12', '$2a$10$3b.WiFgvOKOT4D956hQbpeJjEyBZ6pVyBpE4lW0IlFl5CqaIavFPW', '园林绿化处置科', 12, '园林绿化处置科', 1, 0
FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_department WHERE id = 12 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'dept_12' AND deleted = 0);

INSERT INTO sys_role_user (role_id, user_id)
SELECT r.id, u.id FROM sys_role r
JOIN sys_user u ON u.username = 'dept_12' AND u.deleted = 0
WHERE r.role_code = 'DEPT' AND r.deleted = 0
ON DUPLICATE KEY UPDATE role_id = role_id;

UPDATE sys_department d
JOIN sys_user u ON u.username = 'dept_12' AND u.deleted = 0
SET d.login_user_id = u.id
WHERE d.id = 12 AND d.deleted = 0;

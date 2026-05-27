-- 恢复示例处置部门并创建部门登录账号（用户名=部门名称，密码 admin123）
USE cityguard;

UPDATE sys_department SET deleted = 0, status = 1 WHERE id IN (10, 11, 12);

-- admin123 有效 BCrypt（与 admin 账号一致）
SET @pwd = '$2a$10$3b.WiFgvOKOT4D956hQbpeJjEyBZ6pVyBpE4lW0IlFl5CqaIavFPW';
SET @role_id = (SELECT id FROM sys_role WHERE role_code = 'DEPT' AND deleted = 0 LIMIT 1);

-- 部门 10
INSERT INTO sys_user (username, password, real_name, department_id, department_name, status, deleted)
SELECT d.dept_name, @pwd, d.dept_name, d.id, d.dept_name, 1, 0
FROM sys_department d
WHERE d.id = 10 AND d.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.username = d.dept_name AND u.deleted = 0);

INSERT INTO sys_role_user (role_id, user_id)
SELECT @role_id, u.id FROM sys_user u
JOIN sys_department d ON d.id = 10 AND u.username = d.dept_name AND u.deleted = 0
WHERE @role_id IS NOT NULL
ON DUPLICATE KEY UPDATE role_id = role_id;

UPDATE sys_department d
JOIN sys_user u ON u.username = d.dept_name AND u.deleted = 0
SET d.login_user_id = u.id
WHERE d.id = 10 AND d.deleted = 0;

-- 部门 11
INSERT INTO sys_user (username, password, real_name, department_id, department_name, status, deleted)
SELECT d.dept_name, @pwd, d.dept_name, d.id, d.dept_name, 1, 0
FROM sys_department d
WHERE d.id = 11 AND d.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.username = d.dept_name AND u.deleted = 0);

INSERT INTO sys_role_user (role_id, user_id)
SELECT @role_id, u.id FROM sys_user u
JOIN sys_department d ON d.id = 11 AND u.username = d.dept_name AND u.deleted = 0
WHERE @role_id IS NOT NULL
ON DUPLICATE KEY UPDATE role_id = role_id;

UPDATE sys_department d
JOIN sys_user u ON u.username = d.dept_name AND u.deleted = 0
SET d.login_user_id = u.id
WHERE d.id = 11 AND d.deleted = 0;

-- 部门 12
INSERT INTO sys_user (username, password, real_name, department_id, department_name, status, deleted)
SELECT d.dept_name, @pwd, d.dept_name, d.id, d.dept_name, 1, 0
FROM sys_department d
WHERE d.id = 12 AND d.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.username = d.dept_name AND u.deleted = 0);

INSERT INTO sys_role_user (role_id, user_id)
SELECT @role_id, u.id FROM sys_user u
JOIN sys_department d ON d.id = 12 AND u.username = d.dept_name AND u.deleted = 0
WHERE @role_id IS NOT NULL
ON DUPLICATE KEY UPDATE role_id = role_id;

UPDATE sys_department d
JOIN sys_user u ON u.username = d.dept_name AND u.deleted = 0
SET d.login_user_id = u.id
WHERE d.id = 12 AND d.deleted = 0;

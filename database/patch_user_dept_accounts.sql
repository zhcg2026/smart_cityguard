-- 为当前在用的一级部门创建登录账号（用户名=部门名称，密码 admin123）
-- 并清理已废弃的示例「处置科」
USE cityguard;

SET @pwd = '$2a$10$3b.WiFgvOKOT4D956hQbpeJjEyBZ6pVyBpE4lW0IlFl5CqaIavFPW';
SET @role_id = (SELECT id FROM sys_role WHERE role_code = 'DEPT' AND deleted = 0 LIMIT 1);

-- 废弃示例二级科
UPDATE sys_department SET deleted = 1 WHERE id IN (10, 11, 12);
UPDATE sys_user SET deleted = 1 WHERE id IN (25, 26, 27);

-- 为在用部门：1,6,8,9,100 创建/关联部门账号
-- 使用存储过程式批量：逐个部门插入

INSERT INTO sys_user (username, password, real_name, department_id, department_name, status, deleted)
SELECT d.dept_name, @pwd, d.dept_name, d.id, d.dept_name, 1, 0
FROM sys_department d
WHERE d.id IN (1, 6, 8, 9, 100) AND d.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.username = d.dept_name AND u.deleted = 0);

INSERT INTO sys_role_user (role_id, user_id)
SELECT @role_id, u.id
FROM sys_user u
JOIN sys_department d ON d.deleted = 0 AND d.id IN (1, 6, 8, 9, 100) AND u.username = d.dept_name AND u.deleted = 0
WHERE @role_id IS NOT NULL
ON DUPLICATE KEY UPDATE role_id = role_id;

UPDATE sys_department d
JOIN sys_user u ON u.username = d.dept_name AND u.deleted = 0
SET d.login_user_id = u.id
WHERE d.id IN (1, 6, 8, 9, 100) AND d.deleted = 0;

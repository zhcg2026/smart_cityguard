-- 修复现有数据库中的角色编码（改为大写，与前端一致）
-- 执行此脚本前请确认当前数据库是 cityguard

USE cityguard;

-- 更新角色编码为大写（使用主键 id 避免 safe update mode 报错）
UPDATE sys_role SET role_code = 'ADMIN' WHERE id = 1;
UPDATE sys_role SET role_code = 'COLLECTOR' WHERE id = 2;
UPDATE sys_role SET role_code = 'ACCEPTOR' WHERE id = 3;
UPDATE sys_role SET role_code = 'DISPATCHER' WHERE id = 4;
UPDATE sys_role SET role_code = 'HANDLER' WHERE id = 5;

-- 补充缺失的角色
INSERT INTO sys_role (id, role_name, role_code, description, status) VALUES
(6, '值班长', 'SUPERVISOR', '值班长角色', 1),
(7, '考核员', 'EVALUATOR', '考核员角色', 1),
(8, '领导', 'LEADER', '领导角色', 1)
ON DUPLICATE KEY UPDATE role_code = VALUES(role_code);

-- 验证结果
SELECT id, role_name, role_code FROM sys_role WHERE deleted = 0;
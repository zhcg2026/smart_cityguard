-- 示例：二级处置部门 + 部门下处置人员（密码均为 admin123 的 BCrypt）
-- 在 cityguard 库执行：mysql ... cityguard < patch_handler_dept_users.sql

USE cityguard;

-- 二级处置部门（挂在监督中心 id=1 下，可按实际调整 parent_id）
INSERT INTO sys_department (id, dept_name, parent_id, dept_level, sort_order, status, deleted) VALUES
(10, '市容环卫处置科', 1, 2, 10, 1, 0),
(11, '市政设施处置科', 1, 2, 11, 1, 0),
(12, '园林绿化处置科', 1, 2, 12, 1, 0)
ON DUPLICATE KEY UPDATE dept_name = VALUES(dept_name), parent_id = VALUES(parent_id), dept_level = VALUES(dept_level);

-- 派遣员（中心级，用于受理员立案后批转）
INSERT INTO sys_user (id, username, password, real_name, phone, department_id, department_name, status, deleted) VALUES
(10, 'dispatcher01', '$2a$10$EqKcp1WFKVQISheBxmXJHePvTnF9.rNvQIV4aBBJmXJHePvTnF9.rNv', '张派遣', '13800000010', 1, '运城市城管监督中心', 1, 0)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), department_id = VALUES(department_id);

INSERT INTO sys_role_user (role_id, user_id)
SELECT r.id, 10 FROM sys_role r WHERE r.role_code = 'DISPATCHER' LIMIT 1
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 各部门处置人员（HANDLER）：登录后可在「部门待指派」处理派遣到本部门的案件
INSERT INTO sys_user (id, username, password, real_name, phone, department_id, department_name, status, deleted) VALUES
(11, 'handler_sr', '$2a$10$EqKcp1WFKVQISheBxmXJHePvTnF9.rNvQIV4aBBJmXJHePvTnF9.rNv', '李环卫', '13800000011', 10, '市容环卫处置科', 1, 0),
(12, 'handler_sz', '$2a$10$EqKcp1WFKVQISheBxmXJHePvTnF9.rNvQIV4aBBJmXJHePvTnF9.rNv', '王市政', '13800000012', 11, '市政设施处置科', 1, 0),
(13, 'handler_yl', '$2a$10$EqKcp1WFKVQISheBxmXJHePvTnF9.rNvQIV4aBBJmXJHePvTnF9.rNv', '赵园林', '13800000013', 12, '园林绿化处置科', 1, 0)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), department_id = VALUES(department_id), department_name = VALUES(department_name);

INSERT INTO sys_role_user (role_id, user_id)
SELECT r.id, u.id FROM sys_role r
JOIN (SELECT 11 AS id UNION SELECT 12 UNION SELECT 13) u
WHERE r.role_code = 'HANDLER'
ON DUPLICATE KEY UPDATE role_id = role_id;

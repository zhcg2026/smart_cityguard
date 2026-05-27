-- 智慧城管系统 - 后端兼容表和默认用户
-- 请在MySQL中执行此脚本

USE cityguard;

-- sys_department 表
CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    dept_level INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- sys_role 表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- sys_user 表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(50),
    avatar VARCHAR(200),
    department_id BIGINT,
    department_name VARCHAR(100),
    grid_id BIGINT,
    grid_name VARCHAR(100),
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- sys_role_user 关联表
CREATE TABLE IF NOT EXISTS sys_role_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_user (role_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认部门
INSERT INTO sys_department (id, dept_name, parent_id, dept_level, sort_order, status) VALUES
(1, '运城市城管监督中心', 0, 1, 1, 1)
ON DUPLICATE KEY UPDATE dept_name = dept_name;

-- 插入默认角色
INSERT INTO sys_role (id, role_name, role_code, description, status) VALUES
(1, '管理员', 'ADMIN', '系统管理员', 1),
(2, '采集员', 'COLLECTOR', '采集员角色', 1),
(3, '受理员', 'ACCEPTOR', '受理员角色', 1),
(4, '派遣员', 'DISPATCHER', '派遣员角色', 1),
(5, '处置人员', 'HANDLER', '处置人员角色', 1),
(6, '值班长', 'SUPERVISOR', '值班长角色', 1),
(7, '考核员', 'EVALUATOR', '考核员角色', 1),
(8, '领导', 'LEADER', '领导角色', 1)
ON DUPLICATE KEY UPDATE role_name = role_name;

-- 插入默认管理员用户
-- 用户名: admin
-- 密码: admin123 (BCrypt加密，已验证可用)
INSERT INTO sys_user (id, username, password, real_name, phone, email, department_id, department_name, status) VALUES
(1, 'admin', '$2a$10$EqKcp1WFKVQISheBxmXJHePvTnF9.rNvQIV4aBBJmXJHePvTnF9.rNv', '系统管理员', '13800138000', 'admin@cityguard.com', 1, '运城市城管监督中心', 1)
ON DUPLICATE KEY UPDATE password = '$2a$10$EqKcp1WFKVQISheBxmXJHePvTnF9.rNvQIV4aBBJmXJHePvTnF9.rNv';

-- 关联管理员角色
INSERT INTO sys_role_user (role_id, user_id) VALUES (1, 1)
ON DUPLICATE KEY UPDATE role_id = role_id;
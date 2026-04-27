-- 智慧城管系统数据库初始化脚本
-- 运城市城市综合管理服务系统

-- 创建数据库
CREATE DATABASE IF NOT EXISTS cityguard DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cityguard;

-- ============================================
-- 1. 用户与角色
-- ============================================

-- 部门表
CREATE TABLE sys_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '部门名称',
    type VARCHAR(20) COMMENT '类型：监管/责任/处置',
    parent_id BIGINT DEFAULT 0 COMMENT '上级部门',
    address VARCHAR(200) COMMENT '地址',
    phone VARCHAR(20) COMMENT '电话',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '部门表';

-- 角色表
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    code VARCHAR(30) NOT NULL COMMENT '角色编码',
    description VARCHAR(200) COMMENT '描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '角色表';

INSERT INTO sys_role (name, code, description) VALUES
('采集员', 'COLLECTOR', '现场采集问题'),
('受理员', 'RECEIVER', '受理登记案件'),
('派遣员', 'DISPATCHER', '派遣案件到处置部门'),
('处置部门', 'HANDLER', '接收并处置案件');

-- 用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码（加密）',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    phone VARCHAR(20) COMMENT '电话',
    department_id BIGINT COMMENT '所属部门',
    role_id BIGINT COMMENT '角色',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '用户表';

-- ============================================
-- 2. 立结案标准
-- ============================================

-- 大类表
CREATE TABLE standard_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(10) NOT NULL COMMENT '大类代码',
    name VARCHAR(50) NOT NULL COMMENT '大类名称',
    type VARCHAR(20) NOT NULL COMMENT '类型：部件/事件/服务事项',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '大类表';

-- 小类表
CREATE TABLE standard_subcategory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL COMMENT '大类ID',
    code VARCHAR(10) NOT NULL COMMENT '小类代码',
    name VARCHAR(50) NOT NULL COMMENT '小类名称',
    supervisor VARCHAR(500) COMMENT '监管主体',
    responsible VARCHAR(500) COMMENT '责任主体',
    law_basis TEXT COMMENT '法律法规依据',
    law条款 TEXT COMMENT '法律法规具体条款',
    collect_require TEXT COMMENT '采集要求',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '小类表';

-- 立案条件表
CREATE TABLE standard_condition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subcategory_id BIGINT NOT NULL COMMENT '小类ID',
    condition_desc VARCHAR(500) NOT NULL COMMENT '立案条件描述',
    deadline VARCHAR(50) NOT NULL COMMENT '处置时限',
    deadline_type VARCHAR(20) COMMENT '时限类型：紧急工作时/工作时/工作日',
    close_condition VARCHAR(200) COMMENT '结案条件',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '立案条件表';

-- ============================================
-- 3. 案件流程
-- ============================================

-- 案件主表
CREATE TABLE case_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_no VARCHAR(30) NOT NULL COMMENT '案件编号',
    source VARCHAR(20) NOT NULL COMMENT '来源：采集员/12345/市民投诉/领导交办/视频监控',
    level VARCHAR(20) DEFAULT 'normal' COMMENT '等级：日常/一般/严重/重大',
    category_type VARCHAR(20) COMMENT '类型：部件/事件/服务事项',
    category_id BIGINT COMMENT '大类',
    subcategory_id BIGINT COMMENT '小类',
    condition_id BIGINT COMMENT '立案条件',
    description TEXT COMMENT '问题描述',
    location VARCHAR(200) COMMENT '位置描述',
    longitude DECIMAL(10,7) COMMENT '经度',
    latitude DECIMAL(10,7) COMMENT '纬度',
    area VARCHAR(50) COMMENT '区域',
    street VARCHAR(50) COMMENT '街道',
    community VARCHAR(50) COMMENT '社区',
    deadline DATETIME COMMENT '处置截止时间',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态',
    reporter_id BIGINT COMMENT '上报人/登记人',
    reporter_phone VARCHAR(20) COMMENT '举报人电话',
    reporter_name VARCHAR(50) COMMENT '举报人姓名',
    is_public TINYINT DEFAULT 0 COMMENT '是否公开举报人',
    is_callback TINYINT DEFAULT 0 COMMENT '是否回访',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '案件主表';

-- 案件状态枚举说明：
-- pending: 待查（无照片）
-- filing: 待立案/待批转
-- dispatched: 已派遣
-- handling: 处置中
-- verifying: 待核实
-- closed: 已结案
-- rejected: 已作废

-- 案件流程记录
CREATE TABLE case_process (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id BIGINT NOT NULL COMMENT '案件ID',
    action VARCHAR(30) NOT NULL COMMENT '动作：登记/立案/派遣/接收/处置/核实/结案/作废',
    operator_id BIGINT COMMENT '操作人',
    operator_name VARCHAR(50) COMMENT '操作人姓名',
    from_status VARCHAR(20) COMMENT '原状态',
    to_status VARCHAR(20) COMMENT '新状态',
    remark TEXT COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '案件流程记录';

-- 案件附件
CREATE TABLE case_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id BIGINT NOT NULL COMMENT '案件ID',
    type VARCHAR(20) COMMENT '类型：照片/视频/录音',
    url VARCHAR(500) NOT NULL COMMENT '文件路径',
    description VARCHAR(200) COMMENT '描述',
    upload_time DATETIME COMMENT '上传时间',
    uploader_id BIGINT COMMENT '上传人',
    stage VARCHAR(20) COMMENT '阶段：上报/处置/核实',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '案件附件';

-- ============================================
-- 4. 任务
-- ============================================

-- 核查任务（确认问题是否存在）
CREATE TABLE task_verify (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id BIGINT NOT NULL COMMENT '案件ID',
    collector_id BIGINT COMMENT '采集员',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending/completed',
    result VARCHAR(20) COMMENT '结果：exists/not_exists',
    description TEXT COMMENT '核查说明',
    completed_at DATETIME COMMENT '完成时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '核查任务表';

-- 核实任务（确认处置是否到位）
CREATE TABLE task_check (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id BIGINT NOT NULL COMMENT '案件ID',
    collector_id BIGINT COMMENT '采集员',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending/completed',
    result VARCHAR(20) COMMENT '结果：ok/not_ok',
    description TEXT COMMENT '核实说明',
    completed_at DATETIME COMMENT '完成时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '核实任务表';

-- ============================================
-- 5. 通知公告
-- ============================================

-- 今日提示
CREATE TABLE notice_tip (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL COMMENT '标题',
    content TEXT COMMENT '内容',
    publisher_id BIGINT COMMENT '发布人',
    expire_date DATE COMMENT '过期日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '今日提示';

-- 公文通告
CREATE TABLE notice_official (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL COMMENT '标题',
    content TEXT COMMENT '内容',
    type VARCHAR(20) COMMENT '类型：通知/公告',
    publisher_id BIGINT COMMENT '发布人',
    publish_time DATETIME COMMENT '发布时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '公文通告';

-- ============================================
-- 初始化数据
-- ============================================

-- 示例部门
INSERT INTO sys_department (name, type) VALUES
('运城市城市管理局', '监管'),
('运城市市政公用服务中心', '责任'),
('运城市园林绿化服务中心', '责任'),
('运城市市容环卫中心', '责任');

-- 示例用户
INSERT INTO sys_user (username, password, name, role_id, department_id) VALUES
('collector01', '$2a$10$test', '张三', 1, 1),
('receiver01', '$2a$10$test', '李四', 2, 1);

-- 初始化大类（部分示例）
INSERT INTO standard_category (code, name, type, sort_order) VALUES
('01', '公用设施', '部件', 1),
('02', '道路交通设施', '部件', 2),
('03', '市容环境设施', '部件', 3),
('04', '园林绿化设施', '部件', 4),
('05', '其他部件', '部件', 5),
('01', '市容环境', '事件', 10),
('02', '宣传广告', '事件', 11),
('03', '施工管理', '事件', 12),
('04', '突发事件', '事件', 13),
('05', '街面秩序', '事件', 14),
('06', '扩展事件', '事件', 15),
('01', '服务事项', '服务事项', 20);
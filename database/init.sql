-- 智慧城管系统数据库初始化脚本
-- 运城市城市综合管理服务系统
-- 55张表完整结构

-- 创建数据库
CREATE DATABASE IF NOT EXISTS cityguard DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cityguard;

-- ============================================
-- 一、组织架构相关表 (4张)
-- ============================================

-- 1.1 部门表
CREATE TABLE department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    dept_code VARCHAR(20) NOT NULL COMMENT '部门编码',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '上级部门ID',
    dept_level INT NOT NULL COMMENT '部门层级：1=市级，2=二级部门，3=三级单位',
    dept_type VARCHAR(20) NOT NULL COMMENT '部门类型：dispatch=处置部门, manage=管理部门, supervise=监督中心',
    leader_name VARCHAR(50) COMMENT '负责人姓名',
    leader_phone VARCHAR(20) COMMENT '负责人电话',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态：1=正常，0=停用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_dept_code (dept_code)
) COMMENT '部门表';

-- 1.2 岗位表
CREATE TABLE position (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    position_code VARCHAR(20) NOT NULL COMMENT '岗位编码',
    position_name VARCHAR(50) NOT NULL COMMENT '岗位名称',
    position_type VARCHAR(20) NOT NULL COMMENT '岗位类型：collector=采集员, acceptor=受理员, dispatcher=派遣员, supervisor=值班长, leader=领导, handler=处置人员',
    description VARCHAR(200) COMMENT '岗位描述',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_position_code (position_code)
) COMMENT '岗位表';

-- 1.3 用户表
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_code VARCHAR(20) NOT NULL COMMENT '用户编码/工号',
    user_name VARCHAR(50) NOT NULL COMMENT '用户姓名',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    password VARCHAR(100) NOT NULL COMMENT '密码（加密存储）',
    dept_id BIGINT NOT NULL COMMENT '所属部门ID',
    position_id BIGINT COMMENT '主岗位ID',
    gender TINYINT COMMENT '性别：1=男，2=女',
    avatar VARCHAR(200) COMMENT '头像URL',
    email VARCHAR(50) COMMENT '邮箱',
    id_card VARCHAR(20) COMMENT '身份证号',
    status TINYINT DEFAULT 1 COMMENT '状态：1=正常，0=停用，2=离职',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_user_code (user_code),
    UNIQUE KEY uk_phone (phone)
) COMMENT '用户表';

-- 1.4 用户岗位关联表
CREATE TABLE user_position (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    position_id BIGINT NOT NULL COMMENT '岗位ID',
    is_primary TINYINT DEFAULT 0 COMMENT '是否主岗位：0=否，1=是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_position (user_id, position_id)
) COMMENT '用户岗位关联表';

-- ============================================
-- 二、权限相关表 (4张)
-- ============================================

-- 2.1 角色表
CREATE TABLE role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_code VARCHAR(20) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(200) COMMENT '角色描述',
    is_system TINYINT DEFAULT 0 COMMENT '是否系统内置',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_role_code (role_code)
) COMMENT '角色表';

-- 2.2 角色用户关联表
CREATE TABLE role_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_user (role_id, user_id)
) COMMENT '角色用户关联表';

-- 2.3 菜单表
CREATE TABLE menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    menu_code VARCHAR(50) NOT NULL COMMENT '菜单编码',
    menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    parent_id BIGINT DEFAULT 0 COMMENT '上级菜单ID',
    menu_type TINYINT NOT NULL COMMENT '菜单类型：1=目录，2=菜单，3=按钮',
    path VARCHAR(100) COMMENT '路由路径',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0,
    visible TINYINT DEFAULT 1 COMMENT '是否可见',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_menu_code (menu_code)
) COMMENT '菜单表';

-- 2.4 角色菜单关联表
CREATE TABLE role_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) COMMENT '角色菜单关联表';

-- ============================================
-- 三、网格和地理编码相关表 (6张)
-- ============================================

-- 3.1 街道表
CREATE TABLE street (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    street_code VARCHAR(10) NOT NULL COMMENT '街道编码（4位）',
    street_name VARCHAR(100) NOT NULL COMMENT '街道名称',
    area_code VARCHAR(6) NOT NULL COMMENT '所属市区编码',
    area_name VARCHAR(100) COMMENT '所属市区名称',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_street_code (street_code)
) COMMENT '街道表';

-- 3.2 社区表
CREATE TABLE community (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    community_code VARCHAR(10) NOT NULL COMMENT '社区编码（3位）',
    community_name VARCHAR(100) NOT NULL COMMENT '社区名称',
    street_id BIGINT NOT NULL COMMENT '所属街道ID',
    street_code VARCHAR(10) NOT NULL COMMENT '所属街道编码',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_community_code (street_code, community_code)
) COMMENT '社区表';

-- 3.3 单元网格表
CREATE TABLE grid (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    grid_code VARCHAR(16) NOT NULL COMMENT '网格编码（16位）',
    grid_name VARCHAR(100) COMMENT '网格名称',
    community_id BIGINT NOT NULL COMMENT '所属社区ID',
    street_id BIGINT NOT NULL COMMENT '所属街道ID',
    area_code VARCHAR(6) NOT NULL COMMENT '所属市区编码',
    area DECIMAL(10,2) COMMENT '网格面积（平方米）',
    boundary JSON COMMENT '网格边界',
    center_lng DECIMAL(10,6) COMMENT '中心经度',
    center_lat DECIMAL(10,6) COMMENT '中心纬度',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_grid_code (grid_code)
) COMMENT '单元网格表';

-- 3.4 责任网格表
CREATE TABLE responsibility_grid (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    resp_grid_code VARCHAR(20) NOT NULL COMMENT '责任网格编码',
    resp_grid_name VARCHAR(100) NOT NULL COMMENT '责任网格名称',
    user_id BIGINT COMMENT '绑定的采集员ID',
    grid_ids VARCHAR(500) COMMENT '包含的单元网格ID列表',
    area DECIMAL(10,2) COMMENT '责任网格总面积',
    boundary JSON COMMENT '责任网格边界',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_resp_grid_code (resp_grid_code)
) COMMENT '责任网格表';

-- 3.5 责任网格单元关联表
CREATE TABLE responsibility_grid_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    resp_grid_id BIGINT NOT NULL COMMENT '责任网格ID',
    grid_id BIGINT NOT NULL COMMENT '单元网格ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_resp_grid_detail (resp_grid_id, grid_id)
) COMMENT '责任网格单元关联表';

-- 3.6 地理编码表
CREATE TABLE geo_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    geo_type VARCHAR(20) NOT NULL COMMENT '地址类型：street=街巷，community=小区，building=建筑物，poi=兴趣点，doorplate=门牌',
    geo_name VARCHAR(100) NOT NULL COMMENT '地址名称',
    geo_code VARCHAR(50) COMMENT '地址编码',
    parent_id BIGINT DEFAULT 0 COMMENT '上级地址ID',
    full_address VARCHAR(200) COMMENT '完整地址描述',
    longitude DECIMAL(10,6) COMMENT '经度',
    latitude DECIMAL(10,6) COMMENT '纬度',
    grid_id BIGINT COMMENT '所属单元网格ID',
    street_id BIGINT COMMENT '所属街道ID',
    community_id BIGINT COMMENT '所属社区ID',
    remark VARCHAR(200) COMMENT '备注',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0
) COMMENT '地理编码表';

-- ============================================
-- 四、立结案标准相关表 (5张)
-- ============================================

-- 4.1 问题大类表
CREATE TABLE category_big (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    big_code VARCHAR(2) NOT NULL COMMENT '大类编码（2位）',
    big_name VARCHAR(50) NOT NULL COMMENT '大类名称',
    category_type VARCHAR(10) NOT NULL COMMENT '类型：component=部件，event=事件',
    description VARCHAR(200) COMMENT '大类描述',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_big_code_type (big_code, category_type)
) COMMENT '问题大类表';

-- 4.2 问题小类表
CREATE TABLE category_small (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    small_code VARCHAR(2) NOT NULL COMMENT '小类编码（2位）',
    small_name VARCHAR(100) NOT NULL COMMENT '小类名称',
    big_id BIGINT NOT NULL COMMENT '所属大类ID',
    big_code VARCHAR(2) NOT NULL COMMENT '所属大类编码',
    category_type VARCHAR(10) NOT NULL COMMENT '类型',
    full_code VARCHAR(4) NOT NULL COMMENT '完整编码',
    supervise_subject VARCHAR(200) COMMENT '监管主体',
    responsibility_subject VARCHAR(200) COMMENT '责任主体',
    legal_basis VARCHAR(500) COMMENT '法律法规依据',
    collect_requirement VARCHAR(500) COMMENT '采集要求',
    description VARCHAR(200) COMMENT '小类描述',
    is_extended TINYINT DEFAULT 0 COMMENT '是否扩展小类',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_small_code_type (big_code, small_code, category_type)
) COMMENT '问题小类表';

-- 4.3 立结案标准表
CREATE TABLE case_standard (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    standard_code VARCHAR(10) NOT NULL COMMENT '立案条件编码',
    small_id BIGINT NOT NULL COMMENT '所属小类ID',
    big_code VARCHAR(2) NOT NULL COMMENT '所属大类编码',
    small_code VARCHAR(2) NOT NULL COMMENT '所属小类编码',
    category_type VARCHAR(10) NOT NULL COMMENT '类型',
    condition_desc VARCHAR(500) NOT NULL COMMENT '立案条件描述',
    handle_time_limit VARCHAR(20) NOT NULL COMMENT '处置时限格式',
    handle_time_value INT NOT NULL COMMENT '处置时限数值',
    handle_time_type VARCHAR(20) NOT NULL COMMENT '时限类型：urgent_hour,work_hour,work_day,natural_day',
    close_condition VARCHAR(200) NOT NULL COMMENT '结案条件',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_standard_code (standard_code)
) COMMENT '立结案标准表';

-- 4.4 权责配置表
CREATE TABLE responsibility_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    small_id BIGINT NOT NULL COMMENT '小类ID',
    standard_id BIGINT COMMENT '立案条件ID',
    dept_id BIGINT NOT NULL COMMENT '处置部门ID',
    dept_level INT COMMENT '部门层级要求',
    is_primary TINYINT DEFAULT 1 COMMENT '是否主处置部门',
    priority INT DEFAULT 0 COMMENT '派遣优先级',
    remark VARCHAR(200) COMMENT '备注说明',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0
) COMMENT '权责配置表';

-- 4.5 小类扩展属性表
CREATE TABLE category_extend_field (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    small_id BIGINT NOT NULL COMMENT '小类ID',
    field_name VARCHAR(50) NOT NULL COMMENT '字段名称',
    field_label VARCHAR(50) NOT NULL COMMENT '字段显示名称',
    field_type VARCHAR(20) NOT NULL COMMENT '字段类型：text,number,select,date',
    field_options VARCHAR(500) COMMENT '下拉选项JSON',
    is_required TINYINT DEFAULT 0 COMMENT '是否必填',
    default_value VARCHAR(100) COMMENT '默认值',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0
) COMMENT '小类扩展属性表';

-- ============================================
-- 五、案件流转核心表 (4张)
-- ============================================

-- 5.1 案件主表
CREATE TABLE case_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    case_code VARCHAR(19) NOT NULL COMMENT '案件编码（19位国标编码）',

    -- 问题分类
    category_type VARCHAR(10) NOT NULL COMMENT '类型：component=部件，event=事件',
    big_code VARCHAR(2) NOT NULL COMMENT '大类编码',
    big_name VARCHAR(50) NOT NULL COMMENT '大类名称',
    small_code VARCHAR(2) NOT NULL COMMENT '小类编码',
    small_name VARCHAR(100) NOT NULL COMMENT '小类名称',
    small_id BIGINT NOT NULL COMMENT '小类ID',
    standard_id BIGINT COMMENT '立案条件ID',
    condition_desc VARCHAR(500) COMMENT '立案条件描述',

    -- 来源信息
    source_type VARCHAR(20) NOT NULL COMMENT '来源类型：collector=采集员上报，register=案件登记，leader=领导批示，transfer=部门批转',
    source_desc VARCHAR(50) COMMENT '来源描述',

    -- 上报人信息
    reporter_id BIGINT COMMENT '上报人ID',
    reporter_name VARCHAR(50) COMMENT '上报人姓名',
    reporter_phone VARCHAR(20) COMMENT '上报人联系电话',

    -- 位置信息
    longitude DECIMAL(10,6) COMMENT '经度',
    latitude DECIMAL(10,6) COMMENT '纬度',
    address VARCHAR(200) COMMENT '地址描述',
    street_id BIGINT COMMENT '所属街道ID',
    community_id BIGINT COMMENT '所属社区ID',
    grid_id BIGINT COMMENT '所属单元网格ID',
    resp_grid_id BIGINT COMMENT '所属责任网格ID',

    -- 案件描述
    description VARCHAR(500) NOT NULL COMMENT '问题描述',
    remark VARCHAR(200) COMMENT '备注',

    -- 案件状态
    case_status VARCHAR(20) NOT NULL COMMENT '案件状态',
    current_node VARCHAR(20) COMMENT '当前流转节点',
    current_handler_id BIGINT COMMENT '当前处理人ID',
    current_handler_name VARCHAR(50) COMMENT '当前处理人姓名',
    current_dept_id BIGINT COMMENT '当前处理部门ID',
    current_dept_name VARCHAR(100) COMMENT '当前处理部门名称',

    -- 处置部门信息
    handle_dept_id BIGINT COMMENT '处置部门ID',
    handle_dept_name VARCHAR(100) COMMENT '处置部门名称',

    -- 时限信息
    time_limit_type VARCHAR(20) COMMENT '时限类型',
    time_limit_value INT COMMENT '时限数值',
    deadline_time DATETIME COMMENT '处置截止时间',

    -- 时间节点
    report_time DATETIME COMMENT '上报时间',
    accept_time DATETIME COMMENT '立案时间',
    dispatch_time DATETIME COMMENT '派遣时间',
    handle_receive_time DATETIME COMMENT '处置部门接收时间',
    handle_finish_time DATETIME COMMENT '处置完成时间',
    check_time DATETIME COMMENT '核查完成时间',
    close_time DATETIME COMMENT '结案时间',

    -- 标记
    is_urgent TINYINT DEFAULT 0 COMMENT '是否紧急',
    is_supervised TINYINT DEFAULT 0 COMMENT '是否督办',
    is_similar TINYINT DEFAULT 0 COMMENT '是否相似案件',
    is_typical TINYINT DEFAULT 0 COMMENT '是否典型案件',
    is_forced_close TINYINT DEFAULT 0 COMMENT '是否强制结案',

    -- 回访信息
    need_visit TINYINT DEFAULT 0 COMMENT '是否需要回访',
    visit_status VARCHAR(20) COMMENT '回访状态',
    visit_result VARCHAR(50) COMMENT '回访结果',
    visit_satisfaction TINYINT COMMENT '满意度',

    -- 核查信息
    verify_task_id BIGINT COMMENT '核实任务ID',
    check_task_id BIGINT COMMENT '核查任务ID',

    -- 申诉信息
    appeal_status VARCHAR(20) COMMENT '申诉状态',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_case_code (case_code),
    INDEX idx_case_status (case_status),
    INDEX idx_small_id (small_id),
    INDEX idx_handle_dept (handle_dept_id),
    INDEX idx_report_time (report_time)
) COMMENT '案件主表';

-- 5.2 案件流转记录表
CREATE TABLE case_flow_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    case_id BIGINT NOT NULL COMMENT '案件ID',
    case_code VARCHAR(19) NOT NULL COMMENT '案件编码',

    -- 流转节点
    node_code VARCHAR(20) NOT NULL COMMENT '节点编码',
    node_name VARCHAR(50) NOT NULL COMMENT '节点名称',

    -- 操作信息
    operate_type VARCHAR(20) NOT NULL COMMENT '操作类型',
    operate_result VARCHAR(20) NOT NULL COMMENT '操作结果',
    operate_opinion VARCHAR(500) COMMENT '操作意见',

    -- 操作人信息
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) NOT NULL COMMENT '操作人姓名',
    operator_phone VARCHAR(20) COMMENT '操作人电话',
    operator_dept_id BIGINT COMMENT '操作人部门ID',
    operator_dept_name VARCHAR(100) COMMENT '操作人部门名称',
    operator_position VARCHAR(50) COMMENT '操作人岗位',

    -- 接收人信息
    receiver_id BIGINT COMMENT '接收人ID',
    receiver_name VARCHAR(50) COMMENT '接收人姓名',
    receiver_dept_id BIGINT COMMENT '接收人部门ID',
    receiver_dept_name VARCHAR(100) COMMENT '接收人部门名称',

    -- 时间信息
    operate_time DATETIME NOT NULL COMMENT '操作时间',
    receive_time DATETIME COMMENT '接收时间',
    time_used INT COMMENT '用时（秒）',
    is_timeout TINYINT DEFAULT 0 COMMENT '是否超时',

    -- 附件
    attachments VARCHAR(500) COMMENT '附件ID列表',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_case_id (case_id),
    INDEX idx_node_code (node_code),
    INDEX idx_operator (operator_id)
) COMMENT '案件流转记录表';

-- 5.3 案件附件表
CREATE TABLE case_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    case_id BIGINT NOT NULL COMMENT '案件ID',
    flow_record_id BIGINT COMMENT '关联流转记录ID',

    -- 附件信息
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型：image=图片，video=视频，audio=音频',
    file_name VARCHAR(100) NOT NULL COMMENT '文件名称',
    file_path VARCHAR(200) NOT NULL COMMENT '文件存储路径',
    file_size INT COMMENT '文件大小（字节）',
    file_ext VARCHAR(10) COMMENT '文件扩展名',

    -- 图片特有字段
    photo_type VARCHAR(20) COMMENT '照片类型：near=近景，far=远景，reference=参照物，handle_before=处置前，handle_after=处置后',
    longitude DECIMAL(10,6) COMMENT '拍摄经度',
    latitude DECIMAL(10,6) COMMENT '拍摄纬度',
    shoot_time DATETIME COMMENT '拍摄时间',

    -- 上传人
    uploader_id BIGINT NOT NULL COMMENT '上传人ID',
    uploader_name VARCHAR(50) COMMENT '上传人姓名',

    -- 关联节点
    node_code VARCHAR(20) COMMENT '关联节点',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    INDEX idx_case_id (case_id),
    INDEX idx_flow_record (flow_record_id)
) COMMENT '案件附件表';

-- 5.4 案件扩展属性值表
CREATE TABLE case_extend_value (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    case_id BIGINT NOT NULL COMMENT '案件ID',
    field_id BIGINT NOT NULL COMMENT '扩展属性字段ID',
    field_name VARCHAR(50) NOT NULL COMMENT '字段名称',
    field_label VARCHAR(50) NOT NULL COMMENT '字段显示名称',
    field_value VARCHAR(200) COMMENT '字段值',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_case_field (case_id, field_id),
    INDEX idx_case_id (case_id)
) COMMENT '案件扩展属性值表';

-- ============================================
-- 六、核实核查任务表 (4张)
-- ============================================

-- 6.1 核实任务表
CREATE TABLE verify_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_code VARCHAR(20) NOT NULL COMMENT '任务编码',

    -- 关联案件
    case_id BIGINT COMMENT '关联案件ID',
    case_code VARCHAR(19) COMMENT '关联案件编码',

    -- 核实来源
    source_type VARCHAR(20) NOT NULL COMMENT '来源类型：public=公众举报，video=视频上报，leader=领导批示',
    source_desc VARCHAR(200) COMMENT '来源描述',
    reporter_name VARCHAR(50) COMMENT '举报人姓名',
    reporter_phone VARCHAR(20) COMMENT '举报人电话',

    -- 问题信息
    big_code VARCHAR(2) COMMENT '预判大类编码',
    big_name VARCHAR(50) COMMENT '预判大类名称',
    small_code VARCHAR(2) COMMENT '预判小类编码',
    small_name VARCHAR(100) COMMENT '预判小类名称',
    description VARCHAR(500) COMMENT '问题描述',

    -- 位置信息
    longitude DECIMAL(10,6) COMMENT '经度',
    latitude DECIMAL(10,6) COMMENT '纬度',
    address VARCHAR(200) COMMENT '地址描述',
    street_id BIGINT COMMENT '所属街道ID',
    community_id BIGINT COMMENT '所属社区ID',
    grid_id BIGINT COMMENT '所属单元网格ID',
    resp_grid_id BIGINT COMMENT '所属责任网格ID',

    -- 任务信息
    task_status VARCHAR(20) NOT NULL COMMENT '任务状态',
    assign_time DATETIME COMMENT '指派时间',
    finish_time DATETIME COMMENT '完成时间',
    deadline_time DATETIME COMMENT '截止时间',

    -- 指派信息
    collector_id BIGINT COMMENT '指派采集员ID',
    collector_name VARCHAR(50) COMMENT '指派采集员姓名',
    collector_phone VARCHAR(20) COMMENT '指派采集员电话',

    -- 核实结果
    verify_result VARCHAR(20) COMMENT '核实结果：exist=问题存在，not_exist=问题不存在，unable=无法核实',
    verify_opinion VARCHAR(500) COMMENT '核实意见',

    -- 创建人信息
    creator_id BIGINT NOT NULL COMMENT '创建人ID',
    creator_name VARCHAR(50) COMMENT '创建人姓名',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_task_code (task_code),
    INDEX idx_case_id (case_id),
    INDEX idx_collector (collector_id),
    INDEX idx_task_status (task_status)
) COMMENT '核实任务表';

-- 6.2 核实任务附件表
CREATE TABLE verify_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    verify_task_id BIGINT NOT NULL COMMENT '核实任务ID',

    file_type VARCHAR(20) NOT NULL COMMENT '文件类型',
    file_name VARCHAR(100) NOT NULL COMMENT '文件名称',
    file_path VARCHAR(200) NOT NULL COMMENT '文件存储路径',
    file_size INT COMMENT '文件大小（字节）',

    photo_type VARCHAR(20) COMMENT '照片类型',
    longitude DECIMAL(10,6) COMMENT '拍摄经度',
    latitude DECIMAL(10,6) COMMENT '拍摄纬度',
    shoot_time DATETIME COMMENT '拍摄时间',

    uploader_id BIGINT NOT NULL COMMENT '上传人ID',
    uploader_name VARCHAR(50) COMMENT '上传人姓名',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    INDEX idx_verify_task (verify_task_id)
) COMMENT '核实任务附件表';

-- 6.3 核查任务表
CREATE TABLE check_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_code VARCHAR(20) NOT NULL COMMENT '任务编码',

    -- 关联案件
    case_id BIGINT NOT NULL COMMENT '关联案件ID',
    case_code VARCHAR(19) NOT NULL COMMENT '关联案件编码',

    -- 案件信息摘要
    small_name VARCHAR(100) COMMENT '小类名称',
    address VARCHAR(200) COMMENT '地址描述',
    longitude DECIMAL(10,6) COMMENT '经度',
    latitude DECIMAL(10,6) COMMENT '纬度',

    -- 处置信息
    handle_dept_id BIGINT COMMENT '处置部门ID',
    handle_dept_name VARCHAR(100) COMMENT '处置部门名称',
    handle_opinion VARCHAR(500) COMMENT '处置意见',
    handle_finish_time DATETIME COMMENT '处置完成时间',

    -- 任务信息
    task_status VARCHAR(20) NOT NULL COMMENT '任务状态',
    assign_time DATETIME COMMENT '指派时间',
    finish_time DATETIME COMMENT '完成时间',
    deadline_time DATETIME COMMENT '截止时间',

    -- 同时段核查
    is_same_period TINYINT DEFAULT 0 COMMENT '是否同时段核查',
    same_period_time DATETIME COMMENT '同时段核查时间',

    -- 指派信息
    collector_id BIGINT COMMENT '指派采集员ID',
    collector_name VARCHAR(50) COMMENT '指派采集员姓名',
    collector_phone VARCHAR(20) COMMENT '指派采集员电话',

    -- 核查结果
    check_result VARCHAR(20) COMMENT '核查结果：pass=核查通过，not_pass=核查不通过，unable=无法核查',
    check_opinion VARCHAR(500) COMMENT '核查意见',

    -- 指派人信息
    assigner_id BIGINT COMMENT '指派人ID',
    assigner_name VARCHAR(50) COMMENT '指派人姓名',

    -- 返工次数
    rework_count INT DEFAULT 0 COMMENT '返工次数',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_task_code (task_code),
    INDEX idx_case_id (case_id),
    INDEX idx_collector (collector_id),
    INDEX idx_task_status (task_status)
) COMMENT '核查任务表';

-- 6.4 核查任务附件表
CREATE TABLE check_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    check_task_id BIGINT NOT NULL COMMENT '核查任务ID',

    file_type VARCHAR(20) NOT NULL COMMENT '文件类型',
    file_name VARCHAR(100) NOT NULL COMMENT '文件名称',
    file_path VARCHAR(200) NOT NULL COMMENT '文件存储路径',
    file_size INT COMMENT '文件大小（字节）',

    photo_type VARCHAR(20) COMMENT '照片类型',
    longitude DECIMAL(10,6) COMMENT '拍摄经度',
    latitude DECIMAL(10,6) COMMENT '拍摄纬度',
    shoot_time DATETIME COMMENT '拍摄时间',

    uploader_id BIGINT NOT NULL COMMENT '上传人ID',
    uploader_name VARCHAR(50) COMMENT '上传人姓名',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    INDEX idx_check_task (check_task_id)
) COMMENT '核查任务附件表';

-- ============================================
-- 七、申诉申报相关表 (3张)
-- ============================================

-- 7.1 申诉申报申请表
CREATE TABLE appeal_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    appeal_code VARCHAR(20) NOT NULL COMMENT '申诉申报编码',

    case_id BIGINT COMMENT '关联案件ID',
    case_code VARCHAR(19) COMMENT '关联案件编码',

    -- 申请类型
    apply_type VARCHAR(20) NOT NULL COMMENT '申请类型：appeal=申诉，declare=申报',

    -- 申诉类型
    appeal_type VARCHAR(50) COMMENT '申诉情形',
    appeal_desc VARCHAR(500) COMMENT '申诉说明',

    -- 申报类型
    declare_type VARCHAR(50) COMMENT '申报类型',
    declare_desc VARCHAR(500) COMMENT '申报说明',

    -- 扣分项信息
    deduction_type VARCHAR(50) COMMENT '扣分项类型',
    deduction_value DECIMAL(5,2) COMMENT '扣分值',
    deduction_time DATETIME COMMENT '扣分产生时间',

    -- 申请部门信息
    apply_dept_id BIGINT NOT NULL COMMENT '申请部门ID',
    apply_dept_name VARCHAR(100) COMMENT '申请部门名称',
    apply_user_id BIGINT NOT NULL COMMENT '申请人ID',
    apply_user_name VARCHAR(50) COMMENT '申请人姓名',
    apply_user_phone VARCHAR(20) COMMENT '申请人电话',

    apply_time DATETIME NOT NULL COMMENT '申请时间',
    deadline_time DATETIME COMMENT '申诉时限',

    -- 审核状态
    appeal_status VARCHAR(20) NOT NULL COMMENT '审核状态',

    -- 审核结果
    final_result VARCHAR(20) COMMENT '最终结果',
    final_opinion VARCHAR(500) COMMENT '最终审核意见',
    adjust_value DECIMAL(5,2) COMMENT '调整分值',

    attachments VARCHAR(500) COMMENT '附件ID列表',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_appeal_code (appeal_code),
    INDEX idx_case_id (case_id),
    INDEX idx_apply_dept (apply_dept_id),
    INDEX idx_appeal_status (appeal_status)
) COMMENT '申诉申报申请表';

-- 7.2 申诉申报审核记录表
CREATE TABLE appeal_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    appeal_id BIGINT NOT NULL COMMENT '申诉申报申请ID',
    appeal_code VARCHAR(20) NOT NULL COMMENT '申诉申报编码',

    -- 审核节点
    review_node VARCHAR(20) NOT NULL COMMENT '审核节点：pre_check=预审核，dept_review=部门审核，leader_review=领导审核',
    review_node_name VARCHAR(50) NOT NULL COMMENT '审核节点名称',

    -- 审核信息
    review_result VARCHAR(20) NOT NULL COMMENT '审核结果',
    review_opinion VARCHAR(500) COMMENT '审核意见',

    -- 审核人信息
    reviewer_id BIGINT NOT NULL COMMENT '审核人ID',
    reviewer_name VARCHAR(50) NOT NULL COMMENT '审核人姓名',
    reviewer_dept_id BIGINT COMMENT '审核人部门ID',
    reviewer_dept_name VARCHAR(100) COMMENT '审核人部门名称',
    reviewer_position VARCHAR(50) COMMENT '审核人岗位',

    review_time DATETIME NOT NULL COMMENT '审核时间',
    time_used INT COMMENT '审核用时（秒）',

    attachments VARCHAR(500) COMMENT '附件ID列表',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_appeal_id (appeal_id),
    INDEX idx_review_node (review_node)
) COMMENT '申诉申报审核记录表';

-- 7.3 申诉申报附件表
CREATE TABLE appeal_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    appeal_id BIGINT NOT NULL COMMENT '申诉申报申请ID',
    review_id BIGINT COMMENT '关联审核记录ID',

    file_type VARCHAR(20) NOT NULL COMMENT '文件类型',
    file_name VARCHAR(100) NOT NULL COMMENT '文件名称',
    file_path VARCHAR(200) NOT NULL COMMENT '文件存储路径',
    file_size INT COMMENT '文件大小（字节）',
    file_ext VARCHAR(10) COMMENT '文件扩展名',

    use_type VARCHAR(20) COMMENT '用途：apply=申请附件，review=审核附件',

    uploader_id BIGINT NOT NULL COMMENT '上传人ID',
    uploader_name VARCHAR(50) COMMENT '上传人姓名',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    INDEX idx_appeal_id (appeal_id),
    INDEX idx_review_id (review_id)
) COMMENT '申诉申报附件表';

-- ============================================
-- 八、计时管理相关表 (4张)
-- ============================================

-- 8.1 工作时段配置表
CREATE TABLE work_time_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_name VARCHAR(50) NOT NULL COMMENT '配置名称',

    am_start_time VARCHAR(10) NOT NULL COMMENT '上午开始时间',
    am_end_time VARCHAR(10) NOT NULL COMMENT '上午结束时间',
    pm_start_time VARCHAR(10) NOT NULL COMMENT '下午开始时间',
    pm_end_time VARCHAR(10) NOT NULL COMMENT '下午结束时间',

    is_default TINYINT DEFAULT 1 COMMENT '是否默认配置',
    status TINYINT DEFAULT 1 COMMENT '状态',
    remark VARCHAR(200) COMMENT '备注说明',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0
) COMMENT '工作时段配置表';

-- 8.2 节假日配置表
CREATE TABLE holiday_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    year INT NOT NULL COMMENT '年份',

    holiday_date DATE NOT NULL COMMENT '日期',
    holiday_type VARCHAR(20) NOT NULL COMMENT '类型：holiday=法定节假日，workday=调休工作日',
    holiday_name VARCHAR(50) COMMENT '节假日名称',

    remark VARCHAR(100) COMMENT '备注说明',
    source VARCHAR(20) COMMENT '来源',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_year_date (year, holiday_date),
    INDEX idx_holiday_date (holiday_date),
    INDEX idx_holiday_type (holiday_type)
) COMMENT '节假日配置表';

-- 8.3 时限计算规则表
CREATE TABLE time_limit_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    time_limit_type VARCHAR(20) NOT NULL COMMENT '时限类型',
    type_name VARCHAR(50) NOT NULL COMMENT '类型名称',

    is_continuous TINYINT NOT NULL COMMENT '是否连续计算',
    include_holiday TINYINT NOT NULL COMMENT '是否包含节假日',
    include_weekend TINYINT NOT NULL COMMENT '是否包含周末',
    use_work_time_config TINYINT DEFAULT 1 COMMENT '是否使用工作时段配置',

    calc_desc VARCHAR(200) COMMENT '计算方式描述',

    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_time_limit_type (time_limit_type)
) COMMENT '时限计算规则表';

-- 8.4 案件计时记录表
CREATE TABLE case_timer_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    case_id BIGINT NOT NULL COMMENT '案件ID',
    case_code VARCHAR(19) NOT NULL COMMENT '案件编码',

    timer_stage VARCHAR(20) NOT NULL COMMENT '计时阶段：dispatch=派遣阶段，handle=处置阶段，check=核查阶段，total=全流程',
    stage_name VARCHAR(50) NOT NULL COMMENT '阶段名称',

    time_limit_type VARCHAR(20) NOT NULL COMMENT '时限类型',
    time_limit_value INT NOT NULL COMMENT '时限数值',

    start_time DATETIME NOT NULL COMMENT '计时开始时间',
    deadline_time DATETIME NOT NULL COMMENT '截止时间',
    actual_finish_time DATETIME COMMENT '实际完成时间',

    total_seconds INT COMMENT '总时限（秒）',
    used_seconds INT COMMENT '已用时（秒）',
    remaining_seconds INT COMMENT '剩余时间（秒）',
    is_timeout TINYINT DEFAULT 0 COMMENT '是否超时',
    timeout_seconds INT COMMENT '超时时长（秒）',

    work_hours_used INT COMMENT '已用工作小时',
    work_days_used INT COMMENT '已用工作日',

    timer_status VARCHAR(20) NOT NULL COMMENT '计时状态',

    is_bundled TINYINT DEFAULT 0 COMMENT '是否捆绑计时',
    bundled_case_ids VARCHAR(500) COMMENT '捆绑案件ID列表',

    remark VARCHAR(200) COMMENT '备注',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_case_id (case_id),
    INDEX idx_timer_stage (timer_stage),
    INDEX idx_deadline_time (deadline_time)
) COMMENT '案件计时记录表';

-- ============================================
-- 九、业务配置相关表 (6张)
-- ============================================

-- 9.1 督办记录表
CREATE TABLE supervise_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    supervise_code VARCHAR(20) NOT NULL COMMENT '督办编码',

    case_id BIGINT NOT NULL COMMENT '案件ID',
    case_code VARCHAR(19) NOT NULL COMMENT '案件编码',

    supervise_type VARCHAR(20) NOT NULL COMMENT '督办类型：timeout=超时督办，leader=领导督办，important=重点督办',
    supervise_level INT DEFAULT 1 COMMENT '督办级别',

    supervise_reason VARCHAR(500) NOT NULL COMMENT '督办原因',
    supervise_requirement VARCHAR(500) COMMENT '督办要求',

    supervisor_id BIGINT NOT NULL COMMENT '督办人ID',
    supervisor_name VARCHAR(50) NOT NULL COMMENT '督办人姓名',
    supervisor_dept_id BIGINT COMMENT '督办人部门ID',
    supervisor_dept_name VARCHAR(100) COMMENT '督办人部门名称',

    supervise_time DATETIME NOT NULL COMMENT '督办时间',

    supervise_status VARCHAR(20) NOT NULL COMMENT '督办状态',

    need_reply TINYINT DEFAULT 0 COMMENT '是否需要回复',

    reply_opinion VARCHAR(500) COMMENT '回复意见',
    reply_user_id BIGINT COMMENT '回复人ID',
    reply_user_name VARCHAR(50) COMMENT '回复人姓名',
    reply_time DATETIME COMMENT '回复时间',

    resolve_time DATETIME COMMENT '解决时间',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_supervise_code (supervise_code),
    INDEX idx_case_id (case_id),
    INDEX idx_supervise_status (supervise_status)
) COMMENT '督办记录表';

-- 9.2 管控区域配置表
CREATE TABLE control_area_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',

    control_type VARCHAR(20) NOT NULL COMMENT '管控类型：area=区域管控，time=时段管控',

    boundary JSON COMMENT '管控区域边界',
    area_desc VARCHAR(200) COMMENT '区域描述',

    start_time DATETIME COMMENT '管控开始时间',
    end_time DATETIME COMMENT '管控结束时间',
    time_desc VARCHAR(200) COMMENT '时段描述',

    small_ids VARCHAR(500) COMMENT '限制小类ID列表',
    small_codes VARCHAR(200) COMMENT '限制小类编码列表',

    status TINYINT DEFAULT 1 COMMENT '状态',

    tip_message VARCHAR(200) COMMENT '禁止上报时的提示信息',

    apply_dept_id BIGINT COMMENT '申请部门ID',
    apply_dept_name VARCHAR(100) COMMENT '申请部门名称',
    apply_reason VARCHAR(500) COMMENT '申请原因',
    approve_status VARCHAR(20) COMMENT '审批状态',

    approve_user_id BIGINT COMMENT '审批人ID',
    approve_user_name VARCHAR(50) COMMENT '审批人姓名',
    approve_time DATETIME COMMENT '审批时间',
    approve_opinion VARCHAR(500) COMMENT '审批意见',

    attachments VARCHAR(500) COMMENT '附件ID列表',

    remark VARCHAR(200) COMMENT '备注',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    INDEX idx_control_type (control_type),
    INDEX idx_status (status)
) COMMENT '管控区域配置表';

-- 9.3 公告表
CREATE TABLE announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    announcement_code VARCHAR(20) NOT NULL COMMENT '公告编码',

    title VARCHAR(100) NOT NULL COMMENT '公告标题',
    content TEXT NOT NULL COMMENT '公告内容',

    announcement_type VARCHAR(20) NOT NULL COMMENT '公告类型',

    doc_number VARCHAR(50) COMMENT '文号',

    publisher_id BIGINT NOT NULL COMMENT '发布人ID',
    publisher_name VARCHAR(50) NOT NULL COMMENT '发布人姓名',
    publisher_dept_id BIGINT COMMENT '发布人部门ID',
    publisher_dept_name VARCHAR(100) COMMENT '发布人部门名称',

    publish_time DATETIME NOT NULL COMMENT '发布时间',

    expire_time DATETIME COMMENT '过期时间',

    receiver_type VARCHAR(20) NOT NULL COMMENT '接收范围类型',
    receiver_ids VARCHAR(500) COMMENT '接收人/部门/岗位ID列表',

    status VARCHAR(20) NOT NULL COMMENT '状态',

    read_count INT DEFAULT 0 COMMENT '已阅读人数',
    total_receiver_count INT DEFAULT 0 COMMENT '接收总人数',

    attachments VARCHAR(500) COMMENT '附件ID列表',

    is_top TINYINT DEFAULT 0 COMMENT '是否置顶',
    top_order INT DEFAULT 0 COMMENT '置顶排序',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_announcement_code (announcement_code),
    INDEX idx_status (status),
    INDEX idx_publish_time (publish_time)
) COMMENT '公告表';

-- 9.4 公告阅读记录表
CREATE TABLE announcement_read (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    announcement_id BIGINT NOT NULL COMMENT '公告ID',

    reader_id BIGINT NOT NULL COMMENT '阅读人ID',
    reader_name VARCHAR(50) COMMENT '阅读人姓名',
    reader_dept_id BIGINT COMMENT '阅读人部门ID',

    read_time DATETIME NOT NULL COMMENT '阅读时间',

    read_source VARCHAR(20) COMMENT '阅读来源',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_announcement_reader (announcement_id, reader_id),
    INDEX idx_announcement_id (announcement_id),
    INDEX idx_reader_id (reader_id)
) COMMENT '公告阅读记录表';

-- 9.5 今日提示表
CREATE TABLE daily_tip (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tip_code VARCHAR(20) NOT NULL COMMENT '提示编码',

    title VARCHAR(100) NOT NULL COMMENT '提示标题',
    content TEXT NOT NULL COMMENT '提示内容',

    publisher_id BIGINT NOT NULL COMMENT '发布人ID',
    publisher_name VARCHAR(50) NOT NULL COMMENT '发布人姓名',

    publish_time DATETIME NOT NULL COMMENT '发布时间',

    expire_time DATETIME COMMENT '过期时间',

    receiver_type VARCHAR(20) DEFAULT 'collector' COMMENT '接收范围',
    receiver_ids VARCHAR(500) COMMENT '指定接收人ID列表',

    status VARCHAR(20) NOT NULL COMMENT '状态',

    read_count INT DEFAULT 0 COMMENT '已阅读人数',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_tip_code (tip_code),
    INDEX idx_status (status),
    INDEX idx_publish_time (publish_time)
) COMMENT '今日提示表';

-- 9.6 系统参数配置表
CREATE TABLE system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    config_key VARCHAR(50) NOT NULL COMMENT '配置键',
    config_value VARCHAR(500) NOT NULL COMMENT '配置值',
    config_type VARCHAR(20) COMMENT '配置类型',

    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
    config_desc VARCHAR(200) COMMENT '配置说明',

    config_group VARCHAR(20) COMMENT '配置分组',

    status TINYINT DEFAULT 1 COMMENT '状态',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_config_key (config_key)
) COMMENT '系统参数配置表';

-- ============================================
-- 十、考核评价相关表 (6张)
-- ============================================

-- 10.1 考核指标配置表
CREATE TABLE evaluation_indicator (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    indicator_code VARCHAR(20) NOT NULL COMMENT '指标编码',
    indicator_name VARCHAR(100) NOT NULL COMMENT '指标名称',
    indicator_type VARCHAR(20) NOT NULL COMMENT '指标类型：area=区域评价，dept=部门评价，position=岗位评价，collector=采集员评价',

    group_name VARCHAR(50) COMMENT '指标分组名称',
    group_code VARCHAR(20) COMMENT '指标分组编码',

    indicator_desc VARCHAR(200) COMMENT '指标说明',
    calc_formula VARCHAR(200) COMMENT '计算公式',

    max_score DECIMAL(5,2) COMMENT '最高分值',
    weight DECIMAL(3,2) COMMENT '权重',

    sort_order INT DEFAULT 0,

    status TINYINT DEFAULT 1,

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_indicator_code (indicator_code),
    INDEX idx_indicator_type (indicator_type)
) COMMENT '考核指标配置表';

-- 10.2 考核周期配置表
CREATE TABLE evaluation_period (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    period_type VARCHAR(20) NOT NULL COMMENT '周期类型',
    period_name VARCHAR(50) NOT NULL COMMENT '周期名称',

    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',

    period_status VARCHAR(20) NOT NULL COMMENT '周期状态',

    publish_time DATETIME COMMENT '发布时间',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    INDEX idx_period_type (period_type),
    INDEX idx_start_date (start_date)
) COMMENT '考核周期配置表';

-- 10.3 区域考核结果表
CREATE TABLE evaluation_area (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    period_id BIGINT NOT NULL COMMENT '考核周期ID',

    area_type VARCHAR(20) NOT NULL COMMENT '区域类型',
    area_id BIGINT NOT NULL COMMENT '区域ID',
    area_code VARCHAR(20) COMMENT '区域编码',
    area_name VARCHAR(100) COMMENT '区域名称',

    report_count INT DEFAULT 0 COMMENT '上报数',
    collector_report_count INT DEFAULT 0 COMMENT '采集员上报数',
    public_report_count INT DEFAULT 0 COMMENT '公众举报数',
    accept_count INT DEFAULT 0 COMMENT '立案数',
    accept_rate DECIMAL(5,2) COMMENT '立案率',

    should_handle_count INT DEFAULT 0 COMMENT '应处置数',
    handle_count INT DEFAULT 0 COMMENT '处置数',
    ontime_handle_count INT DEFAULT 0 COMMENT '按期处置数',
    handle_rate DECIMAL(5,2) COMMENT '处置率',
    ontime_handle_rate DECIMAL(5,2) COMMENT '按期处置率',

    should_close_count INT DEFAULT 0 COMMENT '应结案数',
    close_count INT DEFAULT 0 COMMENT '结案数',
    ontime_close_count INT DEFAULT 0 COMMENT '按期结案数',
    close_rate DECIMAL(5,2) COMMENT '结案率',
    ontime_close_rate DECIMAL(5,2) COMMENT '按期结案率',

    timeout_count INT DEFAULT 0 COMMENT '超时案件数',
    rework_count INT DEFAULT 0 COMMENT '返工案件数',

    high_incident_rate DECIMAL(5,2) COMMENT '高发案件占比',
    repeat_rate DECIMAL(5,2) COMMENT '重复案件率',

    total_score DECIMAL(5,2) COMMENT '综合得分',
    `rank` INT COMMENT '排名',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_period_id (period_id),
    INDEX idx_area_type (area_type),
    INDEX idx_area_id (area_id)
) COMMENT '区域考核结果表';

-- 10.4 部门考核结果表
CREATE TABLE evaluation_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    period_id BIGINT NOT NULL COMMENT '考核周期ID',

    dept_id BIGINT NOT NULL COMMENT '部门ID',
    dept_code VARCHAR(20) COMMENT '部门编码',
    dept_name VARCHAR(100) COMMENT '部门名称',
    dept_level INT COMMENT '部门层级',

    receive_count INT DEFAULT 0 COMMENT '接收案件数',
    handle_count INT DEFAULT 0 COMMENT '处置案件数',
    ontime_handle_count INT DEFAULT 0 COMMENT '按期处置数',
    timeout_handle_count INT DEFAULT 0 COMMENT '超时处置数',
    timeout_not_handle_count INT DEFAULT 0 COMMENT '超时未处置数',

    handle_rate DECIMAL(5,2) COMMENT '处置率',
    ontime_handle_rate DECIMAL(5,2) COMMENT '按期处置率',

    return_count INT DEFAULT 0 COMMENT '回退次数',
    return_rate DECIMAL(5,2) COMMENT '回退率',

    rework_count INT DEFAULT 0 COMMENT '返工次数',
    rework_rate DECIMAL(5,2) COMMENT '返工率',

    check_pass_count INT DEFAULT 0 COMMENT '核查通过数',
    check_not_pass_count INT DEFAULT 0 COMMENT '核查不通过数',
    check_pass_rate DECIMAL(5,2) COMMENT '核查通过率',

    avg_handle_time INT COMMENT '平均处置用时（秒）',

    appeal_count INT DEFAULT 0 COMMENT '申诉次数',
    appeal_pass_count INT DEFAULT 0 COMMENT '申诉通过次数',

    deduction_score DECIMAL(5,2) DEFAULT 0 COMMENT '扣分',
    deduction_reason VARCHAR(500) COMMENT '扣分原因',

    total_score DECIMAL(5,2) COMMENT '综合得分',
    `rank` INT COMMENT '排名',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_period_id (period_id),
    INDEX idx_dept_id (dept_id)
) COMMENT '部门考核结果表';

-- 10.5 岗位考核结果表
CREATE TABLE evaluation_position (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    period_id BIGINT NOT NULL COMMENT '考核周期ID',

    user_id BIGINT NOT NULL COMMENT '用户ID',
    user_name VARCHAR(50) COMMENT '用户姓名',
    dept_id BIGINT COMMENT '所属部门ID',
    dept_name VARCHAR(100) COMMENT '所属部门名称',
    position_type VARCHAR(20) COMMENT '岗位类型',

    total_work_count INT DEFAULT 0 COMMENT '总操作量',
    accept_count INT DEFAULT 0 COMMENT '立案数',
    ontime_accept_count INT DEFAULT 0 COMMENT '按期立案数',
    dispatch_count INT DEFAULT 0 COMMENT '派遣数',
    accurate_dispatch_count INT DEFAULT 0 COMMENT '准确派遣数',
    close_count INT DEFAULT 0 COMMENT '结案数',

    avg_process_time INT COMMENT '平均处理用时（秒）',
    ontime_rate DECIMAL(5,2) COMMENT '按时完成率',

    error_count INT DEFAULT 0 COMMENT '差错数',
    error_rate DECIMAL(5,2) COMMENT '差错率',

    total_score DECIMAL(5,2) COMMENT '综合得分',
    `rank` INT COMMENT '排名',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_period_id (period_id),
    INDEX idx_user_id (user_id)
) COMMENT '岗位考核结果表';

-- 10.6 采集员考核结果表
CREATE TABLE evaluation_collector (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    period_id BIGINT NOT NULL COMMENT '考核周期ID',

    user_id BIGINT NOT NULL COMMENT '采集员ID',
    user_name VARCHAR(50) COMMENT '采集员姓名',
    dept_id BIGINT COMMENT '所属部门ID',
    resp_grid_id BIGINT COMMENT '责任网格ID',
    resp_grid_name VARCHAR(100) COMMENT '责任网格名称',

    report_count INT DEFAULT 0 COMMENT '上报数',
    valid_report_count INT DEFAULT 0 COMMENT '有效上报数',
    invalid_report_count INT DEFAULT 0 COMMENT '无效上报数',
    valid_report_rate DECIMAL(5,2) COMMENT '有效上报率',

    verify_receive_count INT DEFAULT 0 COMMENT '接收核实任务数',
    verify_finish_count INT DEFAULT 0 COMMENT '完成核实数',
    verify_rate DECIMAL(5,2) COMMENT '核实完成率',
    ontime_verify_count INT DEFAULT 0 COMMENT '按时核实数',
    ontime_verify_rate DECIMAL(5,2) COMMENT '按时核实率',

    check_receive_count INT DEFAULT 0 COMMENT '接收核查任务数',
    check_finish_count INT DEFAULT 0 COMMENT '完成核查数',
    check_rate DECIMAL(5,2) COMMENT '核查完成率',
    ontime_check_count INT DEFAULT 0 COMMENT '按时核查数',
    ontime_check_rate DECIMAL(5,2) COMMENT '按时核查率',

    miss_count INT DEFAULT 0 COMMENT '漏报数',
    miss_rate DECIMAL(5,2) COMMENT '漏报率',

    coverage_rate DECIMAL(5,2) COMMENT '区域覆盖率',

    self_handle_count INT DEFAULT 0 COMMENT '自行处置数',

    total_score DECIMAL(5,2) COMMENT '综合得分',
    `rank` INT COMMENT '排名',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_period_id (period_id),
    INDEX idx_user_id (user_id)
) COMMENT '采集员考核结果表';

-- ============================================
-- 十一、消息通知相关表 (5张)
-- ============================================

-- 11.1 消息模板表
CREATE TABLE message_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    template_code VARCHAR(20) NOT NULL COMMENT '模板编码',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',

    msg_type VARCHAR(20) NOT NULL COMMENT '消息类型',
    msg_scene VARCHAR(50) NOT NULL COMMENT '消息场景',

    title_template VARCHAR(100) NOT NULL COMMENT '标题模板',
    content_template VARCHAR(500) NOT NULL COMMENT '内容模板',

    push_methods VARCHAR(50) NOT NULL COMMENT '推送方式',

    status TINYINT DEFAULT 1 COMMENT '状态',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_template_code (template_code),
    INDEX idx_msg_type (msg_type),
    INDEX idx_msg_scene (msg_scene)
) COMMENT '消息模板表';

-- 11.2 消息发送记录表
CREATE TABLE message_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    msg_code VARCHAR(20) NOT NULL COMMENT '消息编码',

    template_id BIGINT COMMENT '消息模板ID',
    template_code VARCHAR(20) COMMENT '模板编码',

    biz_type VARCHAR(20) COMMENT '业务类型',
    biz_id BIGINT COMMENT '业务ID',
    biz_code VARCHAR(30) COMMENT '业务编码',

    msg_title VARCHAR(100) NOT NULL COMMENT '消息标题',
    msg_content VARCHAR(500) NOT NULL COMMENT '消息内容',

    receiver_id BIGINT NOT NULL COMMENT '接收人ID',
    receiver_name VARCHAR(50) COMMENT '接收人姓名',
    receiver_phone VARCHAR(20) COMMENT '接收人手机号',
    receiver_dept_id BIGINT COMMENT '接收人部门ID',
    receiver_dept_name VARCHAR(100) COMMENT '接收人部门名称',

    push_method VARCHAR(20) NOT NULL COMMENT '推送方式',

    send_status VARCHAR(20) NOT NULL COMMENT '发送状态',

    send_time DATETIME COMMENT '发送时间',

    fail_reason VARCHAR(200) COMMENT '失败原因',
    retry_count INT DEFAULT 0 COMMENT '重试次数',

    third_response VARCHAR(200) COMMENT '第三方推送返回信息',

    read_status TINYINT DEFAULT 0 COMMENT '阅读状态',
    read_time DATETIME COMMENT '阅读时间',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_msg_code (msg_code),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_biz_type (biz_type),
    INDEX idx_biz_id (biz_id),
    INDEX idx_send_status (send_status),
    INDEX idx_read_status (read_status)
) COMMENT '消息发送记录表';

-- 11.3 用户消息表
CREATE TABLE user_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    user_id BIGINT NOT NULL COMMENT '用户ID',

    msg_record_id BIGINT NOT NULL COMMENT '消息发送记录ID',
    msg_code VARCHAR(20) COMMENT '消息编码',

    msg_type VARCHAR(20) NOT NULL COMMENT '消息类型',
    msg_title VARCHAR(100) NOT NULL COMMENT '消息标题',
    msg_content VARCHAR(500) NOT NULL COMMENT '消息内容',

    biz_type VARCHAR(20) COMMENT '业务类型',
    biz_id BIGINT COMMENT '业务ID',
    biz_code VARCHAR(30) COMMENT '业务编码',

    msg_status VARCHAR(20) DEFAULT 'unread' COMMENT '消息状态',

    read_time DATETIME COMMENT '阅读时间',

    delete_time DATETIME COMMENT '删除时间',

    is_top TINYINT DEFAULT 0 COMMENT '是否置顶',

    msg_time DATETIME NOT NULL COMMENT '消息产生时间',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_msg_status (msg_status),
    INDEX idx_msg_time (msg_time)
) COMMENT '用户消息表';

-- 11.4 消息订阅配置表
CREATE TABLE message_subscribe (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    user_id BIGINT NOT NULL COMMENT '用户ID',

    msg_type VARCHAR(20) NOT NULL COMMENT '消息类型',
    msg_scene VARCHAR(50) NOT NULL COMMENT '消息场景',

    push_app TINYINT DEFAULT 1 COMMENT '是否APP推送',
    push_sms TINYINT DEFAULT 0 COMMENT '是否短信推送',
    push_wechat TINYINT DEFAULT 0 COMMENT '是否微信推送',

    status TINYINT DEFAULT 1 COMMENT '状态',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_scene (user_id, msg_scene),
    INDEX idx_user_id (user_id)
) COMMENT '消息订阅配置表';

-- 11.5 系统消息表
CREATE TABLE system_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    msg_code VARCHAR(20) NOT NULL COMMENT '消息编码',

    msg_title VARCHAR(100) NOT NULL COMMENT '消息标题',
    msg_content TEXT NOT NULL COMMENT '消息内容',

    msg_type VARCHAR(20) NOT NULL COMMENT '消息类型',

    publisher_id BIGINT NOT NULL COMMENT '发布人ID',
    publisher_name VARCHAR(50) COMMENT '发布人姓名',

    publish_time DATETIME NOT NULL COMMENT '发布时间',

    expire_time DATETIME COMMENT '过期时间',

    receiver_type VARCHAR(20) NOT NULL COMMENT '接收范围',
    receiver_ids VARCHAR(500) COMMENT '接收ID列表',

    status VARCHAR(20) NOT NULL COMMENT '状态',

    total_receiver_count INT DEFAULT 0 COMMENT '接收人数',
    read_count INT DEFAULT 0 COMMENT '已读人数',

    is_top TINYINT DEFAULT 0 COMMENT '是否置顶',
    top_order INT DEFAULT 0 COMMENT '置顶顺序',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_msg_code (msg_code),
    INDEX idx_status (status),
    INDEX idx_publish_time (publish_time)
) COMMENT '系统消息表';

-- ============================================
-- 十二、日志记录相关表 (4张)
-- ============================================

-- 12.1 操作日志表
CREATE TABLE operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    user_id BIGINT NOT NULL COMMENT '操作人ID',
    user_name VARCHAR(50) COMMENT '操作人姓名',
    user_code VARCHAR(20) COMMENT '操作人编码',
    dept_id BIGINT COMMENT '所属部门ID',
    dept_name VARCHAR(100) COMMENT '所属部门名称',

    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    operation_desc VARCHAR(200) COMMENT '操作描述',

    module_name VARCHAR(50) COMMENT '模块名称',
    module_code VARCHAR(20) COMMENT '模块编码',

    method_name VARCHAR(100) COMMENT '方法名称',
    request_url VARCHAR(200) COMMENT '请求URL',
    request_method VARCHAR(10) COMMENT '请求方式',

    request_params TEXT COMMENT '请求参数',

    response_code VARCHAR(10) COMMENT '响应码',
    response_msg VARCHAR(200) COMMENT '响应消息',

    operation_result VARCHAR(20) COMMENT '操作结果',

    biz_type VARCHAR(20) COMMENT '业务类型',
    biz_id BIGINT COMMENT '业务ID',
    biz_code VARCHAR(30) COMMENT '业务编码',

    operation_time DATETIME NOT NULL COMMENT '操作时间',

    time_cost INT COMMENT '耗时（毫秒）',

    operation_source VARCHAR(20) COMMENT '操作来源',

    ip_address VARCHAR(50) COMMENT 'IP地址',

    device_info VARCHAR(200) COMMENT '设备信息',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_biz_type (biz_type),
    INDEX idx_biz_id (biz_id),
    INDEX idx_operation_time (operation_time)
) COMMENT '操作日志表';

-- 12.2 登录日志表
CREATE TABLE login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    user_id BIGINT COMMENT '用户ID',
    user_name VARCHAR(50) COMMENT '用户姓名',
    user_code VARCHAR(20) COMMENT '用户编码',

    login_type VARCHAR(20) NOT NULL COMMENT '登录类型',

    login_status VARCHAR(20) NOT NULL COMMENT '登录状态',

    login_time DATETIME NOT NULL COMMENT '登录时间',

    login_source VARCHAR(20) COMMENT '登录来源',

    ip_address VARCHAR(50) COMMENT 'IP地址',

    device_info VARCHAR(200) COMMENT '设备信息',

    fail_reason VARCHAR(100) COMMENT '失败原因',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_login_time (login_time),
    INDEX idx_login_status (login_status)
) COMMENT '登录日志表';

-- 12.3 数据导入记录表
CREATE TABLE import_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    import_type VARCHAR(20) NOT NULL COMMENT '导入类型',

    file_name VARCHAR(100) NOT NULL COMMENT '导入文件名',
    file_path VARCHAR(200) COMMENT '文件存储路径',
    file_size INT COMMENT '文件大小（字节）',

    total_count INT NOT NULL COMMENT '总行数',
    success_count INT NOT NULL COMMENT '成功行数',
    fail_count INT NOT NULL COMMENT '失败行数',

    import_status VARCHAR(20) NOT NULL COMMENT '导入状态',

    error_detail TEXT COMMENT '错误明细JSON',

    import_user_id BIGINT NOT NULL COMMENT '导入人ID',
    import_user_name VARCHAR(50) COMMENT '导入人姓名',

    import_time DATETIME NOT NULL COMMENT '导入时间',

    remark VARCHAR(200) COMMENT '备注',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_import_type (import_type),
    INDEX idx_import_time (import_time),
    INDEX idx_import_user (import_user_id)
) COMMENT '数据导入记录表';

-- 12.4 异常日志表
CREATE TABLE exception_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    exception_type VARCHAR(100) NOT NULL COMMENT '异常类型',
    exception_msg VARCHAR(500) COMMENT '异常消息',
    exception_stack TEXT COMMENT '异常堆栈',

    class_name VARCHAR(200) COMMENT '类名',
    method_name VARCHAR(100) COMMENT '方法名',
    line_number INT COMMENT '行号',

    request_url VARCHAR(200) COMMENT '请求URL',
    request_method VARCHAR(10) COMMENT '请求方式',
    request_params TEXT COMMENT '请求参数',

    user_id BIGINT COMMENT '用户ID',
    user_name VARCHAR(50) COMMENT '用户姓名',

    exception_time DATETIME NOT NULL COMMENT '异常发生时间',

    handle_status VARCHAR(20) COMMENT '处理状态',
    handle_time DATETIME COMMENT '处理时间',
    handle_user_id BIGINT COMMENT '处理人ID',
    handle_remark VARCHAR(200) COMMENT '处理备注',

    exception_source VARCHAR(20) COMMENT '异常来源',

    ip_address VARCHAR(50) COMMENT 'IP地址',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_exception_type (exception_type),
    INDEX idx_exception_time (exception_time),
    INDEX idx_handle_status (handle_status)
) COMMENT '异常日志表';

-- ============================================
-- 初始化数据
-- ============================================

-- 初始化岗位
INSERT INTO position (position_code, position_name, position_type, description) VALUES
('COLLECTOR', '采集员', 'collector', '负责现场问题采集'),
('ACCEPTOR', '受理员', 'acceptor', '负责案件受理登记'),
('DISPATCHER', '派遣员', 'dispatcher', '负责案件派遣'),
('SUPERVISOR', '值班长', 'supervisor', '负责监督协调'),
('LEADER', '领导', 'leader', '领导岗位'),
('HANDLER', '处置人员', 'handler', '处置部门人员'),
('EVALUATOR', '考核员', 'evaluator', '负责考核评价');

-- 初始化角色
INSERT INTO role (role_code, role_name, description, is_system) VALUES
('COLLECTOR', '采集员', '采集员角色', 1),
('ACCEPTOR', '受理员', '受理员角色', 1),
('DISPATCHER', '派遣员', '派遣员角色', 1),
('HANDLER', '处置部门', '处置部门角色', 1),
('SUPERVISOR', '值班长', '值班长角色', 1),
('ADMIN', '管理员', '系统管理员', 1);

-- 初始化工作时段配置
INSERT INTO work_time_config (config_name, am_start_time, am_end_time, pm_start_time, pm_end_time, is_default, remark) VALUES
('标准工作时间', '08:00', '12:00', '14:00', '18:00', 1, '每天工作时间8小时');

-- 初始化时限计算规则
INSERT INTO time_limit_rule (time_limit_type, type_name, is_continuous, include_holiday, include_weekend, calc_desc) VALUES
('urgent_hour', '紧急工作时', 1, 1, 1, '自然时间连续计算，下班也计时，包含节假日和周末'),
('work_hour', '工作时', 0, 0, 0, '仅工作时间计算，不含节假日和周末'),
('work_day', '工作日', 0, 0, 0, '按工作日计算，不含周末和法定节假日'),
('natural_day', '自然日', 1, 1, 1, '自然时间连续计算，包含所有日期');

-- 初始化大类（部件类）
INSERT INTO category_big (big_code, big_name, category_type, sort_order) VALUES
('01', '公用设施', 'component', 1),
('02', '道路交通设施', 'component', 2),
('03', '市容环境设施', 'component', 3),
('04', '园林绿化设施', 'component', 4),
('05', '其他部件', 'component', 5);

-- 初始化大类（事件类）
INSERT INTO category_big (big_code, big_name, category_type, sort_order) VALUES
('01', '市容环境', 'event', 10),
('02', '宣传广告', 'event', 11),
('03', '施工管理', 'event', 12),
('04', '突发事件', 'event', 13),
('05', '街面秩序', 'event', 14),
('06', '扩展事件', 'event', 15);

-- 初始化系统配置
INSERT INTO system_config (config_key, config_value, config_name, config_desc, config_group) VALUES
('system_name', '智慧城管系统', '系统名称', '系统显示名称', 'system'),
('case_code_prefix', 'YC', '案件编码前缀', '运城市编码前缀', 'case'),
('verify_timeout', '24', '核实任务时限', '核实任务完成时限（小时）', 'task'),
('check_timeout', '2', '核查任务时限', '核查任务完成时限（小时）', 'task'),
('similar_distance', '50', '相似案件距离阈值', '相似案件判定距离（米）', 'case'),
('similar_time', '24', '相似案件时间阈值', '相似案件判定时间（小时）', 'case');

-- 数据库初始化完成
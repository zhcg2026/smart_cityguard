# 智慧城管系统 (Smart Cityguard)

运城市城市综合管理服务系统，包含采集员APP、受理员平台、派遣员平台、处置部门平台等模块。

## 项目结构

```
smart_cityguard/
├── backend/                    # Spring Boot 后端 (15个模块)
│   ├── smart-cityguard-server  # 启动入口
│   ├── smart-cityguard-common  # 公共模块
│   ├── smart-cityguard-auth    # 认证模块
│   ├── smart-cityguard-case    # 案件模块
│   ├── smart-cityguard-config  # 配置模块
│   ├── smart-cityguard-task    # 任务模块
│   ├── smart-cityguard-geo     # 地理模块
│   ├── smart-cityguard-file    # 文件模块
│   ├── smart-cityguard-message # 消息模块
│   ├── smart-cityguard-appeal  # 申诉模块
│   └── ...                     # 其他模块
│
├── frontend/
│   ├── admin-web/              # Vue3 管理端
│   └── collector-app/          # uni-app 采集端
│
├── database/
│   └── init.sql                # 数据库初始化脚本 (55张表)
│
└── docs/                       # 设计文档
```

立结案标准 Excel 模板（`muban.xlsx`）的**解析约定与库表映射**见：**[docs/case-standard-muban-spec.md](docs/case-standard-muban-spec.md)**（先文档、后开发）。

## 技术栈

### 后端
- Spring Boot 3.2.0
- MyBatis-Plus 3.5.5
- Spring Security + JWT
- MySQL 8.0
- Redis (Redisson)
- RabbitMQ
- MinIO (文件存储)
- Knife4j (Swagger API文档)

### 前端
- Vue3 + Vite + Element Plus (管理端)
- uni-app (采集端跨平台APP)

## 快速开始

本地按顺序启动命令见：**[docs/startup-guide.md](docs/startup-guide.md)**（精简版，仅调试用）。

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE cityguard DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 执行初始化脚本
mysql -u root -p cityguard < database/init.sql
```

### 2. 启动后端服务

```bash
# 进入后端目录
cd backend

# Maven编译打包
mvn clean package -DskipTests

# 启动服务
java -jar smart-cityguard-server/target/smart-cityguard-server-1.0.0.jar

# 或直接运行
mvn spring-boot:run -pl smart-cityguard-server
```

**配置文件位置**: `backend/smart-cityguard-server/src/main/resources/application.yml`

需修改的配置：
- MySQL连接信息 (默认: localhost:3306/cityguard, root/root)
- Redis连接信息 (默认: localhost:6379)
- RabbitMQ连接信息 (默认: localhost:5672, guest/guest)
- MinIO连接信息 (默认: localhost:9000, admin/password)

### 3. 启动前端管理端

```bash
# 进入管理端目录
cd frontend/admin-web

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问地址: http://localhost:3000

### 4. 启动采集端 (uni-app)

使用 HBuilderX:
1. 打开 `frontend/collector-app` 目录
2. 点击 "运行" → "运行到浏览器" 或 "运行到手机"
3. 或使用命令行: `npm run dev:h5`

## API文档

启动后端后访问: http://localhost:8080/doc.html

## 主要功能

### 案件流程
上报 → 核查 → 立案 → 派遣 → 处置 → 核实 → 结案

### 用户角色
- 采集员：问题上报、核查核实任务
- 受理员：案件登记、案件批转
- 派遣员：案件派遣、监督督办
- 处置部门：案件处置、反馈处理

### 核心接口

| 模块 | 接口 | 说明 |
|------|------|------|
| Auth | /auth/login | 用户登录 |
| Auth | /auth/info | 获取用户信息 |
| Case | /case/report | 问题上报 |
| Case | /case/register | 立案 |
| Case | /case/dispatch | 派遣 |
| Case | /case/handle | 处置 |
| Case | /case/verify | 核查 |
| Case | /case/check | 核实 |
| Config | /config/category/big/list | 获取大类 |
| Config | /config/category/small/list/{id} | 获取小类 |
| Config | /config/standard/conditions/{id} | 获取立案条件 |
| Task | /task/verify/list | 核查任务列表 |
| Task | /task/check/list | 核实任务列表 |
| File | /file/upload | 文件上传 |
| Geo | /geo/grid/info | 网格定位 |
| Message | /message/list | 用户消息 |
| Appeal | /appeal/submit | 提交申诉 |

## 数据库表结构 (55张表)

### 组织架构 (4张)
- sys_user, sys_role, sys_department, sys_position

### 权限管理 (4张)
- sys_role_user, sys_menu, sys_role_menu

### 网格地理 (6张)
- sys_street, sys_community, sys_grid, responsibility_grid...

### 案件标准 (5张)
- category_big, category_small, case_standard...

### 案件流转 (4张)
- case_info, case_flow_record, case_attachment...

### 核实核查 (4张)
- verify_task, check_task...

### 其他 (28张)
- 申诉、计时、配置、考核、消息、日志等

## 开发进度

| 模块 | 状态 |
|------|------|
| 后端架构 | ✅ 完成 |
| 数据库设计 | ✅ 完成 |
| Auth认证 | ✅ 完成 |
| Config配置 | ✅ 完成 |
| Case案件 | ✅ 完成 |
| Task任务 | ✅ 完成 |
| Geo地理 | ✅ 完成 |
| File文件 | ✅ 完成 |
| Message消息 | ✅ 完成 |
| Appeal申诉 | ✅ 完成 |
| Vue3管理端 | ✅ 基本完成 |
| uni-app采集端 | ✅ 基本完成 |

## GitHub仓库

https://github.com/zhcg2026/smart_cityguard
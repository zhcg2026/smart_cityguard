# 本地调试与会话记录（续作备忘）

> 更新时间：2026-05-21  
> 用途：下次打开项目时快速回忆已做修改与本地启动方式。

---

## 1. 移动端上报失败（MinIO）

- **现象**：`文件上传失败: Failed to connect to ... :9000`  
- **原因**：后端把文件写入 MinIO，本机未起 MinIO 或端口不对。  
- **处理**：本机安装 MinIO（曾用 `winget install MinIO.Server`；或官方 `minio.exe`）。  
- **账号**：`application.yml` 中 `minio.access-key` / `secret-key` 须与 MinIO 启动环境变量一致（如 `minioadmin` 或 `admin`/`password`）。

---

## 2. 上报失败（`case_flow_record.case_code`）

- **现象**：`Field 'case_code' doesn't have a default value`，SQL 为 `CaseFlowRecordMapper.insert-Inline` 且 **INSERT 无 case_code**。  
- **原因**：表要求 `case_code` NOT NULL；原先流程记录插入未带该列 / 运行的是旧包。  
- **代码结论**（`smart-cityguard-case`）：  
  - `CaseServiceImpl.saveFlowRecord`：为每条记录补全 `case_code`（含从 `case_info` 回查兜底）。  
  - **已改为使用 `JdbcTemplate` 手写 INSERT**，避免 `BaseMapper.insert` 在部分环境下仍省略 `case_code`。  
- **部署**：改代码后需 **`mvn clean package`**（或 IDE 全量 Rebuild）并 **重启真正监听 8080 的进程**；曾用 `javaw.exe -jar ...smart-cityguard-server-1.0.0.jar` 占 8080。

---

## 3. 内置启动 MinIO（可选）

- **配置**：`application.yml` → `minio.embedded`  
  - `enabled: true` 时，在创建 `MinioClient` **之前**由后端拉起 `minio` 子进程。  
  - 若 9000 已被占用则 **跳过** 启动。  
  - JVM 退出时 **shutdown hook** 会结束子进程。  
- **实现位置**：`smart-cityguard-file` → `MinioEmbeddedProperties`、`EmbeddedMinioSupport`、`MinioConfig`。  
- **注意**：生产环境保持 **`embedded.enabled: false`**；本机需 `minio` 在 PATH 或配置 `minio.embedded.binary` 绝对路径。

---

## 4. 本地一般要起哪些服务

| 场景 | 通常需要 |
|------|----------|
| 只测采集端 H5 | MySQL + 后端 + MinIO（或 `minio.embedded.enabled=true`）+ `collector-app` |
| 只测管理端 | MySQL + 后端 + MinIO + `admin-web` |
| 全开 | 上述 + 两个前端 |

- **后端**：`8080`，如 `mvn spring-boot:run -pl smart-cityguard-server` 或 `java -jar smart-cityguard-server/target/...jar`  
- **管理端**：`admin-web`，`npm run dev` → 约 `3000`  
- **采集端**：`collector-app`，`npm run dev` → 约 `3003`  
- **MySQL**：`application.yml` 中 `spring.datasource`（当前库名 `cityguard`）  
- **Redis**：当前配置 `spring.redis.enabled: false`，本地可不启  

---

## 5. 常用路径

- 后端主配置：`backend/smart-cityguard-server/src/main/resources/application.yml`  
- 案件流程与流程记录：`backend/smart-cityguard-case/.../CaseServiceImpl.java`  
- 流程记录 Mapper：`CaseFlowRecordMapper.java`  
- MinIO 与内置启动：`backend/smart-cityguard-file/src/main/java/com/cityguard/file/config/`

---

## 6. Windows 查 / 杀 8080

```powershell
netstat -ano | findstr ":8080"
tasklist /FI "PID eq <PID>"
taskkill /PID <PID> /F
```

进程可能是 **`javaw.exe`**（`-jar` 启动），不一定是 `java.exe`。

---

## 7. 其他备忘

- 日志里 **`small_name` 乱码**：检查 MySQL 库/表/连接是否为 **utf8mb4**。  
- 前端若仍报旧错：确认请求是否打到 **本机刚重启的后端**（非远程旧服务）。

---

## 9. 站内消息提醒（2026-05-21）

- **新案**：`reportCase` 后 `notifyAllAcceptorsNewCase` → 所有 `ACCEPTOR` 角色用户。
- **轮询**：管理端 `useMessagePoll.js`（BasicLayout）、采集端 `App.vue`；间隔约 25s；事件 `cityguard:refresh-lists` 刷新列表。
- **改 message/case 模块**：`mvn clean install -pl smart-cityguard-message,smart-cityguard-case -am -DskipTests` 后重启 8080。
- **表结构不对**：执行 `database/patch_user_message_align.sql`。
- **第二期**：WebSocket / App 推送（见 `PROJECT_PROGRESS.md` §2026-05-21）。

---

## 8. 2026-05-10 会话进度（明天续）

### 8.1 受理主干（后端 + 管理端）

- **上报默认状态**：`reportCase` 后案件状态改为 **`pending_register`（待立案）**，流程记录文案「采集员上报（待立案）」。历史 **`pending_verify`** 案件仍可处理。
- **待办接口**：`GET /case/pending?status=acceptor_todo` 合并 **`pending_register` + `pending_verify`**（受理员统一待办）。
- **立案**：`POST /case/register` 使用 **`CaseRegisterRequest`**（可带地址、描述、立案条件说明、经纬度、`clientUpdateTime`）；成功后 **`pending_dispatch`**；流程记录操作人为当前登录用户。
- **派遣**：`dispatch` 支持 **`clientUpdateTime`**，与立案同理做乐观并发（失败提示刷新）。
- **JdbcTemplate 写 `case_flow_record`**：避免 MP `insert` 漏 `case_code`。
- **Maven / IDE**：业务改在 **`smart-cityguard-case`** 时，IDE 运行 classpath 常指向 **`D:\maven-repository\...\smart-cityguard-case-1.0.0.jar`**，改代码后需 **`mvn clean install -pl smart-cityguard-server -am`** 再重启。

### 8.2 管理端角色菜单（已合并进代码，但有已知 BUG）

- **`frontend/admin-web/src/utils/roleAccess.js`**：`RoleGroups`、`canAccessMeta`、待办默认 Tab、角色中文名。
- **`frontend/admin-web/src/router/index.js`**：根 `/`、`/dashboard`、案件/任务等 **`meta.roles`**；守卫里 **`tryNextWithRoleGuard`**（无权限时的跳转逻辑见 **§8.3**，勿再指向 `/dashboard`）。
- **`BasicLayout.vue`**：侧栏按 `canAccessMeta` 过滤；顶栏展示角色。
- **`stores/user.js`**：`initUser` 从 localStorage 恢复 **`roles`**。

### 8.3 管理端登录后「无限重定向」— 问题说明与修复清单（备忘）

**适用范围**：`frontend/admin-web`（Vue Router 4）。采集端 `collector-app` 无此守卫逻辑。

#### 控制台典型报错

```text
vue-router.js: Uncaught (in promise) Error: Infinite redirect in navigation guard
```

浏览器可能同时反复弹出 Element Plus 提示：**「当前账号无权限访问该功能」**。

#### 行为链路（为何会死循环）

1. 用户已带 **token** 访问 **`/`**，路由配置将 **`/`** 重定向到 **`/dashboard`**。
2. **`/dashboard`** 及其父级 **`/`** 布局路由上带有 **`meta.roles`**（见 `RoleGroups.DASHBOARD`），`beforeEach` 里会调用 **`tryNextWithRoleGuard`**。
3. 当 **`canAccessMeta`** 判定当前用户的 **`userStore.roles`** 不满足 **`RoleGroups.DASHBOARD`** 时，守卫执行 **`next({ path: '/dashboard' })`**（见 `router/index.js` 内 **`tryNextWithRoleGuard`**）。
4. 下一次导航目标仍是 **`/dashboard`**，权限仍不满足 → 再次 **`next({ path: '/dashboard' })`** → Vue Router 检测到同一目标的重复重定向，抛出 **Infinite redirect**。

**结论**：把「无权限时的兜底页」设成 **`/dashboard`** 是错误的，因为 **`/dashboard` 本身就需要通过同一套角色校验**。

#### 第二类常见问题（不配代码也会「像登不进去」）

- **`getUserInfo` 成功但 `roles` 为空数组**：`canAccessMeta` 对非 ADMIN 会直接 **`false`**，同样会触发上面的守卫分支。
- **后端返回了库里存在、前端未收录的角色编码**：例如 **`database/init.sql`** 中有 **`LEADER`**（领导），若 **`roleAccess.js`** 里没有 **`RoleCode.LEADER`** 且 **`RoleGroups.DASHBOARD`** 未包含该角色，即使用户「登录成功」，也会在进入工作台时被拦下（再结合错误的 **`next('/dashboard')`** 就会死循环）。

#### 如何确认代码仍是 Bug 版

在仓库根目录对管理端路由搜：

```text
path: '/dashboard'
```

若出现在 **`tryNextWithRoleGuard`** 的无权限分支里，即为待修复状态。

#### 推荐修复（可逐项手工粘贴，避免一次性大补丁超时）

| 步骤 | 文件 | 做什么 |
|------|------|--------|
| 1 | 新建 `frontend/admin-web/src/views/error/NoPermission.vue` | 独立全屏页：说明无权限、**重新登录**（`logout` + `replace('/login')`）、可选「进入工作台」`push('/')`（修复后仅有权用户能留在工作台；无权用户仍会回到本页，但不再死循环）。样式可参考同目录 **`404.vue`**。 |
| 2 | `frontend/admin-web/src/router/index.js` | 在 **`/login`** 之后增加路由 **`path: '/no-permission'`**，**不要**设置 **`meta.roles`**（该页必须不被角色守卫二次踢飞）。 |
| 3 | 同上 `router/index.js` 内 **`tryNextWithRoleGuard`** | 开头：`if (to.path === '/no-permission') { next(); return }`。无权限分支：`next({ path: '/no-permission', replace: true })`，**删除** `next({ path: '/dashboard' })`。 |
| 4 | `frontend/admin-web/src/utils/roleAccess.js` | 在 **`RoleCode`** 增加 **`LEADER: 'LEADER'`**（与库表一致）；**`roleNameMap`** 增加中文名；将 **`COLLECTOR`、`LEADER`** 加入 **`RoleGroups.DASHBOARD`**（至少保证「仅能进工作台」的账号能落到 **`/dashboard`** 而不是权限页）。若某角色还需看案件等菜单，再把对应 **`RoleGroups.*`** 按需扩充。 |

#### 状态

- **截至 2026-05-11**：上述改动曾因会话中断 **未完整落地**；请以 **`router/index.js`** 是否仍向 **`/dashboard`** 跳转为准。

### 8.4 后端接口权限（未做）

- 当前仅 **前端** 菜单/路由控制；**API 未按角色鉴权**。正式环境建议在 Spring Security 对 `/case/**`、`/system/**` 等做角色约束。

# 项目操作与进度记录

> 每次重要变更后在此追加一条摘要，便于快速了解进展。按日期倒序（最新在上）。

---

## 2026-06-02 工作小结（综合查询/考核 + 延期挂账两级审批 + 处置 UX）

### 当日摘要

1. **综合查询（CaseQuery）**：
   - 问题状态：合并重复「作废」、已结案含 `forced_close`；去掉无业务含义的「案件登记/部门批转」来源。
   - 事部件类型→大类→小类级联；新增问题描述（包含/等于）筛选。
   - 新增 `frontend/admin-web/src/utils/caseQuery.js`；后端 `CaseQueryFilterSupport`、来源/描述条件。
2. **考核统计（evaluation/index.vue）**：
   - 筛选与综合查询对齐；**合计行**数字可点击反查（`drillAllDepts`）。
   - 后端 `CaseReportServiceImpl.drillDown` 允许 `drillHandleDeptId` 为空查全部部门。
3. **内容发布存草稿**：`FlexibleLocalDateTimeDeserializer` + `@JsonFormat` 修复 `2026-06-30 00:00:00` 解析失败。
4. **列表/详情时间**：`formatDateTime` 去掉 `T`；CaseList/CasePending/CaseDetail 已统一。
5. **延期/挂账两级审批**（核心）：
   - **HANDLER** 申请 → `pending_dept` → **DEPT** 同意报送/驳回 → **DISPATCHER** 终审。
   - **DEPT** 直接申请跳过部门初审；库补丁 `database/patch_case_adjustment_dept_review.sql`（部门初审字段）。
   - 已超时案件不可申请延期/挂账（`CaseTimerService.isHandleStageOverdue`）。
   - 处置人员详情：`handlerDeptNotice` 展示部门当次反馈（指派/打回/驳回理由）。
6. **处置照片展示**：
   - 移动端/管理端：按上传时间分批，**横向排列、旧→新**；点击预览当前图。
   - 打回后再处置可查看历次 `handle_finish` 照片。
7. **管理端案件详情**：
   - 现场照片分区（上报/处置批次/核查/核实）；流程记录**单行紧凑**（时间+加粗步骤名+奇偶行底色）。

| 层 | 内容 |
|----|------|
| **库** | `patch_case_adjustment_dept_review.sql` |
| **后端·case** | 两级审批、`HandlerDeptNotice`、查询/统计/反查 |
| **后端·common** | `JacksonDateTimeConfig`、`FlexibleLocalDateTimeDeserializer` |
| **后端·timer** | `isHandleStageOverdue` |
| **前端·管理端** | CaseQuery、evaluation、CaseDetail（附件/流程/延期审批） |
| **前端·采集端** | HandleDetail（部门提示、处置照片批次、延期/挂账） |
| **文档** | `case-adjustment-design.md`、`case-comprehensive-query-design.md` |

### 联调注意

- 改 **case/common/message/timer** 后须 `mvn install` 对应模块并重启 8080。
- 二级审批须执行 **`patch_case_adjustment_dept_review.sql`**（本机 `cityguard` 已执行）。

### 明天优先（接续）

1. **延期/挂账全链路验收**：HANDLER 申请 → DEPT 驳回/同意 → DISPATCHER 批准/驳回；超时不可申请边界 case。
2. **综合查询 + 考核统计 + 申诉** 与阶段 0 验收清单对照复测。
3. 可选：管理端「延期/挂账审批」独立页增加「部门待审」Tab（`GET /case/adjustment/pending-dept`）。
4. 可选：`AdjustmentReview.vue` 与案件详情部门审批入口统一体验。

**本地联调**：8080 / 3000 / 3003；改 case 模块 → `mvn clean install -pl smart-cityguard-case,smart-cityguard-common,smart-cityguard-message,smart-cityguard-timer -am -DskipTests` → 重启 8080。

---

## 2026-06-01 工作小结（紧急工作时计时 + 处置端时限展示）

### 当日摘要

1. **紧急工作时连续计时修复**：
   - 根因：旧 muban 导入后 `case_standard.handle_time_limit` 为「2紧急工作时」，但 `handle_time_type` 仍为 `work_hour`，配置页编辑下拉显示「工作时」；计时引擎若仅信 type 会按工作时段算。
   - **`HandleTimeLimitNormalizer`** + **`CategoryCodeHelper.parseHandleTimeLimitText`**：解析时限**以文案为准**（含「紧急」→ `urgent_hour` 连续计时）。
   - **库补丁** `database/patch_fix_urgent_handle_time_type.sql`（已对本机 `cityguard` 执行，雨水井盖等立案条件 type 已改为 `urgent_hour`）。
   - **剩余时限文案**：`CaseTimerService.formatRemaining` 改为「剩余1小时55分」，避免整小时截断误解。
2. **管理端**：
   - `CaseDetail.vue` 处置阶段展示「2紧急工作时·连续计时」+ 截止时间。
   - `CategoryManage.vue` 列表优先展示含「紧急」原文；编辑时从文案推断正确 `handleTimeType`。
3. **处置人员移动端（collector-app）**：
   - 待处置列表：右侧 **剩余时间 + 截止时间**（超时红色）。
   - 处置详情：顶部提示条 + **「处置时限」** 分组（规则/截止/剩余）。
   - 新增 `src/utils/caseTimer.js`。
4. **联调案例**：`YC202606010002`（雨水井盖 / 立案条件 855）派遣 22:36 → 截止 00:36（`urgent_hour` 2 小时连续），与规则一致。
5. **环境**：四端 8080/3000/3003/9000 已多次重启；改 **timer/config** 须 `mvn install` 后重启 8080。

| 层 | 内容 |
|----|------|
| **后端·timer** | `HandleTimeLimitNormalizer`、`CategoryCodeHelper` 解析、`CaseTimerStageDisplay.timeLimitLabel/continuous`、`formatDuration` |
| **后端·config** | `CategoryCodeHelper.resolveHandleTime` |
| **库** | `patch_fix_urgent_handle_time_type.sql` |
| **前端·管理端** | `CaseDetail.vue`、`CategoryManage.vue` |
| **前端·采集端** | `handle/index.vue`、`HandleDetail.vue`、`utils/caseTimer.js` |

### 当日讨论纪要

| 话题 | 结论 |
|------|------|
| 全链路测试 / 待办是否等于完工 | 待办清空 ≠ 全部工作完成；建议阶段 0 验收后再扩功能 |
| 国产化 / 容量 / 完善路线 | 已整理分阶段路线；不着急上线，优先完善 |
| 手机端测试 | H5 真机可用（同 WiFi + 可选 `host: true`） |
| 公众号系列第二篇 | 案件闭环与多角色协同（文稿，未入库） |

### 明天优先（接续）

1. 阶段 0 验收（综合查询/考核/申诉 + 阶段计时 + 紧急工作时新案抽测）。
2. 采集端 `vite.config.js` 加 `host: true` + 手机 H5 联调说明。
3. 处置端时限：真机 HANDLER 账号走一遍列表/详情。
4. 可选：重新导入 muban 刷新全库立案条件（生产慎用全量替换）。

**本地联调**：8080 / 3000 / 3003 / 9000；改 **timer/config** → `mvn clean install -pl smart-cityguard-timer,smart-cityguard-config -am -DskipTests` → 重启 8080。

---

## 2026-05-30 工作小结（续：菜单/计时规则 + 规划讨论）

### 当日摘要（代码变更）

1. **内容发布独立菜单**：从「业务配置」子项拆出，侧栏一级 **内容发布**（`/content/publish`）；旧路径 `/config/announcement` 重定向；权限仍为 ADMIN/SUPERVISOR（`RoleGroups.CONFIG`）。
2. **计时规则 / 隐去小类覆盖**：
   - 菜单「时限配置」→ **「计时规则」**；`TimeLimitConfig.vue` 去掉「小类覆盖」Tab，页顶说明处置时限在 **案件分类 → 立案条件**。
   - `CaseTimerService.resolveHandleTimeLimit`：**仅 `standardId`（立案条件）**，停用 `category_time_limit_override`。
   - 小类覆盖 API 标 `@Deprecated`；`CategoryManage.vue` 说明已更新。
3. **部门管理列表样式**：`DeptManage.vue` 树节点左右布局、登录标签样式优化。
4. **DEPT 角色名乱码**：新增 `database/patch_fix_dept_role_name.sql`；`patch_dept_login.sql` 加注释指引。

| 层 | 内容 |
|----|------|
| **前端·路由** | `router/index.js`：`/content` 一级菜单；`/config/announcement` → redirect |
| **前端·配置** | `TimeLimitConfig.vue`、`CategoryManage.vue` |
| **前端·系统** | `DeptManage.vue` 树样式 |
| **后端·timer** | `CaseTimerService` 仅立案条件解析处置时限 |
| **后端·config** | 小类覆盖接口 `@Deprecated` |
| **库** | `patch_fix_dept_role_name.sql` |

**改 timer/config 后**：`mvn clean install -pl smart-cityguard-timer,smart-cityguard-config -am -DskipTests` → 重启 8080。

### 当日讨论纪要（接续用）

| 话题 | 结论摘要 |
|------|----------|
| **待办事项是否等于工作完成** | 待办 = 按角色聚合的**案件待办队列**；清空 ≈ 本岗当前环节办完。不含：申诉、延期审批、任务台账、内容发布、领导查统计等；全系统结案 ≠ 待办为空。 |
| **国产化适配** | 相对好适配（Java + Vue H5）；主战场是 **MySQL → 达梦/金仓**（DDL + 原生 SQL）；Redis/RabbitMQ 未硬依赖；地图已是高德，若招标要天地图需抽象一层。 |
| **功能完善度 / 生产容量** | 主干 **可试运行**（~85% 案件流）；正式生产需验收 + 部署加固。单机 modest 配置粗估 **几十人同时在线、几百账号**；无压测，500+ 需 Redis/多实例/消息改推送。 |
| **完善路线规划** | 阶段 0 验收收口 → 1 试运行部署 → 2 回退链/状态机/待办并入任务 → 3 报表/菜单权限 → 4 Capacitor/消息实时 → 5 信创/压测（详见会话规划，不重复展开）。 |
| **手机端何时可测** | **现在即可**：采集端 H5（`:3003`）+ 手机浏览器 + 同 WiFi；需 `vite` 开 `host: true`（尚未改代码，明天可补）；高德 Key 配 `.env.local`。Capacitor 调试包约 1～2 周，不阻塞 H5 真机测。 |
| **上线节奏** | 用户表示**不着急上线**，优先继续完善功能；手机测试与功能开发可并行，不必等功能全部做完。 |

### 明天优先（接续）

1. **阶段 0 验收**：按 2026-05-28 清单 + 阶段计时抽测（立案/派遣/处置各转一次）。
2. **可选小改**：`collector-app/vite.config.js` 加 `host: true`，写一条手机 H5 联调说明（或入 `docs/startup-guide.md`）。
3. **阶段 2 起步**：回退链第一批（派遣/部门回退，见 `docs/case-workflow-spec.md` §11 P0）。
4. 改 **timer/config** 后确认已 `mvn install` 并重启 8080；管理端刷新看「内容发布」独立菜单与「计时规则」页。

**本地联调**：8080 / 3000 / 3003 / 9000。

---

## 2026-05-30 工作小结（阶段计时展示 + 部门消息权限）

### 当日摘要

1. **阶段计时展示修正**：待立案/待派遣案件不再误标为「处置截止」；列表增加 **计时阶段** + **阶段截止**；详情按 **受理 / 派遣 / 处置** 分行展示各阶段截止与剩余时限（`timerStages`、`stageDeadlineTime`）。
2. **根因说明**：上报即启动 **受理 15 分钟** 计时（`onCaseReported`），旧逻辑把当前阶段截止统一映射到 `deadlineTime` 且前端固定文案「处置截止时间」，造成歧义（例：`YC202605260001` 待立案却有处置截止）。
3. **部门消息铃铛**：`RoleGroups.MESSAGE` 补 **DEPT**，处置部门点击右上角铃铛可进 `/message/list`，不再跳转无权限页。
4. **环境**：四端 8080/3000/3003/9000 已重启；改 **timer/case** 后须 `mvn install` 再重启 8080。
5. **移动端打包（讨论）**：采集端仍为 Vite H5；Capacitor 打 Android 调试包可行（需 API 地址 + CORS），与 `npm run dev` 电脑联调可并行；用户有 Android Studio 环境，尚未落地壳工程。
6. **GitHub**：已推 `master`（`a331da3`）。

| 层 | 内容 |
|----|------|
| **后端·计时** | `CaseTimerStageDisplay`、`buildCaseTimerStages`；`CaseTimerDisplayInfo.timerStage/stageName/stageTimeout`；`CaseInfo.stageDeadlineTime`、`timerStages`；不再用受理/派遣计时覆盖 `case_info.deadline_time` |
| **后端·工作台** | 待办排序/展示用 `effectiveStageDeadline` + `timerStageName` |
| **前端·管理端** | `caseTimer.js`；`CaseDetail`/`CaseList`/`CasePending`/`dashboard` 阶段文案 |
| **前端·采集端** | `handle/index.vue` 展示阶段名 + 阶段截止 |
| **前端·权限** | `roleAccess.js`：`MESSAGE` 含 `DEPT` |

### 验证建议

| 项 | 要点 |
|----|------|
| 待立案案件 | 详情见「受理截止时间」；列表计时阶段为「受理」 |
| 待派遣 | 计时阶段为「派遣」 |
| 已派遣 | 处置阶段截止与 `case_info.deadline_time` 一致 |
| DEPT 账号 | 铃铛 → 消息列表可访问 |

### 明天优先（接续）

1. 阶段计时全链路抽测（立案/派遣/处置各转一次状态）。
2. 可选：Capacitor Android 调试包（`VITE_API_BASE_URL` + 后端 CORS）。
3. P1 联调验收（综合查询/考核统计/申诉）若尚未全测，继续按 2026-05-28 清单打勾。

**本地联调**：8080 / 3000 / 3003 / 9000；改 **timer/case** → `mvn clean install -pl smart-cityguard-timer,smart-cityguard-case -am -DskipTests` → 重启 8080。

---

## 2026-05-28 工作小结（综合查询 + 考核统计 + 处置超时申诉 + 工作台优化）

### 当日摘要

1. **P1 综合查询引擎**：管理端多条件分页检索上线；菜单在 **考核评价 → 综合查询**（`/evaluation/query`，旧 `/case/query` 重定向）。
2. **P1/P2 考核统计 MVP**：按处置部门聚合指标；支持数字反查全页列表；申诉通过后超时类指标排除 `handle_timeout_exempt=1`。
3. **P2+ 处置超时申诉（双审）**：部门提交 → 派遣员初审 → 受理员二审；一案一次；通过后统计豁免、界面保留「曾超时」痕迹。
4. **菜单/路由**：街道社区隐藏；**采集员管理**独立菜单 `/collector/index`（原 `/geo/collector` 重定向）。
5. **工作台优化**：
   - 移除快捷操作区；
   - 统计卡片增加 **日/周/月/年** 周期（`GET /case/dashboard/stats?period=`），点击卡片带周期跳转列表；
   - 今日提示 / 公文通告各只展示 **5 条**，其余从「更多」进入；
   - **全部待办** 改为聚合页 `/case/todos`（`GET /case/dashboard/todos/page`），按角色合并各待办队列，不再误跳单一「待核实」列表。
6. **环境**：已执行 `database/patch_case_appeal_timeout.sql`；四端 8080/3000/3003/9000 已启动；改 **case** 后须 `mvn install` 再重启 8080。
7. **GitHub**：已推 `master`（`27e2c59` 主功能；`602dae3` 工作台优化）。

| 层 | 内容 |
|----|------|
| **库** | `patch_case_appeal_timeout.sql`（`case_info.handle_timeout_exempt`、`handle_timeout_exempt_appeal_id`） |
| **后端·查询** | `CaseQueryCriteria`、`POST /case/query`；角色范围过滤 |
| **后端·统计** | `CaseReportService`、`POST /case/report/statistics`、`POST /case/report/drill`；`CaseReportMetricSql` |
| **后端·申诉** | `TimeoutAppealController` `/appeal/timeout/**`；`TimeoutAppealServiceImpl`；移除旧 `AppealController` |
| **后端·计时** | `CaseTimerService` 申诉通过后展示「曾超时（申诉通过，不计入考核）」；工作台超时排除豁免 |
| **后端·工作台** | `DashboardPeriodHelper`；`collectMergedDashboardTodoCases`；`/case/dashboard/todos/page` |
| **前端** | `CaseQuery.vue`、`evaluation/index.vue`、`AppealList/Detail`、`CaseDetail` 申诉、`CaseDashboardTodos.vue`、`dashboard/index.vue` 周期与布局 |
| **文档** | `docs/case-comprehensive-query-design.md`、`docs/case-appeal-timeout-design.md` |

### 当日早些时候（采集员地图，已推 `20eec40`）

- **P1 采集员地图**：`/geo/collector` 首版；选中采集员后地图仅显示该员 `reporter_id` 上报案件。

### 联调 / 验收清单（待用户全测）

| 模块 | 要点 |
|------|------|
| 综合查询 | 多条件组合、角色可见范围、分页 |
| 考核统计 | 部门行指标、点击数字反查、返回统计表 |
| 申诉 | DEPT 结案+曾超时 → 提交；DISPATCHER/ACCEPTOR 双审；通过后考核不计超时 |
| 采集员地图 | 列表、片区绑定、按员过滤案件点 |
| 工作台 | 统计周期切换；全部待办聚合页；提示/通告各 5 条 |

### 明天优先（接续）

1. 申诉 + 综合查询 + 考核统计 **全链路联调验收**（按上表逐项打勾）。
2. 考核统计可选：处置超时指标与 `case_timer_record.is_timeout` 完全对齐（当前部分仍用 `handle_finish_time` 与 `deadline_time` 比较）。
3. 采集员地图可选增强：任务点、按片区统计。
4. P0 小尾巴（可选）：挂账到期改库抽测。

**本地联调**：8080 / 3000 / 3003 / 9000；改 **case/appeal/timer** → `mvn clean install -pl smart-cityguard-appeal,smart-cityguard-case,smart-cityguard-timer -am -DskipTests` → 重启 8080。

---

## 2026-05-28 工作小结（P0 验收 + P1 采集员地图）— 归档

> 本节为当日较早记录；综合查询/考核统计/申诉见上一节。

### 当日摘要

1. **P0 延期/挂账**：全链路手工验收通过，路线图 P0 标记完成；代码已推 GitHub（`e08d04e`）。
2. **P1 采集员地图**：管理端 **地理信息 → 采集员管理**（`/geo/collector`）首版上线。
3. **交互优化**：点选采集员后，地图**仅显示该员上报案件**（按 `reporter_id` 过滤），未选中时不展示案件点。
4. **环境**：四端（8080/3000/3003/9000）已多次重启；改 **geo** 后须 `mvn install` 再重启后端。

| 层 | 内容 |
|----|------|
| **后端** | `GET /geo/collector-map/overview`；`CollectorMapService`；案件点位含 `reporterId` |
| **前端** | `CollectorManage.vue`：采集员列表、片区高亮、片区多选绑定、上报案件 marker |
| **路由** | `/geo/collector`；片区管理菜单文案改为「片区管理」 |

### 采集员地图验收

| 项 | 结果 |
|----|------|
| 采集员列表 + 搜索 | 通过 |
| 选中采集员高亮责任片区 | 通过 |
| 管理片区绑定（`setRespGridCollectors`） | 通过 |
| 选中采集员仅看其上报案件 | 通过 |
| 近 7/30/90 天筛选 | 通过 |

**本地联调**：8080 / 3000 / 3003 / 9000；改 **geo** → `mvn clean install -pl smart-cityguard-geo -am -DskipTests` → 重启 8080。

---

## 路线图（2026-05-28 更新）

| 优先级 | 模块 | 状态 |
|--------|------|------|
| **P0** | 延期 + 挂账（表、API、计时、DEPT 申请 + 派遣员待审、定时恢复） | **已联调，基本打通** |
| **P1** | 综合查询引擎、采集员地图页 | **均已首版落地**（待全链路验收） |
| **P2** | 统计报表 4-A MVP | **考核统计首版已落地**（反查、部门聚合）；其余报表待开发 |
| **P2+** | 申诉双审 + 超时剔除统计 | **处置超时申诉 MVP 已落地**（待联调验收） |

---

## 2026-05-27 延期 / 挂账联调验收

### 当日摘要

**P0 延期 + 挂账** 已手工跑通主流程，业务基本可用。处置部门在案件详情申请 → 派遣员在「延期挂账审批」列表或**案件详情页**批准/驳回 → 截止时间延长 / 挂账暂停计时 / 到期自动恢复，与 **`docs/case-adjustment-design.md`** 一致。

| 验收项 | 结果 |
|--------|------|
| DEPT 申请延期（`pending_handle` / `handling`） | 通过 |
| DEPT 申请挂账（**自选挂账截止日期**，最长 1 年） | 通过 |
| DISPATCHER 待审列表批准/驳回 | 通过 |
| DISPATCHER 案件详情页批准/驳回（`pendingExtensionApply` / `pendingSuspendApply`） | 通过（改 case 模块后须 `mvn install` 并重启 8080） |
| 延期批准后 `deadline_time` 延长 | 通过 |
| 挂账批准后不可处置、挂账提示 | 通过 |
| 挂账到期定时恢复（`CaseAdjustmentScheduler`） | 已落地（长周期需观察或改库测） |
| 驳回不占延期次数、挂账驳回可再申请 | 按设计 |

### P0 交付物（对照）

| 层 | 内容 |
|----|------|
| **库** | `database/patch_case_adjustment.sql`（`case_adjustment_apply`、`case_info` 扩展字段） |
| **后端** | `/case/adjustment/apply|pending|review|list/{caseId}`；`CaseTimerService.extendHandleDeadline` / `pauseHandleTimer` / `resumeHandleTimer` |
| **前端** | `CaseDetail.vue`（DEPT 申请 + 派遣员详情审批）；`AdjustmentReview.vue`；路由 `/case/adjustment-review` |
| **定时** | `CaseAdjustmentScheduler` 扫描 `is_suspended=1 AND suspend_until<=NOW()` |

### 联调备忘

- 子模块 **`smart-cityguard-case`**（及 timer）改代码后：`mvn clean install -pl smart-cityguard-case,smart-cityguard-timer -am -DskipTests`，再重启 **8080**；否则详情无 `pendingExtensionApply`、新接口 404 或旧计时逻辑。
- 管理端改 `CaseDetail.vue` 后 **Ctrl+F5** 或确认 `:3000` 开发服在跑。
- **下一步（P1）**：综合查询引擎、采集员地图页（片区 + 案件点）。

---

## 2026-05-25（续）延期 / 挂账首版

### 当日摘要

**处置部门申请 → 派遣员审批**；延期在当前 deadline 上 +1 个原处置时限（批准满 2 次）；挂账最长 1 年、批准 1 次、到期自动恢复；挂账期间不可处置操作。设计见 **`docs/case-adjustment-design.md`**；库补丁 **`database/patch_case_adjustment.sql`**。

| 层 | 内容 |
|----|------|
| **库** | `case_adjustment_apply`；`case_info` 增 `dispatch_operator_id`、`is_suspended`、`suspend_until`、`extension_approved_count` |
| **后端** | `CaseAdjustmentService`、`/case/adjustment/**`；`CaseTimerService.extendHandleDeadline`；定时恢复 `CaseAdjustmentScheduler` |
| **前端** | `CaseDetail` 部门申请；`AdjustmentReview.vue` 派遣员审批；路由 `/case/adjustment-review` |

**验证**：DEPT 登录案件详情申请 → DISPATCHER「延期挂账审批」批准 → 截止/挂账状态变化；改 **case/timer** 后 `mvn install` 重启 8080。

---

## 2026-05-25

### 当日摘要（接续用）

上午：工作台统计与待办对接真实数据；**内容发布**（今日提示 + 公文通告）；**工作台待办** `GET /case/dashboard/todos`；侧栏修复。下午：**案件分类**三栏 CRUD（大类/小类/立案条件）；采集端核查/核实附件与上传提示修复；案件列表状态中文、待核查/我立案过滤；**菜单管理**调研（占位未做）。**config 模块** install 后重启 8080 解决分类接口 404。本地 **8080 / 3000** 已重启。

---

### 1. 工作台：案件统计卡片

| 项 | 说明 |
|----|------|
| **接口** | `GET /case/dashboard/stats` → `CaseDashboardStatsDto`（待处理/处理中/已完成/超时/作废） |
| **作废统计** | `cancelled` 含 `not_accepted` + `cancelled`（业务作废多为 `not_accepted`） |
| **列表跳转** | `GET /case/list?statGroup=`：`pending` / `processing` / `completed` / `overdue` / `cancelled` |
| **前端** | `dashboard/index.vue` 卡片调 API 并点击跳转；`CaseList.vue` 支持 `statGroup` 查询参数 |

**关键文件**：`CaseServiceImpl.java`、`CaseController.java`、`CaseDashboardStatsDto.java`、`frontend/admin-web/src/api/case.js`。

---

### 2. 内容发布（通告 + 今日提示）

| 层 | 内容 |
|----|------|
| **可见性** | `all` / `role` / `user`（及 legacy `collector`/`admin`）；`ContentVisibilityHelper` |
| **用户侧** | `GET /message/announcement/list`、`/dailytip/latest`、`/dailytip/list`、详情 `/{id}` |
| **管理侧** | `GET /message/announcement/admin/list`、`POST/PUT/DELETE` 通告与今日提示（ADMIN/SUPERVISOR） |
| **前端** | `views/config/AnnouncementManage.vue`（双 Tab）；路由 `/config/announcement`；`api/config.js` |
| **模块** | `smart-cityguard-message`；改后需 **`mvn clean install -pl smart-cityguard-message -am -DskipTests`** 再重启 |

---

### 3. 首页：今日提示 & 公文通告交互

| 项 | 说明 |
|----|------|
| **详情** | `ContentDetailDialog.vue`；首页条目点击拉详情 |
| **浏览列表** | `/notice/dailytip`、`/notice/announcement`（`meta.hidden`，不进侧栏） |
| **更多** | 跳转浏览列表（非管理发布页）；所有登录用户可见 |
| **API 补充** | `getDailyTipList`、`getDailyTipDetail` |

---

### 4. 工作台：待办事项（P0 已落地）

| 项 | 说明 |
|----|------|
| **接口** | `GET /case/dashboard/todos?limit=10`（上限 30） |
| **逻辑** | 按角色聚合多路 `getPendingCases` 队列，去重，按 **截止时间**、上报时间排序 |
| **角色队列** | 受理员：待立案/核实/核查/结案等；派遣员：待派遣/把关/回退；部门：待指派/批转；处置人员：处置中；管理员/值班长：主要队列汇总 |
| **前端** | 表格展示；「处理」→ `/case/detail/{id}?action=process`；「全部待办」按 `defaultPendingTabForRoles` 跳对应待办页；监听 `cityguard:refresh-lists` |

**DTO**：`CaseDashboardTodoItemDto`、`CaseDashboardTodosDto`。

**未做（P1）**：核查/核实 **任务** 并入待办（`type=task`）；考核员/领导等无案件待办角色列表为空属正常。

---

### 5. 管理端侧边栏与其它修复

- **`BasicLayout.vue`**：去掉 `unique-opened`、菜单不再整树 `:key` 重建；子菜单展开后 `scrollbar` 更新；flex + `min-height: 0` 避免「内容发布」被裁切。
- **`App.vue`**：有 token 无 roles 时拉 `getUserInfo`。
- **`roleAccess.js`**：`mergeRouteMeta` 等。

---

### 6. 业务配置：案件分类管理（CRUD）

| 层 | 内容 |
|----|------|
| **后端** | `smart-cityguard-config`：`CategoryCatalogService` / `CategoryCatalogServiceImpl`；`CategoryCodeHelper`、`CategoryReferenceMapper`（删除前引用检查） |
| **接口** | `GET/POST/DELETE /config/category/big/manage`、`/config/category/big`；小类 `.../small/manage/{bigId}`、`.../small`；立案条件 `.../standard/manage/{smallId}`、`.../standard`（增删改需 **ADMIN**） |
| **前端** | `views/config/CategoryManage.vue`（三栏：大类 / 小类 / 立案条件）；`api/config.js` 对应方法；路由 **`/config/category`**（`RoleGroups.CONFIG`） |
| **与导入关系** | 批量导入仍走 **`POST /config/standard/import-muban`**（`StandardManage.vue`）；分类页用于日常增删改，不必每次导 Excel |

**排障**：管理端 `GET /api/config/category/big/manage?type=2` 若 **404**，多为 **`smart-cityguard-config` 未 `mvn install` 或 8080 未重启**（其它 `/config/**` 正常、新路径整段 404）。当日已 `install` 并重启，直连 8080 返回 200。

```powershell
Set-Location d:\smart_cityguard\backend
mvn clean install -pl smart-cityguard-config -am -DskipTests
mvn spring-boot:run -pl smart-cityguard-server -DskipTests
```

---

### 7. 案件流转与采集端联调修复

| 项 | 说明 |
|----|------|
| **核查/核实照片** | 采集端 `CheckTask.vue` / `VerifyTask.vue`：提交前 `syncAttachmentsFromFileList`；MinIO 失败用对话框 + 红色提示条；`uploadFeedback.js` + `skipErrorToast` 避免错误 Toast 一闪而过 |
| **后端核查** | `TaskServiceImpl`：核查通过须带附件；`applyAcceptorCollectCheckScope` 按 `assigner_id` 等匹配；`sendCheckTask` 无立案人时写 `register_operator_id` |
| **管理端展示** | `CaseDetail.vue`、`fileUrl.js` 预览与核查记录加载优化 |
| **我立案的案件** | `CaseServiceImpl`：排除 `not_accepted`、`cancelled` |
| **待核查列表** | 不再仅依赖 `register_operator_id`，增加核查任务下发人、流程记录匹配 |
| **状态中文** | `utils/caseStatus.js`；`CaseList` / `CasePending` / `CaseDetail` 统一显示「核查中」等（不再裸显 `checking`） |

**验证**：受理员下发核查 → 采集员带图提交 → 管理端详情可见核查图；作废案不出现在「我立案的案件」。

---

### 8. 系统管理：菜单管理（调研，未实现）

| 项 | 说明 |
|----|------|
| **现状** | `MenuManage.vue` 仍为「功能开发中」；库表 `menu`、`role_menu` 已建但**无种子数据**、无后端 API |
| **实际权限** | 管理端侧栏与路由守卫仍靠 **`router/index.js` + `roleAccess.js` 的 `RoleGroups`**，与库无关 |
| **若要做** | 建议 **MVP（档 A）**：菜单 CRUD + 角色勾菜单 + 侧栏按 `menu_code` 过滤，路由仍静态注册（约 2～3 天）；完全动态路由工作量大，暂不建议 |

---

### 9. 本地环境（当日）

| 服务 | 端口 | 状态 |
|------|------|------|
| 后端 | 8080 | 已重启（含 case / message / **config** 新接口） |
| 管理端 | 3000 | 已重启 |
| 采集端 | 3003 | 按需启动（测核查/上报时） |

```powershell
# 改 case 模块（统计/待办/列表过滤）后
mvn clean install -pl smart-cityguard-case -am -DskipTests

# 改 message 模块（内容发布）后
mvn clean install -pl smart-cityguard-message -am -DskipTests

# 改 config 模块（案件分类 CRUD）后
mvn clean install -pl smart-cityguard-config -am -DskipTests
```

---

### 10. 明天建议接续

1. **案件分类联调**：管理员登录 → **业务配置 → 案件分类**；增删改大类/小类/立案条件；再走采集端上报 / 管理端立案，确认下拉与计时用的 `small_id` 一致。删除前注意引用检查提示。
2. **核查全链路复测**：下发核查 → 采集端上传（故意失败看红色提示）→ 管理端详情看图 → 待核查列表是否出现。
3. **工作台待办**：受理员/派遣员/部门/处置人员分别登录，待办条数与「全部待办」跳转是否与各待办 Tab 一致。
4. **内容发布**：草稿/已发布、按角色/用户可见；首页与 `/notice/*` 过滤是否正确。
5. **计时（承接 05-24）**：新案核对 `deadline_time`、列表剩余时限；可选 **时限配置** 管理页、挂账暂停。
6. **待办 P1（可选）**：`GET /case/dashboard/todos` 并入核查/核实任务（`type=task`）。
7. **菜单管理（可选）**：若产品要可配置侧栏，按 §8 档 A 拆任务；否则继续用 `roleAccess.js` 即可。

---

## 2026-05-24

### 当日摘要（接续用）

采集端问题上报改为单页 + 下拉选类；**案件计时**首版落地（受理/派遣/处置/核查核实）；管理端案件列表增加**剩余时限 + 截止时间**；库补丁已执行、后端已重启。

---

### 1. 采集端：问题上报页重构

| 项 | 说明 |
|----|------|
| **文件** | `frontend/collector-app/src/views/report/index.vue`、`src/api/case.js` |
| **案件来源** | 移除「市民举报」；采集员端固定巡查上报（`source=1`，后端仍写 `sourceType=collector`） |
| **分类选择** | 先选 **部件/事件** → 大类 → 小类 → 立案条件；大类按 `GET /config/category/big/list?type=1\|2` 分表拉取 |
| **交互** | 取消三步向导，**单页**完成描述/照片/地图/提交；分类均为 **van-picker 下拉** |
| **提交** | `categoryType` 随选择传 `component` / `event`（不再写死 `event`） |
| **修复** | 底部「提交上报」`z-index: 10000` 挡住选择器 → 降为 100 + 打开 picker 时隐藏按钮 |

---

### 2. 案件计时（首版，考核以处置阶段为主）

| 层 | 内容 |
|----|------|
| **规则（已确认）** | 含「紧急」→ **连续计时**；不含「紧急」→ **仅工作时**（8–12、14–18，1 天=8 工作小时）；派遣至部门起算，部门批转派遣员止算；退回重办继续计、挂账暂停（挂账功能未做，接口预留） |
| **模板解析修正** | `MubanStandardImportService.parseHandleTime`：`紧急工作时`→`urgent_hour`，`X小时`→`work_hour` 等（与旧逻辑相反，**需重新导入 muban** 才刷新库内标准） |
| **模块** | `smart-cityguard-timer`：`DeadlineCalculator`、`CaseTimerService`、计时表 Mapper |
| **流转挂钩** | 上报→受理 15min；立案→派遣 15min；**派遣**→启动处置时限；**部门批转派遣员**→结算按时/超时；核查/核实任务截止 **30min** |
| **数据** | 写 `case_timer_record`；处置阶段同步 `case_info.deadline_time`、`time_limit_*`、`is_urgent` |
| **列表展示** | `CaseInfo` 增加 `timeRemaining`、`handleRemainingSeconds`、`handleTimeout`；列表接口 `applyTimerDisplay` |
| **实体对齐** | `TimeLimitRule` 与 `init.sql` 一致；新增 `category_time_limit_override`（小类覆盖，管理端配置页未做） |

**关键文件**：`backend/smart-cityguard-timer/**`、`CaseServiceImpl` 计时调用、`database/patch_case_timer.sql`、`database/init.sql`（新库含覆盖表与暂停字段）。

---

### 3. 管理端：案件列表时限展示

- **文件**：`CaseList.vue`、`CasePending.vue`、`utils/dateFormat.js`
- **列**：剩余时限（原有）+ **截止时间**（`deadlineTime`）；超时红色样式 `handleTimeout`
- **后端**：未派遣案件展示进行中的受理/派遣阶段截止（`resolveDisplayTimerRecord`）

---

### 4. 数据库与本地环境

| 动作 | 结果 |
|------|------|
| **补丁** | 已执行 `database/patch_case_timer.sql`：`category_time_limit_override` 表已建；`case_timer_record` 已加 `pause_start_time`、`total_paused_seconds`（MySQL 8 不支持 `ADD COLUMN IF NOT EXISTS`，暂停列已手工 ALTER 成功） |
| **编译** | `mvn clean install -pl smart-cityguard-timer,smart-cityguard-config,smart-cityguard-case,smart-cityguard-server -am -DskipTests` 通过 |
| **后端** | 8080 已重启；`/doc.html` 200 |
| **MySQL** | `application.yml`：`root` / 库 `cityguard` |

```powershell
# 改 timer/case 后
Set-Location d:\smart_cityguard\backend
mvn clean install -pl smart-cityguard-timer,smart-cityguard-case -am -DskipTests
mvn spring-boot:run -pl smart-cityguard-server -DskipTests
```

| 服务 | 端口 |
|------|------|
| 后端 | 8080 |
| 管理端 | 3000 |
| 采集端 | 3003 |
| MinIO | 9000 |

---

### 5. 明天建议接续

1. **计时联调**：新案件走完整链路（上报→立案→派遣→处置→部门批转），核对列表/详情 **剩余时限、截止时间、超时标记**；旧案件无 `case_timer_record` 可能显示 `--`。
2. **重新导入 muban**（若要用修正后的时限解析）：管理端 → 立结案标准；注意生产库勿随意全量替换。
3. **管理端「时限配置」页**：`TimeLimitConfig.vue` 仍为占位——需做工作时段/节假日/全局规则 + **小类时限覆盖** CRUD。
4. **挂账**：业务功能 + 调用 `pauseHandleTimer` / `resumeHandleTimer`。
5. **单案延期**：下一期，不在时限配置里做。
6. **采集端**：处置列表可展示 `timeRemaining`（目前主要用 `deadlineTime`）；案件详情页可补时限区块。
7. **考核统计**：`evaluation_*` 表 `timeout_count` 等字段尚未对接计时结果。

---

## 2026-05-21

### 当日摘要（接续用）

P0 API 回归通过；P1 核查/核实主干 + 受理员选人地图 + 核查照片展示 + 站内消息提醒（轮询）已落地。本地四端（8080/3000/3003/9000）已拉起。

---

### 1. P0 联调回归（API 自动化）

- **脚本**：`scripts/p0-regression.ps1` + `scripts/p0-users.json`；密码 **`admin123`**。
- **结果**：**17/17 PASS**（主干 A1–A8 + 回退/守卫 D 组）。
- **手测**：`docs/p0-regression-checklist.md`（浏览器按钮文案部分仍待确认）。

---

### 2. P1 核查 / 核实（可选分支）

| 项 | 说明 |
|----|------|
| **后端** | `POST /case/send-check`、`/case/send-verify`；`POST /task/check/execute`、`/task/verify/execute`；`CaseTaskCompletionHandlerImpl` 回写案件状态 |
| **采集员指派** | `GET /case/{id}/collector-candidates`（距离推荐 + 责任片区；位置为**最近上报坐标**） |
| **管理端** | 发送核查/核实弹窗（地图 + 采集员单选）；`collectorUserId` 可选 |
| **采集端** | `CheckTask.vue` / `VerifyTask.vue`；任务 API 路径已对齐 `/task/check/*`、`/task/verify/*` |
| **改子模块后** | `mvn clean install -pl smart-cityguard-case,smart-cityguard-task -am -DskipTests` 再启 8080 |

**受理员权限（核查中可看案）**：`checking` 状态纳入 `canAcceptorViewCase` / `applyAcceptorCaseScope`；下发人、流程操作人可查。

**踩坑**：`jdbcTemplate.query` 责任片区采集员列表误用 `ResultSetExtractor` → 改为 `queryForList`（「Before start of result set」）。

---

### 3. 核查 / 核实现场照片

- **落库**：采集员提交时写入 `check_attachment` / `verify_attachment`（此前未持久化）。
- **管理端展示**：并入 **「现场照片/视频」** 同一区域；图下标注「核查照片」「核实照片」（无单独卡片）。
- **采集端修复**：上传接口返回 `res.data` 为字符串，修正 `CheckTask.vue` / `VerifyTask.vue` 误用 `res.data.url` 导致附件 URL 为空。
- **接口**：`GET /case/{id}/check-task-records`、`/verify-task-records`（带附件列表）。

**说明**：修复前已提交的任务库中无附件，需重新下发并上传才有图。

---

### 4. 站内消息提醒（第一期 MVP）

| 层 | 内容 |
|----|------|
| **约定** | **新案上报 → 通知所有 `ACCEPTOR` 角色用户**（用户确认） |
| **后端** | `UserNotificationSender` SPI + `user_message` 表字段对齐 `init.sql`；案件流转节点写消息（立案→派遣员、派遣→部门、指派→HANDLER、核查/核实→采集员、任务完成→下发人等） |
| **接口** | `/message/unread/count`、`/unread`、`/list` 等改为 **JWT 当前用户**（不再传 `userId`） |
| **管理端** | 顶栏铃铛角标；约 **25s** 轮询 + `ElNotification`；`cityguard:refresh-lists` 刷新案件/待办列表 |
| **采集端** | 登录后轮询 + Vant 通知；任务列表自动刷新 |
| **库表** | 若消息插入失败，执行 `database/patch_user_message_align.sql` |

**未做**：WebSocket 秒推、手机系统推送（留第二期）。

---

### 5. 产品讨论（未开发）

- **手机实时定位 / 轨迹回放**：当前路线（H5 → Capacitor App）可行；持续定位 + 轨迹表 + 管理端回放需 P2/P3；H5 仅适合「点一下定位」。

---

### 6. 本地环境（当日末）

| 服务 | 端口 | 备注 |
|------|------|------|
| 后端 | 8080 | 含 case + task + message；改模块后需 `install` |
| 管理端 | 3000 | Vite 热更新一般即可 |
| 采集端 | 3003 | 同上 |
| MinIO | 9000 | 上传/预览依赖 |

```powershell
Set-Location d:\smart_cityguard\backend
mvn clean install -pl smart-cityguard-case,smart-cityguard-task,smart-cityguard-message -am -DskipTests
mvn spring-boot:run -pl smart-cityguard-server -DskipTests

Set-Location d:\smart_cityguard\frontend\admin-web; npm run dev
Set-Location d:\smart_cityguard\frontend\collector-app; npm run dev
```

---

### 7. 明天建议（优先级）

1. **手测消息提醒**：双浏览器（受理员 + 采集员）上报新案 → 受理员弹窗/角标/列表刷新；再走立案、派遣、下发核查全链路各角色是否收到。
2. **手测核查照片**：采集员重新提交带照片的核查 → 管理端「现场照片/视频」是否出现「核查照片」。
3. **消息表**：若无消息写入，执行 `database/patch_user_message_align.sql` 后重试。
4. **P1 浏览器回归**：按 `p0-regression-checklist.md` §P1 勾选发送核查/核实、采集员执行、受理员仍可见案。
5. **可选**：消息中心页完善；WebSocket 设计草图；`MessageList` 与菜单入口是否在侧栏展示。

**关键文件（今日）**：

- `CaseServiceImpl.java`、`CaseTaskCompletionHandlerImpl.java`、`TaskServiceImpl.java`
- `UserNotificationSender*.java`、`MessageController.java`
- `admin-web/.../CaseDetail.vue`、`composables/useMessagePoll.js`、`layouts/BasicLayout.vue`
- `collector-app/.../CheckTask.vue`、`VerifyTask.vue`、`composables/useMessagePoll.js`

---

## 2026-05-20

### 案件流程：规格定稿 + P0 落地 + 服务重启

#### 1. 业务规格（已定稿）

- **主文档**：`docs/case-workflow-spec.md`（六步闭环、批转/回退、核查 vs 核实、多账号、分期）。
- **核心约定**：
  - **核查**（立案前，`check_task`）与 **核实**（结案前，`verify_task`）分离，均非每案必走。
  - **批转**推进流程，**回退**仅修正；回退不清空已填信息。
  - 受理员认领：首次立案/作废写入 `register_operator_id`；立案人全程可查，收尾受理员可指定他人。
  - 派遣员不绑定立案人，以当班 `current_handler` 为准。
  - 部门回退派遣员后，派遣员须先再派部门，才能批转受理员。
  - 受理员处置不达标 → 当班派遣员 → **原部门** `pending_handle` 返工。
  - 立案/结案须选立结案标准（`standardId`）；管理员强制操作暂缓（P2）。

#### 2. P0 已实现（代码已提交工作区，后端已 install + 重启）

| 层 | 内容 |
|----|------|
| **后端** | 6 个新回退/撤销接口；`CaseFlowOperateType`；`case_flow_record` 写入 `operate_type` / `receiver_id`；`checkCase` 停用；`closeCase`/`registerCase` 校验 `standardId`；`returned` 可再立案批转 |
| **管理端** | `CaseDetail.vue`：撤销指派、打回处置人、派遣员回退受理员/返工部门、受理员回退派遣员；去掉旧「核实通过/不通过」 |
| **采集端** | `HandleDetail.vue`：处置前「回退处置部门」（理由必填） |
| **API 清单** | `POST /case/dept-revoke-assign`、`dept-return-handler`、`handler-return-dept`、`dispatcher-return-acceptor`、`dispatcher-return-dept`、`acceptor-return-dispatcher` |

**关键文件**：`CaseServiceImpl.java`、`CaseController.java`、`CaseFlowOperateType.java`、`admin-web/.../CaseDetail.vue`、`collector-app/.../HandleDetail.vue`、`admin-web/src/api/case.js`。

#### 3. 本地环境（当日末）

- 已拉起：**8080** 后端、**3000** 管理端、**3003** 采集端、**9000/9001** MinIO（`minioadmin` / `D:\minio-data`）。
- 改 case 模块后惯例：`mvn clean install -pl smart-cityguard-case -am -DskipTests` 再启 8080。

#### 4. 明天建议（接续优先级）

1. **P0 联调回归**（建议按角色各走一条）：
   - 主干：采集上报 → 受理立案 → 派遣 → 部门指派 → HANDLER 处置 → 部门确认 → 派遣员批转受理员 → 结案。
   - 回退：派遣员回退受理员 → 受理员再立案批转；部门撤销指派后回退派遣员；部门打回 HANDLER；HANDLER 回退部门；派遣员返工部门；受理员回退派遣员返工。
   - 守卫：部门回退后，派遣员直接「批转受理员」应被拒绝。
2. **菜单/文案**：受理员「待核实案件」菜单与旧 `acceptorMode=verify` 路由是否需改为仅核查任务（P1 前可先改文案避免误解）。
3. **P1 开发**（见 `case-workflow-spec.md` §11）：
   - 发送核查 / 发送核实（替换占位按钮）+ 最近采集员指派 + 采集端任务执行页。
   - 列表/详情 **六步 stage** 展示。
4. **可选**：补 HANDLER 测试账号；旧 `pending_check` 误状态数据清理。

#### 5. 本地启动（PowerShell）

```powershell
# 后端（改 case 后先 install）
Set-Location d:\smart_cityguard\backend; mvn clean install -pl smart-cityguard-case -am -DskipTests; mvn spring-boot:run -pl smart-cityguard-server -DskipTests

# 管理端 / 采集端
Set-Location d:\smart_cityguard\frontend\admin-web; npm run dev
Set-Location d:\smart_cityguard\frontend\collector-app; npm run dev

# MinIO（若未起）
$env:MINIO_ROOT_USER='minioadmin'; $env:MINIO_ROOT_PASSWORD='minioadmin'
& "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\MinIO.Server_Microsoft.Winget.Source_8wekyb3d8bbwe\minio.exe" server D:\minio-data --console-address ':9001'
```

---

## 2026-05-19

### 案件权限、受理员/派遣员菜单（当日接续）

#### 案件主干（批转闭环）

- **流程**：HANDLER 提交 → **`handle_finish`** → DEPT **`POST /case/dept-confirm`** → 派遣员把关 → **`POST /case/dispatcher-forward-acceptor`**（必选 `acceptorUserId`）→ 指定受理员 **核实/结案**；DEPT 不可核实/结案。
- **关键后端**：`CaseServiceImpl`（`handleCase` / `deptConfirmCase` / `dispatcherForwardToAcceptor` / `assertAcceptorCheckCloseOperator`）；`CaseController` 新路由。
- **关键前端**：`CaseDetail.vue`（批转受理员、核实/结案按钮）；`collector-app` 处置端按角色分流。

#### 受理员：可见范围与菜单

- **规则**：
  - **公共池**：`register_operator_id` 为空且状态为 `reported` / `pending_register` / `pending_verify` → 所有受理员可见、可立案/作废（先接手者优先，`register_operator_id` 乐观写入）。
  - **我立案**：`register_operator_id = 本人`（或流程节点 **「立案并批转」** 的操作人）→ 全程可查，含已批转他人收尾、已结案；**不能**代替他人核实/结案。
  - **待我结案**：`pending_check` 且 `current_handler_id = 本人` → 仅 **结案**（从该菜单进详情只显示结案按钮）。
  - **待核实案件**：原上报待核实 + 核实任务 + **批转给自己的 `pending_check`** → 仅 **核实**。
- **01 立案、02 收尾**：立案人 01 在「我立案的案件」全程只读跟踪；02 在「待我核实/结案」办核实/结案（菜单已改名为 **待我结案案件**）。
- **路由**：`/case/my-registered`（我立案的案件）、`/case/pending-close`（待我结案案件）；详情通过 `?acceptorMode=verify|close` 区分按钮。
- **历史数据**：`YC202605190001` 等早期立案未写 `register_operator_id` 导致列表不可见 → 已执行 `patch_case_register_operator.sql` 回填逻辑；代码侧增加流程 **「立案并批转」** 兜底识别。

#### 派遣员：可见范围与菜单

- **案件列表 / 详情**：不再显示全部案件；仅 **当前处理人为本人** 或 **流程中本人曾操作**（派遣、批转受理员等）。
- **子菜单**（不变）：待派遣、待办审核、回退案件（按 `current_handler_id`）；**经办案件**（`dispatcher_handled`，按 `case_flow_record.operator_id`）。

#### 其它

- **作废**：`rejectCase` 写入 `register_operator_id` 并校验未被他人接手。
- **管理员**：案件列表 **批量删除** `POST /case/delete`。
- **主要改动文件**：`CaseServiceImpl.java`、`CaseController.java`、`router/index.js`、`CasePending.vue`、`CaseDetail.vue`、`database/patch_case_register_operator.sql`。

#### 本地验证要点

1. 受理员01 / 02 切换登录：公共池、我立案、待我结案列表互不误入。
2. 派遣员01：案件列表无无关案件；经办案件可见 `YC202605190001`（已结案）。
3. 改 case 模块后：`mvn clean install -pl smart-cityguard-case -am -DskipTests` 再启 **8080**。

### 本地环境与服务

- 已拉起：**后端 8080**、**管理端 3000**、**采集端 H5 3003**、**MinIO 9000/9001**（`minioadmin` / `minioadmin`，数据目录 `D:\minio-data`）。
- MinIO 路径（WinGet）：`...\MinIO.Server_Microsoft.Winget.Source_8wekyb3d8bbwe\minio.exe`。
- 改 **`smart-cityguard-case`** 后须 **`mvn clean install -pl smart-cityguard-case -am -DskipTests`** 再启后端，否则新接口（如 `/case/dept-confirm`）可能 404。

### 部门案件列表权限（DEPT）

- **问题**：部门账号在「案件列表」看到全部案件。
- **修复**：`GET /case/list`、`GET /case/{id}` 对 **DEPT** 按 `handle_dept_id = 当前用户部门` 过滤；详情越权返回「您无权查看该案件」。

### 移动端：处置人员（HANDLER）专属界面

- **问题**：HANDLER 登录 `collector-app` 仍为采集员首页/上报/任务。
- **实现**：
  - `stores/user.js` 保存 `roles`；`utils/roleAccess.js` 分流。
  - 纯 HANDLER：Tab **待处置 + 我的**，登录进 `/handle`；双角色（HANDLER+COLLECTOR）保留上报/任务。
  - 新页：`views/handle/index.vue`（待办列表）、`HandleDetail.vue`（处置提交）。
  - API：`getPendingCaseList(status=handling)`、`handleCase`、`getCaseAttachments`。
- **处置详情增强**：`components/CaseLocationMap.vue`（高德地图+导航）；上报照片经 **`/api/file/preview`** 带 Token 展示（`utils/fileUrl.js`），解决 MinIO 直链不显示。

### 案件流：处置完成 → 部门确认 → 派遣员核实/结案

- **业务**：
  1. HANDLER 提交处置 → 状态 **`handle_finish`**（界面：**处置人员已处置**），反馈处置部门。
  2. **DEPT** 在详情点 **「批转派遣员」**（`POST /case/dept-confirm`）→ **`pending_check`**，指定派遣员。
  3. **派遣员** 核实/结案；**DEPT 不能核实、不能结案**（后端 `assertNotDeptOperatorForCheckClose`）。
- **后端**：
  - `handleCase` 改为进入 `handle_finish`（不再直接 `pending_check`）。
  - 新 DTO/接口：`CaseDeptConfirmRequest`、`POST /case/dept-confirm`。
  - 详情字段 **`awaitingDeptConfirm`**（非表字段）：兼容旧数据（`pending_check` + 处置完成时间 + 当前处理人仍为 HANDLER）。
  - 待办 Tab：`dept_confirm_todo`；派遣员 `pending_check` 按 `current_handler_id` 过滤。
- **管理端**：`CasePending` 增「处置人员已处置」Tab，DEPT 默认进该 Tab；`CaseDetail` 批转按钮依赖 `awaitingDeptConfirm`。
- **联调注意**：`handle_dept_id` 须与部门登录账号的 `department_id` 一致（一级中心如「市容环卫中心」id=100）。

### 验证路径（全链路）

1. 采集上报 → 受理员A 立案批转派遣员 → 派遣至部门 → 部门指派 HANDLER → HANDLER 提交处置。
2. 部门 **批转派遣员** → 派遣员 **批转受理员B** → B 在「待我结案案件」结案；A 在「我立案的案件」仅查看。
3. 部门/派遣员/受理员案件列表均按角色过滤；管理员可见全部。

### 明天建议接续

- 补全各中心 **HANDLER** 测试账号；全链路回归批转受理员下拉（默认立案人）。
- 结案考核字段：可选增加 `close_operator_id` 与立案人分开统计。
- 旧数据：批量执行 `patch_case_register_operator.sql`；清理误状态 `pending_check` 案件。
- 部门退回处置、发送核查（占位）落地；MinIO 未启动时的上传提示。

---

## 2026-05-17

### 部门登录账号与案件分派（二级平台）

- **业务**：部门本身可作为账号登录；派遣员将案件派至 **处置部门** 后，由 **部门账号** 在「部门待指派」中分派给本部门 **处置人员（HANDLER）**，处置人员仅在「处置中」提交结果。
- **角色**：新增 **`DEPT`（部门账号）**；`HANDLER` 仅负责现场处置，不再具备部门待指派权限。
- **后端**：
  - `sys_department.login_user_id`；创建/补全部门账号 **`ensureDeptLogin`**、**`resetDeptLoginPassword`**（`SystemController`）。
  - 新建二级部门时自动创建登录用户（用户名优先 **部门全称**，冲突则用 `dept_{id}`）；**一级部门（中心/队伍）** 也可手动「创建登录账号」。
  - 登录支持 **用户名** 或 **部门名称**（`AuthServiceImpl.resolveLoginUser`）。
  - 部门账号不可在用户管理中增删改（`deptLoginAccount`）；`CaseServiceImpl` 待办与 `assign-handler` 权限按 `DEPT` 收紧。
- **前端**：`roleAccess.js` 增加 `DEPT`；`DeptManage.vue` 展示登录名/创建账号/重置密码；`CaseDetail.vue` / `CasePending.vue` 权限与默认 Tab 调整；`UserManage.vue` 隐藏 `DEPT` 角色选项。
- **数据库补丁**：
  - `database/patch_dept_login.sql` — `DEPT` 角色、`login_user_id` 列；
  - `database/patch_restore_dept_login.sql` — 示例处置科账号（已废弃）；
  - `database/patch_user_dept_accounts.sql` — **当前在用 5 个一级部门** 的部门账号（见下表）。
- **当前部门登录（密码均为 `admin123`，用户名=部门全称）**：监督指挥中心、采集队伍、园林绿化中心、综合行政执法队、市容环卫中心。示例「处置科」10/11/12 已从库中标记删除。
- **本地坑**：
  - PowerShell 不能用 `<` 重定向 SQL，需 `cmd /c "..."` 或 `Get-Content ... | mysql.exe`；
  - `mysql` 未进 PATH 时用 **`C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`** 全路径；
  - PowerShell 传 BCrypt 时 **`$` 会被展开**，更新密码须用单引号或变量包一层；
  - 补丁中无效哈希 `$2a$10$EqKcp1WFKVQ...` 会导致「密码错误」，应使用与 `admin` 一致的 **`$2a$10$3b.WiFgvOKOT4D956hQbpeJjEyBZ6pVyBpE4lW0IlFl5CqaIavFPW`**（`admin123`）。
- **验证**：管理员 `admin` 立案→派遣至「市容环卫中心」等 → 用部门名登录 → 「部门待指派」选人 → 处置人登录「处置中」提交。

### 关联历史（本日前后仍在用的能力）

- 受理员立案指定派遣员、派遣员待派遣队列、`POST /case/assign-handler`、用户管理左树右表、系统管理员不可归属部门等（见此前会话，未单独成节时可从 git 变更追溯）。

### 下次建议接续

- 为各中心在 **用户管理** 下维护真实 **HANDLER** 处置人员，并与派遣目标部门一致。
- 采集队伍若不做案件分派，可不在待办中期望有单；按业务决定是否从「可创建部门账号」列表排除。
- 联调：部门停用、删部门时级联删除部门账号、案件 `handle_dept_id` 与部门 id 对齐。

---

## 2026-05-15

### 立结案标准：muban 模板与导入（已落地）

- **规范**：`docs/case-standard-muban-spec.md`（部件/事件两 Sheet、9 列、续行规则、库表映射、物理清理策略）；根目录 **`muban.xlsx`** 为约定模板。
- **后端**：`POST /config/standard/import-muban`（`multipart` 字段 **`file`**），**仅 `ROLE_ADMIN`**（`@PreAuthorize`）；**`MubanStandardImportService`**（Apache POI）解析 Excel；**`StandardCatalogCleanupMapper`** 按 `category_type` **物理 DELETE** 关联表后重建大类/小类/标准。
- **实体对齐**：**`CaseStandard`** 与 `init.sql` 中 **`case_standard`** 一致；`condition_desc` → 字段 **`conditionContent`**（JSON 与采集端一致）。**`CategorySmall`** 补充 **`categoryType`、`fullCode`、`superviseSubject`、`responsibilitySubject`**；**`handleDays`/`checkDays`** 标 **`exist = false`**（库无列）。
- **鉴权**：**`JwtAuthenticationFilter`** 查 **`sys_role_user`** 填充 **`LoginUser.roles`** 并生成 **`ROLE_*`**，否则方法级 `@PreAuthorize` 不生效。
- **模块依赖**：**`smart-cityguard-config`** 增加 **`smart-cityguard-auth`**、**`poi-ooxml`**。
- **管理端**：**`业务配置 → 立结案标准`**（`StandardManage.vue`）管理员上传、展示各 Sheet 导入统计；**`src/api/config.js`** → **`importMubanStandard`**。
- **本地坑**：`spring-boot:run` 仍从 **`.m2` 旧 JAR** 加载子模块，新接口易 **404**；改 **`geo` / `config` / `auth`** 后需 **`mvn clean install -pl <模块> -am -DskipTests`** 再启后端。已写入 **`.cursor/rules/project-memory.mdc`** 排障说明。

### 文档与启动说明

- **`docs/startup-guide.md`**：精简为本地调试「按顺序启动」版。
- **`README.md`**：指向启动手册与立结案规范文档。

### 明天建议接续

- **联调**：管理员导入 **`muban.xlsx`** 后，走 **采集上报 / 管理端立案** 的大类→小类→立案条件链路；若库表与 **`init.sql`** 不一致先迁库。
- **运维**：生产库若已有案件引用旧 **`small_id`**，勿随意执行导入（会先删再插）；可考虑二期 **增量/软删** 策略。

---

## 2026-05-12

### 地理信息：片区（责任网格）与采集员

- **片区 CRUD / GeoJSON 导入**：`RespGridServiceImpl`、`GeoController`（`/geo/resp-grid/**`）；导入时 `AREA-xxxx` 编码改为 **数值 MAX + 批内递增**，避免同一事务内重复键冲突。
- **404 排查与合并**：片区接口并入 **`GeoController`**（与 `/geo/street/list` 同控制器注册），避免独立 Controller 未进运行包时 **`/geo/resp-grid/list`** 映射缺失。
- **管理端网格页**：`frontend/admin-web/src/views/geo/GridManage.vue`（高德多边形、导入/编辑）；修复 **`dialogVisible.value`**、多边形与列表用 **`extData.areaId`** 对齐高亮；`vite.config.js` 为 **`preview`** 增加与 dev 一致的 **`/api` 代理**。
- **采集员下拉**：对齐 **`SysUser`** 的 **`username` / `realName`**；**`GET /system/user/list`** 增加可选 **`roleCode`** 筛选（`SystemServiceImpl` 中 `EXISTS` 子查询 + 参数规范化），管理端传 **`COLLECTOR`** 只列采集员。
- **片区 ↔ 采集员多对多**：新表 **`responsibility_grid_collector`**；**`PUT /geo/resp-grid/{id}/collectors`**（Body：用户 ID 数组，空数组清空）；**`GET /geo/resp-grid/collector/{userId}`** 改为返回 **`List<ResponsibilityGrid>`**；移除原 **`POST .../assign`**、**`DELETE .../unbind`**；删除片区时先删关联行；实体增加非表字段 **`collectorUserIds`** 由服务填充。
- **数据库补丁**：`database/patch_resp_grid_collectors.sql`（建表、`INSERT IGNORE` 迁移原 `user_id`、再清空 `user_id`）；兼容 MySQL Workbench **安全更新模式**：最终 **`UPDATE`** 使用 **`WHERE id > 0 AND user_id IS NOT NULL`**（含主键条件）。
- **前端 API**：`frontend/admin-web/src/api/geo.js` 使用 **`setRespGridCollectors`**；分配对话框 **多选**、列表展示多采集员标签、「清空采集员」。
- **验证建议**：在库 **`cityguard`** 执行上述补丁后 **`mvn clean package -pl smart-cityguard-server -am -DskipTests`** 并重启后端；管理端「地理信息 → 网格管理」验证列表、多选保存、清空与 GeoJSON 导入。勿将数据库密码写入本文件。

---

## 2026-05-08

### 后端：路径变量误匹配导致 `Long` 转换失败（`:id` / `list`）

- **现象**：日志 `Failed to convert ... Long; For input string: ":id"`（或曾把 `list` 当成 id），多为 **通配路径 `/{id}` 抢在固定路径之前**，或非法路径段被强转 `Long`。
- **改动**：
  - `CaseController`：`/{id}`、`/{id}/flow`、`/{id}/attachments` 改为 **`/{id:\\d+}`**，仅匹配数字案件主键。
  - `ConfigController`：小类列表、立案条件、时限的 path 变量改为 **`:\\d+`**。
  - `TaskController`：**先注册** `/verify/list`、`/check/list`，再注册 `/verify/{id:\\d+}`、`/check/{id:\\d+}`，避免 `list` 被当成任务 id。
- **验证**：`mvn -pl smart-cityguard-server -am compile` 通过；重启后端后再次「提交上报」与任务列表接口应正常。

### 采集端：提交上报「无反应」修复

- **现象**：点击「提交上报」无反馈，多为底部 **TabBar / 安全区** 与固定按钮区重叠，点击被导航栏吃掉；或仅触发校验但普通 Toast 不明显。
- **改动**（`frontend/collector-app/src/views/report/index.vue`）：
  - 底部按钮区 `bottom` 改为 `calc(50px + env(safe-area-inset-bottom) + 6px)`，`z-index: 2100`，并加阴影；页面 `padding-bottom` 加大，避免遮挡。
  - 地图容器 `:deep(.amap-container)` 限制 `z-index`，避免盖住操作栏。
  - 提交前校验改用 **`showFailToast`**（红色）；提交中 **`showLoadingToast`**；成功 **`showSuccessToast`**。
  - 提交前尝试 **`getUserInfo`** 补全 `reporterId`；校验经纬度 `Number.isFinite`。
  - 提交按钮 `native-type="button"`、`@click.stop.prevent`。
- **验证**：手机或 Chrome 设备模式打开上报第三步，确认「提交」在导航栏之上可点；故意漏填项应出现红色失败提示。

### 执行数据库补丁：测试小类

- **动作**：在本机 MySQL 对库 `cityguard` 执行 `database/patch_add_test_category_small.sql`（成功，`exit code 0`）。
- **结果**：事件类「市容环境」(`big_id=6`, `big_code=01`) 下已存在小类编码 `99` — **测试小类（联调）**，用于采集端上报联调。
- **说明**：补丁可重复执行（`ON DUPLICATE KEY UPDATE`）。若需自行执行，PowerShell 可参考：
  ```powershell
  $sql = Get-Content 'd:\smart_cityguard\database\patch_add_test_category_small.sql' -Raw
  $sql | & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' --host=127.0.0.1 --user=root '--password=<你的密码>' cityguard
  ```
- **相关文件**：`database/patch_add_test_category_small.sql`、`database/init.sql`（新库初始化已含同逻辑插入）。

### 高德与采集端（历史摘要，便于连贯）

- **采集端**：`frontend/collector-app` 上报第三步嵌入高德地图点选、`VITE_AMAP_KEY` / `VITE_AMAP_SECURITY_JS_CODE` 从环境变量读取；本地配置见 **`.env.local`**（已 `.gitignore`，勿提交）。
- **提交上报**：加强校验（含必选小类、与库 `NOT NULL` 一致）、附件删除与 `attachments` 同步、提示 `position: 'top'` 等。
- **测试小类数据**：除本次执行补丁外，`init.sql` 已写入同条测试小类，新建库自带。

---

## 使用说明

- 后续每次完成「可交付」的改动：在本文件 **顶部** 增加一节 `## YYYY-MM-DD`，用简短条目写 **做了什么 / 影响范围 / 如何验证**。
- 与数据库、密钥相关的操作：**不要**把密码、Key 写进本文件；可写「已写入 `.env.local`」或「使用 application.yml 中默认库名」等。

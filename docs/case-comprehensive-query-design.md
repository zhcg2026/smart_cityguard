# 综合查询设计（MVP）

## 权限

| 角色 | role_code | 数据范围 |
|------|-----------|----------|
| 管理员 | ADMIN | 全库 |
| 值班长 | SUPERVISOR | 全库 |
| 领导 | LEADER | 全库 |
| 受理员 | ACCEPTOR | 条件全库，结果按「可查看案件」过滤 |
| 派遣员 | DISPATCHER | 同上 |

## 查询字段

| 项 | 字段 | 说明 |
|----|------|------|
| 任务号 | case_code | exact / prefix |
| 上报时间 | report_time | eq / gt / lt / between；eq 按自然日 |
| 结案时间 | close_time | 同上；**eq 排除未结案**（close_time IS NULL） |
| 问题来源 | case_origins | 多选：采集员上报、市民投诉、电话投诉、领导交办、公众举报、视频上报（受理登记按 source_desc 匹配，不含「案件登记/部门批转」渠道项） |
| 所属区域 | resp_grid_id | 多选（责任网格） |
| 问题状态 | case_status | 多选；「作废」合并 not_accepted+cancelled；「已结案」含历史 forced_close |
| 事部件类型 | category_type | component / event |
| 问题类型 | small_id | 先选类型、大类，再多选小类 |
| 问题描述 | description | eq / contains |
| 处置部门 | handle_dept_id | 单选 |
| 采集员 | reporter_id | 单选 |
| 受理员 | register_operator_id | 立案时写入 |
| 派遣员 | dispatch_operator_id | 派遣至部门时写入 |
| 地址 | address | eq / contains |

## 接口

- `POST /case/query`，Body：`CaseQueryCriteria`
- 返回分页 `Page<CaseInfo>`（含时限展示字段）

## 前端

- 路由：`/evaluation/query`（**考核评价 → 综合查询**，与「考核统计」并列）
- 旧路径 `/case/query` 重定向至 `/evaluation/query`
- 父菜单 `EVALUATION_SECTION`；子项角色 `RoleGroups.CASE_QUERY`

---

## 考核统计（`/evaluation/index`）

- `POST /case/report/statistics`：按处置部门汇总
- `POST /case/report/drill`：点击数字反查案件（`metricKey` + `drillHandleDeptId`；合计行不传 `drillHandleDeptId` 表示全部处置部门）
- 筛选条件与综合查询一致：`caseOrigins`、问题状态展开、问题描述等
- 条件：上报/处置截止/结案时间、处置部门、来源、状态、地址、问题描述
- 角色：`ADMIN`、`SUPERVISOR`、`EVALUATOR`
- 指标口径见 `CaseReportMetricSql` / `CaseReportServiceImpl`

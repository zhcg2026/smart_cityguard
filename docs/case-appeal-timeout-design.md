# 处置超时申诉（双审）设计 — 已确认口径

> 范围：**仅处置部门**对**处置阶段超时**的申诉；采集员申诉、申报、扣分等本期不做。  
> 与路线图 **P2+ 申诉双审 + 超时剔除统计** 对齐。

---

## 1. 「超时」定义（已确认）

**业务含义**：处置部门完成处置并**批转派遣员**的时刻，已超过该案**处置阶段截止时间**。  
**处置时限**覆盖：

- 处置人员现场处置时间（`handling` → `handle_finish`）
- 处置部门确认、批转派遣员的操作时间（`handle_finish` → 部门 `deptConfirm` 批转）

**系统落点（与现网计时一致）**：

- 处置阶段计时在 **`deptConfirmCase`（部门确认并批转派遣员）** 时调用 `CaseTimerService.onCaseHandleFinished(caseId, now)` 结束。
- `finishTime` = 批转操作时间；与 `case_timer_record`（`timer_stage='handle'`）的 `deadline_time` 比较。
- `finishTime > deadline_time` → `case_timer_record.is_timeout = 1`（处置阶段超时）。

**展示口径**：

- 列表/详情可同时展示：截止时刻、批转完成时刻、是否超时、超时时长（`timeout_seconds`）。
- 申诉通过后仍**保留**上述原始超时事实（见第 5 节）。

---

## 2. 申诉时机（已确认）

同时满足方可申请：

| # | 条件 |
|---|------|
| 1 | 处置阶段计时 **`is_timeout = 1`**（以批转时刻判定） |
| 2 | 案件 **已结案**（`case_status IN ('closed','forced_close')` 且 `close_time IS NOT NULL`） |

不在处置中、未结案案件不可申诉。

---

## 3. 申请方与次数（已确认）

| 项 | 口径 |
|----|------|
| **申请入口** | **仅处置部门**（`DEPT` 角色；部门登录账号） |
| **次数** | **一案 1 次**；任意一级打回后**不可再次提交** |
| **并发** | 同一案件存在进行中的申诉单时不可重复申请 |

---

## 4. 审核流程（双审）

```
部门提交 (DEPT)
    → pending_dispatcher   待派遣员初审
        ├ 派遣员通过 → pending_acceptor   待受理员二审
        │       ├ 受理员通过 → approved     不计入超时（统计豁免）
        │       └ 受理员打回 → rejected     仍计超时
        └ 派遣员打回 → rejected             仍计超时（不可再诉）
```

| 节点 | 角色 | 操作 |
|------|------|------|
| 初审 | `DISPATCHER` | 通过 / 打回 + 意见 |
| 二审 | `ACCEPTOR` | 通过 / 打回 + 意见（终审） |

审核记录写入 **`appeal_review`**（每节点一条）：`review_node` = `dispatcher_review` / `acceptor_review`。

---

## 5. 「标记为未超时」与展示（已确认）

### 5.1 案件主表（建议字段）

在 **`case_info`** 增加（或补丁 SQL）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `handle_timeout_exempt` | TINYINT | 0=否，1=申诉通过，**考核/统计按未超时** |
| `handle_timeout_exempt_appeal_id` | BIGINT | 关联通过的 `appeal_apply.id` |
| `appeal_status` | VARCHAR | 案件摘要：`none` / `pending` / `approved` / `rejected` |

**申诉通过时**：

1. `handle_timeout_exempt = 1`，写入 `handle_timeout_exempt_appeal_id`
2. `appeal_status = 'approved'`
3. **不删除** `case_timer_record` 原始 `is_timeout=1`、`timeout_seconds`（保留痕迹）
4. 可选：增加 `is_timeout_overridden=1` 于计时记录，便于报表区分「事实超时」与「统计豁免」

**列表/详情展示（已确认保留）**：

- 仍显示：曾超时、批转超时时刻、超时时长
- 附加标签：**「不计超时（申诉通过）」** 或等价文案
- `handleTimeout` 类展示：统计用豁免；界面可读字段 `handleTimeoutExempt`

### 5.2 统计口径

工作台「超时」、**考核统计**中「超时处置 / 超时待处置 / 超时结案」等：

```sql
-- 计为超时
handle_timeout_exempt IS NULL OR handle_timeout_exempt = 0
AND (处置阶段 is_timeout = 1 或 业务等价条件)
```

申诉通过案件**不计入**超时类指标，但可在明细/日志中查到「曾超时已豁免」。

---

## 6. 申诉单数据（`appeal_apply`）

与 `database/init.sql` 对齐，本期类型固定：

| 字段 | 值 |
|------|-----|
| `apply_type` | `timeout_handle`（处置超时申诉，区别于其他期类型） |
| `case_id` / `case_code` | 关联案件 |
| `apply_dept_id` / `apply_user_id` | 申请部门与操作人 |
| `appeal_desc` | 申诉说明（客观因素） |
| `appeal_status` | 状态机（见第 4 节） |
| `final_result` | `approved` / `rejected`（二审或一审打回后写入） |

**申请校验**：

- 登录角色含 `DEPT`，且 `department_id` = 案件 `handle_dept_id`
- 结案 + 处置阶段 `is_timeout=1`
- 无历史申诉或仅有 rejected（且 rejected 后不可再申请）

附件：沿用 `appeal_attachment` 或 `attachments` 字段。

---

## 7. 管理端「申诉列表」

**菜单**：申诉管理 → 申诉列表（`/appeal/list`）

| 角色 | 能力 |
|------|------|
| `DEPT` | 我部门提交的申诉；从案件详情「提起申诉」入口（结案后） |
| `DISPATCHER` | 待初审 / 已审 |
| `ACCEPTOR` | 待二审 / 已审 |
| `ADMIN` / `SUPERVISOR` | 全部 |

**列表字段**：申诉编号、任务号、处置部门、申请时间、说明摘要、当前节点、初审/二审结果、案件是否曾超时、是否已豁免。

**筛选**：任务号、部门、申请时间、状态、审核结果。

**详情**：案件摘要 + 处置截止/批转/结案时间 + 计时证据 + 附件 + 审批按钮。

---

## 8. 与现网代码差异（实施时注意）

| 项 | 现状 | 目标 |
|----|------|------|
| `AppealApply` 实体 | 简化字段，与 init.sql 不一致 | 对齐库表 |
| `AppealServiceImpl` | 单级 `reviewAppeal` | 双审 + 写 `appeal_review` |
| `AppealList.vue` | 占位 | 列表 + 详情审批 |
| 计时结束点 | 已在 `deptConfirmCase` | **保持**，与业务定义一致 |
| `case_info.appeal_status` | 未使用 | 挂接状态机 |

---

## 9. MVP 接口（草案）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/appeal/timeout/submit` | 部门提交（结案+超时校验） |
| POST | `/appeal/timeout/dispatcher-review` | 派遣员初审 |
| POST | `/appeal/timeout/acceptor-review` | 受理员二审 |
| GET | `/appeal/timeout/list` | 分页列表（按角色过滤） |
| GET | `/appeal/timeout/{id}` | 详情（含案件计时、审核记录） |

改 **appeal / case / timer** 模块后须 `mvn install` 对应子模块并重启 8080。

---

## 10. 本期不做

- 采集员、受理员、派遣员代提申诉
- 非处置超时类申诉（`declare`、扣分调整等）
- 打回后补充材料再诉、领导三审
- 申诉通过后自动改历史流转记录

---

## 11. 已确认决策清单（2026-05-28）

- [x] 超时 = 部门批转派遣员时刻超出处置截止时间（含部门操作时间）
- [x] 申诉时机 = 已超时且已结案
- [x] 仅处置部门可申请
- [x] 通过后保留超时展示 + 统计豁免（`handle_timeout_exempt`）
- [x] 派遣员打回不可再诉，一案 1 次
- [x] 仅处置部门超时申诉，其他类型暂缓

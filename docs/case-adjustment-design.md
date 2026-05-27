# 案件延期 / 挂账 — 设计定稿（2026-05-25）

## 流程

- **申请**：处置部门（DEPT）在管理端案件详情提交。
- **审批**：当案 **派遣员（DISPATCHER）** 批准/驳回；不经批转/回退接口。
- **主案件路由不变**：仅改 `deadline_time`、计时状态、挂账标记。

## 规则

| 类型 | 规则 |
|------|------|
| 延期 | 在 **当前 deadline** 上 +1 个原处置时限；**批准满 2 次** 后不可再申请；**驳回不占次数** |
| 挂账 | 处置部门 **自选挂账截止日期**（自今日起最长 **1 年**）；**批准 1 次** 后不可再挂；驳回可再申请；到期 **自动恢复**；挂账期间 **不可处置操作** |
| 可申请状态 | **待指派**（`pending_handle`）、**处置中**（`handling`），且未挂账、无同类型 pending 申请 |

## 接口

| 方法 | 路径 | 角色 |
|------|------|------|
| POST | `/case/adjustment/apply` | DEPT |
| GET | `/case/adjustment/pending` | DISPATCHER / ADMIN / SUPERVISOR |
| POST | `/case/adjustment/review` | DISPATCHER |
| GET | `/case/adjustment/list/{caseId}` | 案件可读角色 |

## 表

- `case_adjustment_apply` — 申请单
- `case_info` — `dispatch_operator_id`、`is_suspended`、`suspend_until`、`extension_approved_count`

## 计时

- 延期：`CaseTimerService.extendHandleDeadline`
- 挂账：`pauseHandleTimer`；定时任务扫 `is_suspended=1 AND suspend_until<=NOW()` → `resumeHandleTimer`

## 申诉（后续）

派遣员初审 + 受理员终审；通过后不计超时。本期不实现。

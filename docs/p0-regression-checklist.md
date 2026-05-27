# P0 联调回归检查表

> API 自动化：`scripts/p0-regression.ps1`（账号见 `scripts/p0-users.json`，密码 `admin123`）  
> 管理端/采集端需在浏览器中再确认按钮与文案。

## 前置

1. MySQL `cityguard`、后端 **8080** 已启动（改 case 模块后先 `mvn clean install -pl smart-cityguard-case -am -DskipTests`）。
2. 可选：MinIO **9000**（上报附件）、管理端 **3000**、采集端 **3003**。
3. 测试账号密码已统一为 **`admin123`**（受理员01/02、派遣员01、市容环卫中心、张晓明、newuser123）。

```powershell
Set-Location d:\smart_cityguard\backend
mvn spring-boot:run -pl smart-cityguard-server -DskipTests

Set-Location d:\smart_cityguard\frontend\admin-web; npm run dev
Set-Location d:\smart_cityguard\frontend\collector-app; npm run dev

# API 回归（后端已起）
powershell -NoProfile -ExecutionPolicy Bypass -File d:\smart_cityguard\scripts\p0-regression.ps1
```

## API 自动化（2026-05-21）

| 编号 | 场景 | 结果 |
|------|------|------|
| A1–A8 | 主干：上报→立案→派遣→指派→处置→部门确认→批转受理员→结案 | PASS |
| B1–B3 | 部门回退后，派遣员「批转受理员」被拒绝 | PASS |
| C1 | 派遣员回退受理员 → `returned` | PASS |
| D1 | HANDLER 回退部门（处置前） | PASS |
| D2 | 撤销指派 → 部门回退派遣员 | PASS |
| D3 | 部门打回 HANDLER | PASS |
| D4 | 派遣员返工部门 | PASS |
| D5 | 受理员回退派遣员返工 | PASS |

## 浏览器手测（建议各走一条）

### 主干（管理端 + 采集端）

| 步骤 | 角色 | 入口 | 核对 |
|------|------|------|------|
| 1 | 采集员 `newuser123` | 采集端 上报 | 提交成功，`pending_register` |
| 2 | 受理员01 | 待立案 → 详情 | 立案并批转派遣员01，选立结案标准 |
| 3 | 派遣员01 | 待派遣 | 派至「市容环卫中心」 |
| 4 | 市容环卫中心 | 部门待指派 | 指派「张晓明」 |
| 5 | 张晓明 | 采集端 待处置 | 提交处置 → `handle_finish` |
| 6 | 市容环卫中心 | 处置人员已处置 | 「批转派遣员」 |
| 7 | 派遣员01 | 待办审核 | 「批转受理员」选受理员02 |
| 8 | 受理员02 | 待我结案 | 仅「结案」 |

### 回退（管理端 / 采集端）

| 场景 | 操作人 | 预期 |
|------|--------|------|
| 派遣员回退受理员 | 派遣员01，待派遣 | `returned`，受理员可再立案 |
| 撤销指派 + 部门回退 | 市容环卫中心 | 先撤销指派，再回退；`returned` |
| 部门打回 HANDLER | 市容环卫中心，`handle_finish` | 回到 `handling` |
| HANDLER 回退部门 | 张晓明，采集端处置详情 | `pending_handle`，理由必填 |
| 派遣员返工部门 | 派遣员01，`pending_check` | `pending_handle`，原部门 |
| 受理员回退派遣员 | 受理员02，待结案 | 原部门再处置 |
| **守卫** | 部门刚回退后，派遣员 | **无**「批转受理员」，或接口报错 |

## 站内消息（第一期，2026-05-21）

- [ ] 采集员上报后，**所有受理员**账号（至少 2 个）在约 30s 内出现顶栏角标 + 通知弹窗
- [ ] 点击通知可进入案件详情；案件列表/待办列表无需 F5 自动出现新案
- [ ] 立案→派遣员、派遣→部门、指派 HANDLER、下发核查/核实→对应人收到消息
- [ ] 采集员完成核查/核实→下发受理员收到「任务已完成」类消息
- [ ] 若无任何消息：检查 `user_message` 表结构，必要时执行 `database/patch_user_message_align.sql`

## P1 核查/核实（可选分支，2026-05-21）

- 立案前 **发送核查** → `checking` + `check_task` → 采集端「我的任务-核查」→ 完成后回 **待立案**
- 结案前 **发送核实** → `pending_check` 不变 + `verify_task` → 采集端「核实」→ 完成后受理员 **结案/返工**
- 主干流程不强制核查/核实；进行中会拦截重复下发与结案（有进行中核实任务时）

## 未覆盖（后续）

- 六步 `stage` 展示
- 任务附件表 `check_attachment` / `verify_attachment` 落库

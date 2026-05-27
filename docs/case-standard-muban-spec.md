# 立结案标准（muban.xlsx）— 模板规范与实施说明

> **状态**：已按本文实现 **后端导入接口** 与 **管理端上传页**（`业务配置 → 立结案标准`）。模板文件请使用与规范一致的 **`muban.xlsx`**（根目录或本地上传）。

**接口**：`POST /config/standard/import-muban`，`multipart/form-data` 字段名 `file`；**仅 `ROLE_ADMIN`** 可调用。导入前会按 `category_type` **物理删除** `responsibility_config`、`category_extend_field`、`case_standard`、`category_small`、`category_big` 中对应数据再重建（详见下文「导入策略」）。

---

## 1. 目标与范围

| 项目 | 说明 |
|------|------|
| **维护方** | 仅管理员（接口 `@PreAuthorize(hasRole('ADMIN'))`；管理端上传区仅 ADMIN 可见） |
| **输入** | 根目录 **`muban.xlsx`**：两个 Sheet（部件、事件），结构见下文 |
| **输出** | 在系统内形成 **大类 → 小类 → 多条立结案标准（条件 + 时限）**，供采集端、立案端级联选择 |
| **本期不做** | 部署、多环境发布、历史版本回滚（可二期） |

---

## 2. Excel 结构（约定版）

### 2.1 工作表

| Sheet 名 | 对应系统 `category_type` | 说明 |
|----------|--------------------------|------|
| **部件** | `component` | 与现有接口 `GET /config/category/big/list?type=1` 一致 |
| **事件** | `event` | 与 `type=2` 一致 |

### 2.2 列定义（两表相同，共 9 列）

| 列序 | 表头 | 必填 | 说明 |
|------|------|------|------|
| A | 大类代码 | 新大类首行必填 | 与库表 `category_big.big_code` 对应 |
| B | 大类名称 | 新大类首行必填 | `category_big.big_name` |
| C | 小类代码 | 新小类首行必填 | `category_small.small_code` |
| D | 小类名称 | 新小类首行必填 | `category_small.small_name` |
| E | 处置单位 | 小类首行必填 | 文本；建议映射至 `category_small.responsibility_subject`（责任主体） |
| F | 主管部门 | 小类首行必填 | 文本；建议映射至 `category_small.supervise_subject`（监管主体） |
| G | 立案条件 | 每数据行必填 | 一条标准一条立案描述 |
| H | 结案条件 | 每数据行必填 | 对应结案描述 |
| I | 处置时限 | 每数据行必填 | 文本，如「4小时」「15天」「2紧急工作时」；实施时需解析入库 |

### 2.3 行规则（解析算法）

1. **第 1 行**：标题/说明，**整行跳过**。
2. **第 2 行**：表头，**列名校验**（与上表一致则通过）。
3. **第 3 行起**：数据。

**纵向合并语义**（与当前模板一致）：

- **大类**：若 A、B 非空 → 开启新的大类上下文；若为空 → 沿用当前大类。
- **小类**：若 C、D 非空 → 开启新的小类上下文；若为空 → 沿用当前小类（续行）。
- **处置单位 / 主管部门**：续行通常为空，表示与**该小类首行**的 E、F 相同。
- **立案 / 结案 / 时限**：每一行（含续行）生成 **一条**立结案标准记录（同一 `small_id` 下多行 = 多条可选标准）。

### 2.4 编码与唯一性

- 库表约束（见 `database/init.sql`）：**同一 `category_type` 下** `(big_code, small_code)` 小类唯一；大类 `(big_code, category_type)` 唯一。
- 模板中事件类大类代码可为 `01` 等多位，**实施前**需确认：若与库 `VARCHAR(2)` 不一致，应 **放宽字段长度** 或 **模板改为两位**，避免导入失败（**实施时必选一项**）。

---

## 3. 与数据库字段映射（建议）

### 3.1 `category_big`

| 模板 | 字段 |
|------|------|
| 大类代码 | `big_code` |
| 大类名称 | `big_name` |
| Sheet | `category_type` = `component` / `event` |
| — | `sort_order`：可按大类出现顺序递增 |
| — | `status` = 1 |

### 3.2 `category_small`

| 模板 | 字段 |
|------|------|
| 小类代码 | `small_code` |
| 小类名称 | `small_name` |
| 当前大类 id | `big_id` |
| 当前大类代码 | `big_code`（冗余，与库一致） |
| Sheet | `category_type` |
| 处置单位 | `responsibility_subject`（库注释为责任主体，语义与「处置单位」对齐） |
| 主管部门 | `supervise_subject`（监管主体） |
| — | `full_code`：建议 `big_code + small_code` 拼接规则与现网一致（实施时与现有数据对齐） |
| — | `sort_order`：可按小类在表中出现顺序 |

### 3.3 `case_standard`（每条数据行一条）

`init.sql` 中表结构含：`standard_code`、`small_id`、`big_code`、`small_code`、`category_type`、`condition_desc`、`handle_time_limit`、`handle_time_value`、`handle_time_type`、`close_condition`、`sort_order`、`status`、`is_deleted` 等。

**建议映射**：

| 模板 | 字段 |
|------|------|
| 立案条件 | `condition_desc` |
| 结案条件 | `close_condition` |
| 处置时限 | `handle_time_limit`（原文）+ 解析出的 `handle_time_value`、`handle_time_type`（见 §4） |
| — | `small_id`、`big_code`、`small_code`、`category_type` 由上下文带出 |
| — | `standard_code`：已实现为 `S{smallId}_{序号}` |

**注意**：`CaseStandard` 实体已与 `init.sql` 中 `case_standard` 表对齐；`condition_desc` 在 JSON 中输出为 `conditionContent`，与采集端字段一致。

---

## 4. 处置时限文本解析（已实现）

模板中为自然语言，当前支持：

| 示例 | `handle_time_type` | `handle_time_value` |
|------|---------------------|---------------------|
| `2紧急工作时` | `urgent_hour` | 2 |
| `1紧急工作日` | `natural_day` | 1 |
| `4小时` | `work_hour` | 4 |
| `2天` / `2工作日` | `work_day` | 2 |
| 无法解析 | `natural_day` | 1（并保留原文于 `handle_time_limit`） |

---

## 5. 导入策略（已实现：全量物理替换）

按 Sheet 解析出 `category_type`（`component` / `event`）后，对该类型执行：

1. `DELETE FROM responsibility_config WHERE small_id IN (SELECT id FROM category_small WHERE category_type = ?)`
2. `DELETE FROM category_extend_field WHERE small_id IN (…)`
3. `DELETE FROM case_standard WHERE category_type = ?`
4. `DELETE FROM category_small WHERE category_type = ?`
5. `DELETE FROM category_big WHERE category_type = ?`

再插入 Excel 中的大类、小类、标准行。**若生产库已有案件引用旧小类 ID，请勿在未评估的情况下执行导入。**

| 模式 | 说明 |
|------|------|
| ~~按 Sheet 全量替换~~ | **当前实现**：与上表一致，避免与唯一索引 + 逻辑删冲突 |
| **增量合并** | 未实现；若二期需要可改为 upsert |

---

## 6. 功能清单（状态）

1. **后端**：`POST /config/standard/import-muban` + `MubanStandardImportService`（POI）+ JWT 注入角色以支持 `@PreAuthorize` — **已完成**。  
2. **管理端**：`业务配置 → 立结案标准` 上传、结果表 — **已完成**。  
3. **采集 / 立案**：仍使用既有 `/config/**` 接口；导入后刷新页面即可 — **需联调验证**。

---

## 7. 与现有接口的关系

- `GET /config/category/big/list?type=1|2` — 大类列表  
- `GET /config/category/small/list/{categoryBigId}` — 小类列表  
- `GET /config/standard/conditions/{categorySmallId}` — 立案条件（标准）列表  

导入完成后应 **无需改路径** 即可被采集、立案使用；若标准列表接口字段与页面展示不一致，再单开任务改前端展示。

---

## 8. 文档维护

- **模板变更**：以仓库根目录 **`muban.xlsx`** 为准；变更列或行规则时请同步修改 **本文 §2～§4**。  
- **实施完成后**：可在本文顶部增加「已上线版本 / 日期」一行，或改指向带版本号的模板文件名。

---

*本文档随 `muban.xlsx` 与库表评审结果更新；实施以代码与迁移脚本为准。*

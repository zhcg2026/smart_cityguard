/**
 * 与 database/init.sql 中 role.role_code 一致（登录接口返回的 roles 列表）
 */
export const RoleCode = {
  ADMIN: 'ADMIN',
  ACCEPTOR: 'ACCEPTOR',
  DISPATCHER: 'DISPATCHER',
  HANDLER: 'HANDLER',
  DEPT: 'DEPT',
  SUPERVISOR: 'SUPERVISOR',
  EVALUATOR: 'EVALUATOR',
  COLLECTOR: 'COLLECTOR',
  LEADER: 'LEADER'
}

/** 各业务岗位常用角色合集（不含 ADMIN，ADMIN 在 canAccessMeta 中单开） */
export const RoleGroups = {
  /** 工作台：所有管理端业务账号 */
  DASHBOARD: [
    RoleCode.ACCEPTOR,
    RoleCode.DISPATCHER,
    RoleCode.HANDLER,
    RoleCode.DEPT,
    RoleCode.SUPERVISOR,
    RoleCode.EVALUATOR,
    RoleCode.COLLECTOR,
    RoleCode.LEADER,
    RoleCode.ADMIN
  ],
  /** 案件相关 */
  CASE: [
    RoleCode.ACCEPTOR,
    RoleCode.DISPATCHER,
    RoleCode.HANDLER,
    RoleCode.DEPT,
    RoleCode.SUPERVISOR,
    RoleCode.ADMIN
  ],
  /** 综合查询 */
  CASE_QUERY: [
    RoleCode.ADMIN,
    RoleCode.SUPERVISOR,
    RoleCode.LEADER,
    RoleCode.ACCEPTOR,
    RoleCode.DISPATCHER
  ],
  /** 受理员案件管理子菜单 */
  ACCEPTOR_CASE: [RoleCode.ACCEPTOR, RoleCode.ADMIN, RoleCode.SUPERVISOR],
  /** 派遣员案件管理子菜单 */
  DISPATCHER_CASE: [RoleCode.DISPATCHER, RoleCode.ADMIN, RoleCode.SUPERVISOR],
  /** 部门/处置人员待办队列 */
  CASE_PENDING_OPS: [RoleCode.HANDLER, RoleCode.DEPT, RoleCode.SUPERVISOR, RoleCode.ADMIN],
  /** 任务台账（核查/核实全量列表，仅督办） */
  TASK_LEDGER: [RoleCode.ADMIN, RoleCode.SUPERVISOR],
  /** @deprecated 处置人员不再使用独立任务菜单，见 TASK_LEDGER */
  TASK: [RoleCode.ADMIN, RoleCode.SUPERVISOR],
  APPEAL: [
    RoleCode.DEPT,
    RoleCode.ACCEPTOR,
    RoleCode.DISPATCHER,
    RoleCode.SUPERVISOR,
    RoleCode.ADMIN
  ],
  EVALUATION: [RoleCode.EVALUATOR, RoleCode.SUPERVISOR, RoleCode.ADMIN],
  /** 考核评价父菜单（含考核统计 + 综合查询各子项按自身 roles 再过滤） */
  EVALUATION_SECTION: [
    RoleCode.EVALUATOR,
    RoleCode.SUPERVISOR,
    RoleCode.ADMIN,
    RoleCode.LEADER,
    RoleCode.ACCEPTOR,
    RoleCode.DISPATCHER
  ],
  GEO: [RoleCode.ADMIN, RoleCode.SUPERVISOR],
  SYSTEM: [RoleCode.ADMIN],
  CONFIG: [RoleCode.ADMIN, RoleCode.SUPERVISOR],
  MESSAGE: [
    RoleCode.ACCEPTOR,
    RoleCode.DISPATCHER,
    RoleCode.HANDLER,
    RoleCode.SUPERVISOR,
    RoleCode.EVALUATOR,
    RoleCode.ADMIN
  ]
}

/**
 * 合并父/子路由 meta（子路由未单独配置 roles 时继承父级）
 */
export function mergeRouteMeta(parentMeta, childMeta) {
  const parent = parentMeta || {}
  const child = childMeta || {}
  return {
    ...parent,
    ...child,
    roles: child.roles?.length ? child.roles : parent.roles
  }
}

/**
 * @param {import('vue-router').RouteMeta} meta
 * @param {string[]} userRoles 当前用户 role_code 列表
 */
export function canAccessMeta(meta, userRoles) {
  if (!userRoles?.length) return false
  if (userRoles.includes(RoleCode.ADMIN)) return true
  const req = meta?.roles
  if (!req?.length) return true
  return req.some((code) => userRoles.includes(code))
}

/**
 * 待办列表默认 tab：按角色聚焦自己的队列
 * @param {string[]} userRoles
 * @returns {string} CasePending 使用的 status / tab label
 */
/** 顶部展示用 */
export const roleNameMap = {
  ADMIN: '管理员',
  ACCEPTOR: '受理员',
  DISPATCHER: '派遣员',
  HANDLER: '处置人员',
  DEPT: '部门账号',
  SUPERVISOR: '值班长',
  EVALUATOR: '考核员',
  COLLECTOR: '采集员',
  LEADER: '领导'
}

export function formatRoleLabels(codes) {
  if (!codes?.length) return ''
  return codes.map((c) => roleNameMap[c] || c).join('、')
}

export function defaultPendingTabForRoles(userRoles) {
  if (!userRoles?.length) return 'acceptor_todo'
  if (userRoles.includes(RoleCode.ADMIN) || userRoles.includes(RoleCode.SUPERVISOR)) {
    return 'acceptor_todo'
  }
  if (userRoles.includes(RoleCode.ACCEPTOR)) return 'acceptor_pending_register'
  if (userRoles.includes(RoleCode.DISPATCHER)) return 'dispatcher_pending_dispatch'
  if (userRoles.includes(RoleCode.DEPT)) return 'dept_confirm_todo'
  if (userRoles.includes(RoleCode.HANDLER)) return 'handling'
  return 'acceptor_todo'
}

/** 与后端 sys_role.role_code 一致 */
export const RoleCode = {
  COLLECTOR: 'COLLECTOR',
  HANDLER: 'HANDLER',
  DEPT: 'DEPT',
  ADMIN: 'ADMIN'
}

export function hasRole(roles, code) {
  return Array.isArray(roles) && roles.includes(code)
}

/** 以处置人员身份使用移动端（指派给我的处置中案件） */
export function isHandlerMobileUser(roles) {
  return hasRole(roles, RoleCode.HANDLER)
}

export function isCollectorMobileUser(roles) {
  return hasRole(roles, RoleCode.COLLECTOR)
}

export function isDeptMobileUser(roles) {
  return hasRole(roles, RoleCode.DEPT)
}

/** 登录后默认首页 */
export function defaultHomePath(roles) {
  if (isHandlerMobileUser(roles) && !isCollectorMobileUser(roles)) {
    return '/handle'
  }
  if (isDeptMobileUser(roles)) {
    return '/handle'
  }
  return '/home'
}

export const roleNameMap = {
  COLLECTOR: '采集员',
  HANDLER: '处置人员',
  DEPT: '部门账号'
}

export function primaryRoleLabel(roles) {
  if (isHandlerMobileUser(roles) && !isCollectorMobileUser(roles)) {
    return roleNameMap.HANDLER
  }
  if (isCollectorMobileUser(roles)) {
    return roleNameMap.COLLECTOR
  }
  if (hasRole(roles, RoleCode.DEPT)) {
    return roleNameMap.DEPT
  }
  return '用户'
}

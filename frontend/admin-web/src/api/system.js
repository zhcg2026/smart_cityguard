import request from '@/utils/request'

// ========== 用户管理 ==========

export function getUserList(params) {
  return request({
    url: '/system/user/list',
    method: 'get',
    params
  })
}

export function getUserDetail(id) {
  return request({
    url: `/system/user/${id}`,
    method: 'get'
  })
}

export function getUserRoles(id) {
  return request({
    url: `/system/user/${id}/roles`,
    method: 'get'
  })
}

export function createUser(data) {
  return request({
    url: '/system/user',
    method: 'post',
    data
  })
}

export function updateUser(data) {
  return request({
    url: '/system/user',
    method: 'put',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: `/system/user/${id}`,
    method: 'delete'
  })
}

export function resetPassword(id, password) {
  return request({
    url: `/system/user/${id}/reset-password`,
    method: 'post',
    params: { password }
  })
}

// ========== 角色管理 ==========

export function getRoleList() {
  return request({
    url: '/system/role/list',
    method: 'get'
  })
}

export function getRoleDetail(id) {
  return request({
    url: `/system/role/${id}`,
    method: 'get'
  })
}

export function createRole(data) {
  return request({
    url: '/system/role',
    method: 'post',
    data
  })
}

export function updateRole(data) {
  return request({
    url: '/system/role',
    method: 'put',
    data
  })
}

export function deleteRole(id) {
  return request({
    url: `/system/role/${id}`,
    method: 'delete'
  })
}

// ========== 部门管理 ==========

export function getDeptTree() {
  return request({
    url: '/system/dept/tree',
    method: 'get'
  })
}

export function getDeptDetail(id) {
  return request({
    url: `/system/dept/${id}`,
    method: 'get'
  })
}

export function createDept(data) {
  return request({
    url: '/system/dept',
    method: 'post',
    data
  })
}

export function updateDept(data) {
  return request({
    url: '/system/dept',
    method: 'put',
    data
  })
}

export function deleteDept(id) {
  return request({
    url: `/system/dept/${id}`,
    method: 'delete'
  })
}

export function ensureDeptLogin(id) {
  return request({
    url: `/system/dept/${id}/ensure-login`,
    method: 'post'
  })
}

export function resetDeptLoginPassword(id, password) {
  return request({
    url: `/system/dept/${id}/reset-login-password`,
    method: 'post',
    params: { password }
  })
}
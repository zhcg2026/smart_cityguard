import request from '@/utils/request'

// 登录
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

// 登出
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

// 获取用户信息
export function getUserInfo() {
  return request({
    url: '/auth/info',
    method: 'get'
  })
}

// 刷新Token
export function refreshToken() {
  return request({
    url: '/auth/refresh',
    method: 'post'
  })
}

// 获取用户列表
export function getUserList(params) {
  return request({
    url: '/system/user/list',
    method: 'get',
    params
  })
}

// 获取用户详情
export function getUserDetail(id) {
  return request({
    url: `/system/user/${id}`,
    method: 'get'
  })
}

// 创建用户
export function createUser(data) {
  return request({
    url: '/system/user',
    method: 'post',
    data
  })
}

// 更新用户
export function updateUser(data) {
  return request({
    url: '/system/user',
    method: 'put',
    data
  })
}

// 删除用户
export function deleteUser(id) {
  return request({
    url: `/system/user/${id}`,
    method: 'delete'
  })
}
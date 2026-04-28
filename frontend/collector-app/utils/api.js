import { get, post, upload } from './request'

// 登录
export function login(data) {
  return post('/auth/login', data)
}

// 获取用户信息
export function getUserInfo() {
  return get('/auth/info')
}

// 获取每日提示
export function getDailyTips() {
  return get('/collector/daily-tip')
}

// 获取公文通告
export function getAnnouncements(params) {
  return get('/collector/announcement', params)
}

// 获取大类列表
export function getCategoryBigList() {
  return get('/config/category/big/list')
}

// 获取小类列表
export function getCategorySmallList(categoryBigId) {
  return get(`/config/category/small/list/${categoryBigId}`)
}

// 获取立案条件
export function getConditions(categorySmallId) {
  return get(`/config/standard/conditions/${categorySmallId}`)
}

// 问题上报
export function reportCase(data) {
  return post('/case/report', data)
}

// 上传附件
export function uploadFile(filePath) {
  return upload(filePath)
}

// 获取核查任务列表
export function getVerifyTaskList(params) {
  return get('/task/verify/list', params)
}

// 获取核实任务列表
export function getCheckTaskList(params) {
  return get('/task/check/list', params)
}

// 执行核查任务
export function executeVerifyTask(data) {
  return post('/task/verify/execute', data)
}

// 执行核实任务
export function executeCheckTask(data) {
  return post('/task/check/execute', data)
}

// 获取任务详情
export function getTaskDetail(id, type) {
  return get(`/task/${type}/${id}`)
}

// 获取案件详情
export function getCaseDetail(id) {
  return get(`/case/${id}`)
}

// 获取申诉列表
export function getAppealList(params) {
  return get('/appeal/list', params)
}

// 提交申诉
export function submitAppeal(data) {
  return post('/appeal/submit', data)
}

// 获取消息列表
export function getMessageList(params) {
  return get('/message/list', params)
}

// 获取网格信息
export function getGridInfo(lng, lat) {
  return get('/geo/grid/info', { lng, lat })
}

export default {
  login,
  getUserInfo,
  getDailyTips,
  getAnnouncements,
  getCategoryBigList,
  getCategorySmallList,
  getConditions,
  reportCase,
  uploadFile,
  getVerifyTaskList,
  getCheckTaskList,
  executeVerifyTask,
  executeCheckTask,
  getTaskDetail,
  getCaseDetail,
  getAppealList,
  submitAppeal,
  getMessageList,
  getGridInfo
}
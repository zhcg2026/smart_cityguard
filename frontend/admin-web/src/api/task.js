import request from '@/utils/request'

// 获取核查任务列表
export function getVerifyTaskList(params) {
  return request({
    url: '/task/verify/list',
    method: 'get',
    params
  })
}

// 执行核查任务
export function executeVerifyTask(data) {
  return request({
    url: '/task/verify/execute',
    method: 'post',
    data
  })
}

// 获取核实任务列表
export function getCheckTaskList(params) {
  return request({
    url: '/task/check/list',
    method: 'get',
    params
  })
}

// 执行核实任务
export function executeCheckTask(data) {
  return request({
    url: '/task/check/execute',
    method: 'post',
    data
  })
}

// 获取任务详情
export function getTaskDetail(id, type) {
  return request({
    url: `/task/${type}/${id}`,
    method: 'get'
  })
}
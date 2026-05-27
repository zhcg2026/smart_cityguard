import request from '@/utils/request'

// 核查任务列表（check_task，立案前）
export function getCheckTaskList(params) {
  return request({
    url: '/task/check/list',
    method: 'get',
    params
  })
}

// 核实任务列表（verify_task，结案前）
export function getVerifyTaskList(params) {
  return request({
    url: '/task/verify/list',
    method: 'get',
    params
  })
}

// 执行核查任务（采集端使用；管理端台账不调用）
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
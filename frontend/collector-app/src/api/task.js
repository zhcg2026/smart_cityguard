import request from '@/utils/request'

/** 核查任务列表（立案前可选） */
export function getCheckTaskList(params) {
  return request({
    url: '/task/check/list',
    method: 'get',
    params
  })
}

/** 核实任务列表（结案前可选） */
export function getVerifyTaskList(params) {
  return request({
    url: '/task/verify/list',
    method: 'get',
    params
  })
}

export function getCheckTaskDetail(id) {
  return request({
    url: `/task/check/${id}`,
    method: 'get'
  })
}

export function getVerifyTaskDetail(id) {
  return request({
    url: `/task/verify/${id}`,
    method: 'get'
  })
}

export function executeCheckTask(data) {
  return request({
    url: '/task/check/execute',
    method: 'post',
    data
  })
}

export function executeVerifyTask(data) {
  return request({
    url: '/task/verify/execute',
    method: 'post',
    data
  })
}

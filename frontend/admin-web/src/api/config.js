import request from '@/utils/request'

// 获取大类列表
export function getCategoryBigList(params) {
  return request({
    url: '/config/category/big/list',
    method: 'get',
    params
  })
}

// 获取小类列表
export function getCategorySmallList(params) {
  return request({
    url: '/config/category/small/list',
    method: 'get',
    params
  })
}

// 获取立结案标准
export function getStandardList(params) {
  return request({
    url: '/config/standard/list',
    method: 'get',
    params
  })
}

// 获取立案条件
export function getConditions(categorySmallId) {
  return request({
    url: `/config/standard/conditions/${categorySmallId}`,
    method: 'get'
  })
}

// 获取时限规则
export function getTimeLimitRules(params) {
  return request({
    url: '/config/timelimit/list',
    method: 'get',
    params
  })
}

// 获取工作时段配置
export function getWorkTimeConfig(params) {
  return request({
    url: '/config/worktime/list',
    method: 'get',
    params
  })
}

// 获取节假日配置
export function getHolidayConfig(params) {
  return request({
    url: '/config/holiday/list',
    method: 'get',
    params
  })
}

// 获取通告列表
export function getAnnouncementList(params) {
  return request({
    url: '/config/announcement/list',
    method: 'get',
    params
  })
}

// 发布通告
export function createAnnouncement(data) {
  return request({
    url: '/config/announcement',
    method: 'post',
    data
  })
}

// 获取每日提示
export function getDailyTip(params) {
  return request({
    url: '/config/dailytip/list',
    method: 'get',
    params
  })
}
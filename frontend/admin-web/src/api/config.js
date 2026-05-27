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
export function getCategorySmallList(categoryBigId) {
  return request({
    url: `/config/category/small/list/${categoryBigId}`,
    method: 'get'
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

/** 案件分类管理（含停用项） */
export function getCategoryBigManageList(params) {
  return request({
    url: '/config/category/big/manage',
    method: 'get',
    params
  })
}

export function saveCategoryBig(data) {
  return request({
    url: '/config/category/big',
    method: 'post',
    data
  })
}

export function deleteCategoryBig(id) {
  return request({
    url: `/config/category/big/${id}`,
    method: 'delete'
  })
}

export function getCategorySmallManageList(bigId) {
  return request({
    url: `/config/category/small/manage/${bigId}`,
    method: 'get'
  })
}

export function saveCategorySmall(data) {
  return request({
    url: '/config/category/small',
    method: 'post',
    data
  })
}

export function deleteCategorySmall(id) {
  return request({
    url: `/config/category/small/${id}`,
    method: 'delete'
  })
}

export function getStandardManageList(smallId) {
  return request({
    url: `/config/standard/manage/${smallId}`,
    method: 'get'
  })
}

export function saveCaseStandard(data) {
  return request({
    url: '/config/standard',
    method: 'post',
    data
  })
}

export function deleteCaseStandard(id) {
  return request({
    url: `/config/standard/${id}`,
    method: 'delete'
  })
}

/** 导入 muban.xlsx（仅管理员） */
export function importMubanStandard(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/config/standard/import-muban',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

/** 全局时限计算规则 */
export function getTimeLimitRules() {
  return request({
    url: '/config/timelimit/list',
    method: 'get'
  })
}

/** 大类下小类时限一览 */
export function getSmallTimeLimits(bigId) {
  return request({
    url: '/config/timelimit/small/list',
    method: 'get',
    params: { bigId }
  })
}

/** 保存小类时限覆盖 */
export function saveTimeLimitOverride(data) {
  return request({
    url: '/config/timelimit/override',
    method: 'post',
    data
  })
}

/** 删除小类时限覆盖 */
export function deleteTimeLimitOverride(id) {
  return request({
    url: `/config/timelimit/override/${id}`,
    method: 'delete'
  })
}

/** 工作时段配置 */
export function getWorkTimeConfig() {
  return request({
    url: '/config/worktime/list',
    method: 'get'
  })
}

export function updateWorkTimeConfig(id, data) {
  return request({
    url: `/config/worktime/${id}`,
    method: 'put',
    data
  })
}

/** 节假日配置 */
export function getHolidayConfig(params) {
  return request({
    url: '/config/holiday/list',
    method: 'get',
    params
  })
}

export function saveHoliday(data) {
  return request({
    url: '/config/holiday',
    method: 'post',
    data
  })
}

export function deleteHoliday(id) {
  return request({
    url: `/config/holiday/${id}`,
    method: 'delete'
  })
}

// 获取通告列表（当前用户可见）
export function getAnnouncementList(params) {
  return request({
    url: '/message/announcement/list',
    method: 'get',
    params
  })
}

export function getAnnouncementAdminList() {
  return request({
    url: '/message/announcement/admin/list',
    method: 'get'
  })
}

export function getAnnouncementDetail(id) {
  return request({
    url: `/message/announcement/${id}`,
    method: 'get'
  })
}

// 发布通告
export function createAnnouncement(data) {
  return request({
    url: '/message/announcement',
    method: 'post',
    data
  })
}

export function updateAnnouncement(id, data) {
  return request({
    url: `/message/announcement/${id}`,
    method: 'put',
    data
  })
}

export function deleteAnnouncement(id) {
  return request({
    url: `/message/announcement/${id}`,
    method: 'delete'
  })
}

// 获取每日提示（当前用户可见）
export function getDailyTip(params) {
  return request({
    url: '/message/dailytip/latest',
    method: 'get',
    params
  })
}

export function getDailyTipAdminList() {
  return request({
    url: '/message/dailytip/admin/list',
    method: 'get'
  })
}

export function getDailyTipList(params) {
  return request({
    url: '/message/dailytip/list',
    method: 'get',
    params
  })
}

export function getDailyTipDetail(id) {
  return request({
    url: `/message/dailytip/${id}`,
    method: 'get'
  })
}

export function createDailyTip(data) {
  return request({
    url: '/message/dailytip',
    method: 'post',
    data
  })
}

export function updateDailyTip(id, data) {
  return request({
    url: `/message/dailytip/${id}`,
    method: 'put',
    data
  })
}

export function deleteDailyTip(id) {
  return request({
    url: `/message/dailytip/${id}`,
    method: 'delete'
  })
}

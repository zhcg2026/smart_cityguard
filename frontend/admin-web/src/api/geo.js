import request from '@/utils/request'

// ========== 片区管理 ==========

/**
 * 查询所有片区列表
 */
export function getRespGridList() {
  return request({
    url: '/geo/resp-grid/list',
    method: 'get'
  })
}

/**
 * 查询片区详情
 */
export function getRespGridDetail(id) {
  return request({
    url: `/geo/resp-grid/${id}`,
    method: 'get'
  })
}

/** 校验坐标是否在指定责任片区内 */
export function checkRespGridLocation(respGridId, lng, lat) {
  return request({
    url: '/geo/resp-grid/check-location',
    method: 'post',
    params: { respGridId, lng, lat }
  })
}

/**
 * 新建片区
 */
export function createRespGrid(data) {
  return request({
    url: '/geo/resp-grid',
    method: 'post',
    data
  })
}

/**
 * 更新片区
 */
export function updateRespGrid(id, data) {
  return request({
    url: `/geo/resp-grid/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除片区
 */
export function deleteRespGrid(id) {
  return request({
    url: `/geo/resp-grid/${id}`,
    method: 'delete'
  })
}

/**
 * 导入 GeoJSON 文件创建片区
 */
export function importRespGridGeoJson(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/geo/resp-grid/import',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 设置片区绑定的采集员（全量替换，userIds 可空数组表示清空）
 */
export function setRespGridCollectors(respGridId, userIds) {
  return request({
    url: `/geo/resp-grid/${respGridId}/collectors`,
    method: 'put',
    data: userIds || []
  })
}

/**
 * 查询采集员所属片区列表
 */
export function getRespGridByCollector(userId) {
  return request({
    url: `/geo/resp-grid/collector/${userId}`,
    method: 'get'
  })
}

// ========== 原有街道/社区/网格接口 ==========

export function getStreetList() {
  return request({
    url: '/geo/street/list',
    method: 'get'
  })
}

export function getCommunityList(streetId) {
  return request({
    url: `/geo/community/list/${streetId}`,
    method: 'get'
  })
}

export function getGridList(communityId) {
  return request({
    url: `/geo/grid/list/${communityId}`,
    method: 'get'
  })
}
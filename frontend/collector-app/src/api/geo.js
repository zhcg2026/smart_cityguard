import request from '@/utils/request'

/** 查询采集员绑定的责任片区 */
export function getCollectorRespGrids(userId) {
  return request({
    url: `/geo/resp-grid/collector/${userId}`,
    method: 'get'
  })
}

/** 校验坐标是否在指定片区内 */
export function checkPointInArea(respGridId, lng, lat) {
  return request({
    url: '/geo/resp-grid/check-location',
    method: 'post',
    params: { respGridId, lng, lat },
    skipErrorToast: true
  })
}

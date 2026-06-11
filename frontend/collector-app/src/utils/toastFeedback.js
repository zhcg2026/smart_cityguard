import { showFailToast, showSuccessToast, showToast } from 'vant'

/** 与 App.vue 中样式类一致，避免默认 Toast 白底看不清 */
const FAIL_CLASS = 'upload-fail-toast'
const SUCCESS_CLASS = 'upload-success-toast'
const INFO_CLASS = 'collector-app-toast'

export function showAppFailToast(message, duration = 4000) {
  if (!message) return
  showFailToast({
    message,
    position: 'top',
    duration,
    forbidClick: true,
    className: FAIL_CLASS
  })
}

export function showAppSuccessToast(message, duration = 2500) {
  if (!message) return
  showSuccessToast({
    message,
    position: 'top',
    duration,
    className: SUCCESS_CLASS
  })
}

export function showAppToast(message, duration = 2500) {
  if (!message) return
  showToast({
    message,
    position: 'top',
    duration,
    className: INFO_CLASS
  })
}

/** 从 axios / 业务错误中提取可读文案 */
export function extractRequestErrorMessage(error, fallback = '操作失败') {
  const body = error?.response?.data
  if (typeof body === 'object' && body?.message) {
    return String(body.message)
  }
  if (error?.message && error.message !== 'Error') {
    return String(error.message)
  }
  return fallback
}

import { closeToast, showDialog, showFailToast, showToast } from 'vant'

/** 从接口/网络错误中提取可读文案 */
export function extractUploadErrorMessage(error) {
  const body = error?.response?.data
  if (typeof body === 'object' && body?.message) {
    return String(body.message)
  }
  if (typeof body === 'string' && body.trim()) {
    return body.trim()
  }
  const status = error?.response?.status
  if (status === 502 || status === 503 || status === 504) {
    return '后端或文件服务不可用'
  }
  if (!error?.response && (error?.code === 'ERR_NETWORK' || error?.message === 'Network Error')) {
    return '网络异常，请确认后端(8080)与 MinIO(9000) 已启动'
  }
  return error?.message ? String(error.message) : ''
}

/** 是否为 MinIO / 文件存储类错误 */
export function isStorageServiceError(error) {
  const msg = extractUploadErrorMessage(error).toLowerCase()
  return (
    /minio|文件上传失败|文件服务|connection refused|econnrefused|connect|9000|upload failed/i.test(
      msg
    ) || error?.response?.status === 502
  )
}

const MINIO_HINT =
  '照片上传失败：文件服务(MinIO)未启动。\n\n请在项目根目录运行 start-minio.ps1，或确保本机 9000 端口可用后重新选图上传。'

/**
 * 关闭 loading 后展示上传失败（存储类错误用对话框，避免白框一闪而过）
 * @returns {{ storage: boolean, text: string }}
 */
export async function showUploadFailure(error, options = {}) {
  closeToast()

  const storage = isStorageServiceError(error) || options.forceStorageHint
  const detail = extractUploadErrorMessage(error)
  const text = storage
    ? MINIO_HINT
    : detail
      ? `照片上传失败：${detail}`
      : '照片上传失败，请重试'

  if (storage || options.useDialog) {
    await showDialog({
      title: '照片无法上传',
      message: text,
      confirmButtonText: '知道了',
      messageAlign: 'left',
      className: 'upload-error-dialog'
    })
  } else {
    showFailToast({
      message: text,
      position: 'top',
      duration: 5000,
      forbidClick: true,
      className: 'upload-fail-toast'
    })
  }

  return { storage, text }
}

export function showUploadSuccess(message = '上传成功') {
  closeToast()
  showToast({
    message,
    position: 'top',
    duration: 2000,
    className: 'upload-success-toast'
  })
}

/** 提交前缺少有效照片时的提示（对话框，便于阅读） */
export function showMissingPhotosDialog(scene = '核查') {
  return showDialog({
    title: '缺少现场照片',
    message: `请先上传${scene}现场照片，并等待出现「上传成功」后再提交。\n\n若选图后无成功提示，请先启动 MinIO（start-minio.ps1，端口 9000）。`,
    confirmButtonText: '知道了',
    messageAlign: 'left',
    className: 'upload-error-dialog'
  })
}

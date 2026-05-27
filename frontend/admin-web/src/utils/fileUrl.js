import axios from 'axios'
import { getToken } from '@/utils/auth'

/**
 * 通过后端 /file/download 带 Token 拉取文件，返回可用于 img/video 的 blob URL
 * （兼容未重启、尚无 /file/preview 的后端）
 */
export async function fetchFilePreviewBlobUrl(filePath) {
  if (!filePath || typeof filePath !== 'string') {
    return ''
  }
  const token = getToken()
  const headers = token ? { Authorization: `Bearer ${token}` } : {}
  const params = { fileUrl: filePath.trim() }

  let res
  try {
    res = await axios.get('/api/file/preview', {
      params,
      responseType: 'blob',
      headers
    })
  } catch (e) {
    if (e?.response?.status !== 404) {
      throw e
    }
    res = await axios.get('/api/file/download', {
      params,
      responseType: 'blob',
      headers
    })
  }

  const blob = res.data
  if (!(blob instanceof Blob) || blob.size === 0) {
    return ''
  }
  if (blob.type && blob.type.includes('application/json')) {
    return ''
  }
  return URL.createObjectURL(blob)
}

export function revokeBlobUrl(url) {
  if (url && typeof url === 'string' && url.startsWith('blob:')) {
    URL.revokeObjectURL(url)
  }
}

export function revokeBlobUrls(urls) {
  if (!Array.isArray(urls)) {
    return
  }
  urls.forEach(revokeBlobUrl)
}

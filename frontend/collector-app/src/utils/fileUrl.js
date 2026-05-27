import axios from 'axios'
import { getToken } from '@/utils/auth'

/**
 * 通过后端 /file/preview 带 Token 拉取文件，返回可用于 img 的 blob URL
 * （MinIO 直链在移动端常因鉴权/跨域无法显示）
 */
export async function fetchFilePreviewBlobUrl(filePath) {
  if (!filePath || typeof filePath !== 'string') {
    return ''
  }
  const token = getToken()
  const headers = token ? { Authorization: `Bearer ${token}` } : {}
  const params = { fileUrl: filePath }

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

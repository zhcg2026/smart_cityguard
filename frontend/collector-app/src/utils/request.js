import axios from 'axios'
import { showToast } from 'vant'
import { getToken, removeToken } from '@/utils/auth'
import router from '@/router'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

service.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      showToast(res.message || '请求失败')
      if (res.code === 401) {
        removeToken()
        localStorage.removeItem('userInfo')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  error => {
    const status = error.response?.status
    const body = error.response?.data
    const msg = typeof body === 'object' && body?.message ? body.message : null

    // 调用方自行处理错误提示时跳过全局 toast
    if (error.config?.skipErrorToast) {
      return Promise.reject(error)
    }

    // Spring Security 未认证/令牌失效时返回 HTTP 401（无业务 code 包装）
    if (status === 401) {
      showToast(msg || '登录已失效，请重新登录')
      removeToken()
      localStorage.removeItem('userInfo')
      router.push('/login')
      return Promise.reject(error)
    }

    showToast(msg || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
// 请求工具
const BASE_URL = '/api'

let requestInterceptor = null
let responseInterceptor = null

export function initRequest() {
  // 请求拦截器
  requestInterceptor = (config) => {
    const token = uni.getStorageSync('token')
    if (token) {
      config.header = {
        ...config.header,
        'Authorization': `Bearer ${token}`
      }
    }
    return config
  }

  // 响应拦截器
  responseInterceptor = (response) => {
    const { statusCode, data } = response

    if (statusCode === 200) {
      if (data.code === 200) {
        return data
      } else {
        uni.showToast({
          title: data.message || '请求失败',
          icon: 'none'
        })
        return Promise.reject(data)
      }
    } else if (statusCode === 401) {
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
      uni.reLaunch({ url: '/pages/user/login' })
      return Promise.reject(response)
    } else {
      uni.showToast({
        title: '请求失败',
        icon: 'none'
      })
      return Promise.reject(response)
    }
  }
}

// 通用请求方法
function request(options) {
  const config = requestInterceptor({
    ...options,
    url: BASE_URL + options.url,
    header: {
      'Content-Type': 'application/json',
      ...options.header
    }
  })

  return new Promise((resolve, reject) => {
    uni.request({
      ...config,
      success: (response) => {
        responseInterceptor(response)
          .then(resolve)
          .catch(reject)
      },
      fail: (error) => {
        uni.showToast({
          title: '网络请求失败',
          icon: 'none'
        })
        reject(error)
      }
    })
  })
}

// GET请求
export function get(url, params = {}) {
  return request({
    url,
    method: 'GET',
    data: params
  })
}

// POST请求
export function post(url, data = {}) {
  return request({
    url,
    method: 'POST',
    data
  })
}

// PUT请求
export function put(url, data = {}) {
  return request({
    url,
    method: 'PUT',
    data
  })
}

// DELETE请求
export function del(url, data = {}) {
  return request({
    url,
    method: 'DELETE',
    data
  })
}

// 文件上传
export function upload(filePath, formData = {}) {
  const token = uni.getStorageSync('token')

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/file/upload',
      filePath,
      name: 'file',
      formData,
      header: {
        'Authorization': `Bearer ${token}`
      },
      success: (response) => {
        const data = JSON.parse(response.data)
        if (data.code === 200) {
          resolve(data)
        } else {
          uni.showToast({
            title: data.message || '上传失败',
            icon: 'none'
          })
          reject(data)
        }
      },
      fail: reject
    })
  })
}

export default { get, post, put, del, upload }
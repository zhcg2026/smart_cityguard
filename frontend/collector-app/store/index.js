import { getUserInfo } from '../utils/api'

const store = {
  state: {
    token: uni.getStorageSync('token') || '',
    userInfo: uni.getStorageSync('userInfo') || null,
    location: null
  },

  mutations: {
    setToken(state, token) {
      state.token = token
      uni.setStorageSync('token', token)
    },

    setUserInfo(state, userInfo) {
      state.userInfo = userInfo
      uni.setStorageSync('userInfo', userInfo)
    },

    setLocation(state, location) {
      state.location = location
    },

    clearUser(state) {
      state.token = ''
      state.userInfo = null
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
    }
  },

  actions: {
    // 获取用户信息
    async getUserInfo({ state, mutations }) {
      if (!state.token) return null

      try {
        const res = await getUserInfo()
        mutations.setUserInfo(res.data.user)
        return res.data
      } catch (error) {
        console.error('获取用户信息失败:', error)
        return null
      }
    },

    // 获取定位
    async getLocation({ mutations }) {
      try {
        const location = await uni.getLocation({ type: 'gcj02' })
        mutations.setLocation({
          longitude: location.longitude,
          latitude: location.latitude
        })
        return location
      } catch (error) {
        console.error('获取定位失败:', error)
        return null
      }
    }
  },

  getters: {
    isLoggedIn: (state) => !!state.token,
    userName: (state) => state.userInfo?.realName || '采集员',
    userId: (state) => state.userInfo?.id || ''
  }
}

export default store
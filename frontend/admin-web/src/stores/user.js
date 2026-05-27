import { defineStore } from 'pinia'
import { login, logout, getUserInfo } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: {},
    roles: [],
    permissions: [],
    menus: []
  }),

  actions: {
    // 初始化用户信息（从localStorage恢复）
    initUser() {
      try {
        const userInfo = localStorage.getItem('userInfo')
        if (userInfo && userInfo !== 'undefined') {
          const u = JSON.parse(userInfo)
          this.userInfo = u
          this.roles = u.roles || []
        }
      } catch (e) {
        console.warn('Failed to parse userInfo from localStorage')
        this.userInfo = {}
        this.roles = []
      }
    },

    // 登录
    async login(loginForm) {
      const { username, password } = loginForm
      const res = await login({ username, password })
      const token = res.data.token
      this.token = token
      setToken(token)
      return res
    },

    // 获取用户信息
    async getUserInfo() {
      const res = await getUserInfo()
      // 后端直接返回 LoginUser 对象
      const userData = res.data
      this.userInfo = userData
      this.roles = userData.roles || []
      this.permissions = []
      this.menus = []
      localStorage.setItem('userInfo', JSON.stringify(userData))
      return res
    },

    // 登出
    async logout() {
      await logout()
      this.token = ''
      this.userInfo = {}
      this.roles = []
      this.permissions = []
      this.menus = []
      removeToken()
      localStorage.removeItem('userInfo')
    }
  }
})
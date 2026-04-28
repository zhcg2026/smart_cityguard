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
      const userInfo = localStorage.getItem('userInfo')
      if (userInfo) {
        this.userInfo = JSON.parse(userInfo)
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
      const { user, roles, permissions, menus } = res.data
      this.userInfo = user
      this.roles = roles
      this.permissions = permissions
      this.menus = menus
      localStorage.setItem('userInfo', JSON.stringify(user))
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
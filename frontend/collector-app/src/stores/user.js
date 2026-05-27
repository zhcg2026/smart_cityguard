import { defineStore } from 'pinia'
import { login, getUserInfo } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: {},
    roles: []
  }),

  getters: {
    userId: (state) => state.userInfo?.id ?? state.userInfo?.userId ?? null
  },

  actions: {
    initUser() {
      try {
        const userInfo = localStorage.getItem('userInfo')
        if (userInfo && userInfo !== 'undefined') {
          this.userInfo = JSON.parse(userInfo)
          this.roles = this.userInfo.roles || []
        }
      } catch (e) {
        this.userInfo = {}
        this.roles = []
      }
    },

    async login(loginForm) {
      const { username, password } = loginForm
      const res = await login({ username, password })
      const token = res.data.token
      this.token = token
      setToken(token)
      return res
    },

    async getUserInfo() {
      const res = await getUserInfo()
      const userData = res.data
      this.userInfo = userData
      this.roles = userData?.roles || []
      localStorage.setItem('userInfo', JSON.stringify(userData))
      return res
    },

    async logout() {
      removeToken()
      localStorage.removeItem('userInfo')
      this.token = ''
      this.userInfo = {}
      this.roles = []
    }
  }
})
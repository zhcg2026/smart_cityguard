import { defineStore } from 'pinia'
import { login, getUserInfo } from '@/api/user'
import { getCollectorRespGrids } from '@/api/geo'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { isCollectorMobileUser } from '@/utils/roleAccess'

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

    async enrichCollectorGrids(userData) {
      if (!userData || !isCollectorMobileUser(userData.roles)) {
        return userData
      }
      const userId = userData.id ?? userData.userId
      if (!userId) {
        return userData
      }
      try {
        const res = await getCollectorRespGrids(userId)
        const grids = res.data || []
        const names = grids.map((g) => g.respGridName || g.respGridCode).filter(Boolean)
        userData.respGrids = grids
        userData.gridName = names.length ? names.join('、') : ''
      } catch {
        userData.gridName = userData.gridName || ''
      }
      return userData
    },

    async getUserInfo() {
      const res = await getUserInfo()
      const userData = await this.enrichCollectorGrids(res.data || {})
      this.userInfo = userData
      this.roles = userData?.roles || []
      localStorage.setItem('userInfo', JSON.stringify(userData))
      return { ...res, data: userData }
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
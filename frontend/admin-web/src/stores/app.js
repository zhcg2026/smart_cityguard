import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebar: {
      collapsed: localStorage.getItem('sidebarCollapsed') === 'true'
    },
    device: 'desktop',
    size: localStorage.getItem('size') || 'default'
  }),

  actions: {
    toggleSidebar() {
      this.sidebar.collapsed = !this.sidebar.collapsed
      localStorage.setItem('sidebarCollapsed', this.sidebar.collapsed)
    },

    closeSidebar() {
      this.sidebar.collapsed = true
      localStorage.setItem('sidebarCollapsed', 'true')
    },

    toggleDevice(device) {
      this.device = device
    },

    setSize(size) {
      this.size = size
      localStorage.setItem('size', size)
    }
  }
})
import { initRequest } from './utils/request'

export default {
  install(Vue) {
    // 初始化请求配置
    initRequest()

    // 全局方法
    Vue.prototype.$showToast = (title, icon = 'none') => {
      uni.showToast({ title, icon })
    }

    Vue.prototype.$showLoading = (title = '加载中') => {
      uni.showLoading({ title, mask: true })
    }

    Vue.prototype.$hideLoading = () => {
      uni.hideLoading()
    }

    Vue.prototype.$navigateTo = (url) => {
      uni.navigateTo({ url })
    }

    Vue.prototype.$reLaunch = (url) => {
      uni.reLaunch({ url })
    }

    Vue.prototype.$getLocation = () => {
      return new Promise((resolve, reject) => {
        uni.getLocation({
          type: 'gcj02',
          success: resolve,
          fail: reject
        })
      })
    }

    Vue.prototype.$chooseImage = (count = 9) => {
      return new Promise((resolve, reject) => {
        uni.chooseImage({
          count,
          sizeType: ['compressed'],
          sourceType: ['camera', 'album'],
          success: resolve,
          fail: reject
        })
      })
    }

    Vue.prototype.$chooseVideo = () => {
      return new Promise((resolve, reject) => {
        uni.chooseVideo({
          sourceType: ['camera', 'album'],
          maxDuration: 60,
          success: resolve,
          fail: reject
        })
      })
    }
  }
}
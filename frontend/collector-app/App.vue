<script>
export default {
  onLaunch: function() {
    console.log('App Launch')
    // 检查登录状态
    const token = uni.getStorageSync('token')
    if (!token) {
      uni.reLaunch({ url: '/pages/user/login' })
    }
  },
  onShow: function() {
    console.log('App Show')
  },
  onHide: function() {
    console.log('App Hide')
  }
}
</script>

<style>
/* 全局样式 */
page {
  background-color: #f8f8f8;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 通用样式 */
.container {
  padding: 20rpx;
}

.card {
  background-color: #ffffff;
  border-radius: 8rpx;
  margin-bottom: 20rpx;
  padding: 20rpx;
}

/* 主题色 */
.primary-color {
  color: #409eff;
}

.primary-bg {
  background-color: #409eff;
}
</style>
<template>
  <view class="user-page">
    <!-- 用户信息 -->
    <view class="user-card">
      <view class="user-avatar">
        <image :src="userInfo.avatar || '/static/images/avatar.png'" mode="aspectFill" />
      </view>
      <view class="user-info">
        <view class="user-name">{{ userInfo.realName }}</view>
        <view class="user-position">{{ userInfo.positionName }}</view>
        <view class="user-dept">{{ userInfo.departmentName }}</view>
      </view>
    </view>

    <!-- 统计信息 -->
    <view class="stats-card">
      <view class="stat-item">
        <view class="stat-value">{{ statistics.todayReport }}</view>
        <view class="stat-label">今日上报</view>
      </view>
      <view class="stat-item">
        <view class="stat-value">{{ statistics.monthReport }}</view>
        <view class="stat-label">本月上报</view>
      </view>
      <view class="stat-item">
        <view class="stat-value">{{ statistics.totalReport }}</view>
        <view class="stat-label">累计上报</view>
      </view>
      <view class="stat-item">
        <view class="stat-value">{{ statistics.validRate }}%</view>
        <view class="stat-label">有效率</view>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="menu-card">
      <view class="menu-item" @click="navigateTo('/pages/message/index')">
        <view class="menu-icon message">
          <text class="iconfont">&#xe60c;</text>
        </view>
        <view class="menu-label">消息通知</view>
        <view class="menu-badge" v-if="unreadCount > 0">{{ unreadCount }}</view>
        <view class="menu-arrow">
          <text class="iconfont">&#xe600;</text>
        </view>
      </view>
      <view class="menu-item" @click="navigateTo('/pages/appeal/index')">
        <view class="menu-icon appeal">
          <text class="iconfont">&#xe60d;</text>
        </view>
        <view class="menu-label">申诉申请</view>
        <view class="menu-arrow">
          <text class="iconfont">&#xe600;</text>
        </view>
      </view>
      <view class="menu-item" @click="navigateTo('/pages/user/history')">
        <view class="menu-icon history">
          <text class="iconfont">&#xe60e;</text>
        </view>
        <view class="menu-label">上报记录</view>
        <view class="menu-arrow">
          <text class="iconfont">&#xe600;</text>
        </view>
      </view>
      <view class="menu-item" @click="navigateTo('/pages/user/profile')">
        <view class="menu-icon profile">
          <text class="iconfont">&#xe60f;</text>
        </view>
        <view class="menu-label">个人信息</view>
        <view class="menu-arrow">
          <text class="iconfont">&#xe600;</text>
        </view>
      </view>
    </view>

    <!-- 设置 -->
    <view class="menu-card">
      <view class="menu-item" @click="checkUpdate">
        <view class="menu-icon update">
          <text class="iconfont">&#xe610;</text>
        </view>
        <view class="menu-label">检查更新</view>
        <view class="menu-value">V1.0.0</view>
        <view class="menu-arrow">
          <text class="iconfont">&#xe600;</text>
        </view>
      </view>
      <view class="menu-item" @click="clearCache">
        <view class="menu-icon clear">
          <text class="iconfont">&#xe611;</text>
        </view>
        <view class="menu-label">清除缓存</view>
        <view class="menu-arrow">
          <text class="iconfont">&#xe600;</text>
        </view>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-btn" @click="logout">
      退出登录
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      userInfo: {},
      statistics: {
        todayReport: 0,
        monthReport: 0,
        totalReport: 0,
        validRate: 0
      },
      unreadCount: 0
    }
  },
  onLoad() {
    this.initPage()
  },
  onShow() {
    this.loadStatistics()
    this.loadUnreadCount()
  },
  methods: {
    initPage() {
      const userInfo = uni.getStorageSync('userInfo')
      if (userInfo) {
        this.userInfo = userInfo
      }
    },
    loadStatistics() {
      // TODO: 获取统计数据
      this.statistics = {
        todayReport: 3,
        monthReport: 45,
        totalReport: 156,
        validRate: 92
      }
    },
    loadUnreadCount() {
      // TODO: 获取未读消息数
      this.unreadCount = 5
    },
    navigateTo(url) {
      uni.navigateTo({ url })
    },
    checkUpdate() {
      uni.showToast({ title: '已是最新版本', icon: 'success' })
    },
    clearCache() {
      uni.showModal({
        title: '提示',
        content: '确定要清除缓存吗？',
        success: (res) => {
          if (res.confirm) {
            // 清除缓存（保留用户登录信息）
            uni.showToast({ title: '缓存已清除', icon: 'success' })
          }
        }
      })
    },
    logout() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            uni.removeStorageSync('token')
            uni.removeStorageSync('userInfo')
            uni.reLaunch({ url: '/pages/user/login' })
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.user-page {
  min-height: 100vh;
  background-color: #f8f8f8;
  padding: 20rpx;

  .user-card {
    background: linear-gradient(135deg, #409eff, #66b1ff);
    border-radius: 12rpx;
    padding: 40rpx;
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;

    .user-avatar {
      width: 100rpx;
      height: 100rpx;
      border-radius: 50rpx;
      border: 2rpx solid #fff;
      overflow: hidden;

      image {
        width: 100%;
        height: 100%;
      }
    }

    .user-info {
      margin-left: 30rpx;

      .user-name {
        font-size: 36rpx;
        font-weight: bold;
        color: #fff;
      }

      .user-position, .user-dept {
        font-size: 24rpx;
        color: rgba(255, 255, 255, 0.8);
        margin-top: 10rpx;
      }
    }
  }

  .stats-card {
    display: flex;
    background-color: #fff;
    border-radius: 12rpx;
    padding: 30rpx;
    margin-bottom: 20rpx;

    .stat-item {
      flex: 1;
      text-align: center;

      .stat-value {
        font-size: 40rpx;
        font-weight: bold;
        color: #409eff;
      }

      .stat-label {
        font-size: 24rpx;
        color: #909399;
        margin-top: 10rpx;
      }
    }
  }

  .menu-card {
    background-color: #fff;
    border-radius: 12rpx;
    margin-bottom: 20rpx;

    .menu-item {
      display: flex;
      align-items: center;
      padding: 30rpx;
      border-bottom: 1rpx solid #eee;

      &:last-child {
        border-bottom: none;
      }

      .menu-icon {
        width: 60rpx;
        height: 60rpx;
        border-radius: 30rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 30rpx;
        color: #fff;

        &.message { background-color: #409eff; }
        &.appeal { background-color: #e6a23c; }
        &.history { background-color: #67c23a; }
        &.profile { background-color: #909399; }
        &.update { background-color: #409eff; }
        &.clear { background-color: #f56c6c; }
      }

      .menu-label {
        flex: 1;
        font-size: 28rpx;
        color: #303133;
        margin-left: 20rpx;
      }

      .menu-badge {
        background-color: #ff4d4f;
        color: #fff;
        font-size: 20rpx;
        padding: 2rpx 10rpx;
        border-radius: 10rpx;
        margin-right: 10rpx;
      }

      .menu-value {
        font-size: 24rpx;
        color: #909399;
        margin-right: 10rpx;
      }

      .menu-arrow {
        color: #c0c4cc;
        font-size: 24rpx;
      }
    }
  }

  .logout-btn {
    background-color: #fff;
    border-radius: 12rpx;
    padding: 30rpx;
    text-align: center;
    color: #ff4d4f;
    font-size: 32rpx;
  }
}
</style>
<template>
  <view class="index-page">
    <!-- 用户信息 -->
    <view class="user-info">
      <view class="user-avatar">
        <image src="/static/images/avatar.png" mode="aspectFill" />
      </view>
      <view class="user-detail">
        <view class="user-name">{{ userName }}</view>
        <view class="user-grid">{{ gridName }}</view>
      </view>
      <view class="weather-info">
        <view class="weather">{{ weather }}</view>
        <view class="temperature">{{ temperature }}°C</view>
      </view>
    </view>

    <!-- 统计信息 -->
    <view class="statistics">
      <view class="stat-item">
        <view class="stat-value">{{ statistics.todayReport }}</view>
        <view class="stat-label">今日上报</view>
      </view>
      <view class="stat-item">
        <view class="stat-value">{{ statistics.pendingVerify }}</view>
        <view class="stat-label">待核查任务</view>
      </view>
      <view class="stat-item">
        <view class="stat-value">{{ statistics.pendingCheck }}</view>
        <view class="stat-label">待核实任务</view>
      </view>
      <view class="stat-item">
        <view class="stat-value">{{ statistics.monthReport }}</view>
        <view class="stat-label">本月上报</view>
      </view>
    </view>

    <!-- 快捷功能 -->
    <view class="quick-actions">
      <view class="action-item" @click="navigateTo('/pages/report/index')">
        <view class="action-icon report">
          <text class="iconfont">&#xe604;</text>
        </view>
        <view class="action-label">问题上报</view>
      </view>
      <view class="action-item" @click="navigateTo('/pages/task/index?type=verify')">
        <view class="action-icon verify">
          <text class="iconfont">&#xe602;</text>
        </view>
        <view class="action-label">核查任务</view>
      </view>
      <view class="action-item" @click="navigateTo('/pages/task/index?type=check')">
        <view class="action-icon check">
          <text class="iconfont">&#xe603;</text>
        </view>
        <view class="action-label">核实任务</view>
      </view>
      <view class="action-item" @click="navigateTo('/pages/appeal/index')">
        <view class="action-icon appeal">
          <text class="iconfont">&#xe605;</text>
        </view>
        <view class="action-label">申诉申请</view>
      </view>
    </view>

    <!-- 今日提示 -->
    <view class="section-card">
      <view class="section-header">
        <text class="section-title">今日提示</text>
        <text class="section-more" @click="navigateTo('/pages/message/index?type=tip')">更多</text>
      </view>
      <view class="tip-list">
        <view v-for="tip in dailyTips" :key="tip.id" class="tip-item">
          <view class="tip-icon">
            <text class="iconfont">&#xe606;</text>
          </view>
          <view class="tip-content">{{ tip.content }}</view>
        </view>
        <view v-if="dailyTips.length === 0" class="empty-tip">暂无今日提示</view>
      </view>
    </view>

    <!-- 公文通告 -->
    <view class="section-card">
      <view class="section-header">
        <text class="section-title">公文通告</text>
        <text class="section-more" @click="navigateTo('/pages/message/index?type=announcement')">更多</text>
      </view>
      <view class="announcement-list">
        <view v-for="item in announcements" :key="item.id" class="announcement-item" @click="viewAnnouncement(item)">
          <view class="announcement-tag" :class="{ urgent: item.isUrgent }">
            {{ item.isUrgent ? '紧急' : '普通' }}
          </view>
          <view class="announcement-title">{{ item.title }}</view>
          <view class="announcement-time">{{ item.publishTime }}</view>
        </view>
        <view v-if="announcements.length === 0" class="empty-tip">暂无公文通告</view>
      </view>
    </view>
  </view>
</template>

<script>
import { getDailyTips, getAnnouncements } from '../../utils/api'
import store from '../../store/index'

export default {
  data() {
    return {
      userName: '',
      gridName: '采集员网格',
      weather: '晴',
      temperature: '25',
      statistics: {
        todayReport: 0,
        pendingVerify: 0,
        pendingCheck: 0,
        monthReport: 0
      },
      dailyTips: [],
      announcements: []
    }
  },
  onLoad() {
    this.initPage()
  },
  onShow() {
    this.loadStatistics()
  },
  methods: {
    async initPage() {
      // 设置用户信息
      const userInfo = uni.getStorageSync('userInfo')
      if (userInfo) {
        this.userName = userInfo.realName || '采集员'
        this.gridName = userInfo.gridName || '采集员网格'
      }

      // 加载今日提示
      await this.loadDailyTips()

      // 加载公文通告
      await this.loadAnnouncements()

      // 加载统计数据
      await this.loadStatistics()
    },
    async loadDailyTips() {
      try {
        const res = await getDailyTips()
        this.dailyTips = res.data || []
      } catch (error) {
        console.error('获取今日提示失败:', error)
      }
    },
    async loadAnnouncements() {
      try {
        const res = await getAnnouncements({ limit: 5 })
        this.announcements = res.data?.list || []
      } catch (error) {
        console.error('获取公文通告失败:', error)
      }
    },
    async loadStatistics() {
      // TODO: 从API获取统计数据
      this.statistics = {
        todayReport: 3,
        pendingVerify: 2,
        pendingCheck: 1,
        monthReport: 45
      }
    },
    navigateTo(url) {
      uni.navigateTo({ url })
    },
    viewAnnouncement(item) {
      uni.navigateTo({
        url: `/pages/message/index?type=announcement&id=${item.id}`
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.index-page {
  padding: 20rpx;
  background-color: #f8f8f8;
  min-height: 100vh;

  .user-info {
    display: flex;
    align-items: center;
    padding: 30rpx;
    background: linear-gradient(135deg, #409eff, #66b1ff);
    border-radius: 12rpx;
    margin-bottom: 20rpx;

    .user-avatar {
      width: 80rpx;
      height: 80rpx;
      border-radius: 40rpx;
      border: 2rpx solid #fff;
      overflow: hidden;

      image {
        width: 100%;
        height: 100%;
      }
    }

    .user-detail {
      flex: 1;
      margin-left: 20rpx;

      .user-name {
        font-size: 32rpx;
        font-weight: bold;
        color: #fff;
      }

      .user-grid {
        font-size: 24rpx;
        color: rgba(255, 255, 255, 0.8);
        margin-top: 10rpx;
      }
    }

    .weather-info {
      text-align: right;

      .weather {
        font-size: 24rpx;
        color: #fff;
      }

      .temperature {
        font-size: 36rpx;
        font-weight: bold;
        color: #fff;
      }
    }
  }

  .statistics {
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
        color: #666;
        margin-top: 10rpx;
      }
    }
  }

  .quick-actions {
    display: flex;
    background-color: #fff;
    border-radius: 12rpx;
    padding: 30rpx;
    margin-bottom: 20rpx;

    .action-item {
      flex: 1;
      text-align: center;

      .action-icon {
        width: 80rpx;
        height: 80rpx;
        border-radius: 40rpx;
        margin: 0 auto;
        display: flex;
        align-items: center;
        justify-content: center;

        .iconfont {
          font-size: 40rpx;
          color: #fff;
        }

        &.report {
          background: linear-gradient(135deg, #f5af19, #f12711);
        }

        &.verify {
          background: linear-gradient(135deg, #667eea, #764ba2);
        }

        &.check {
          background: linear-gradient(135deg, #11998e, #38ef7d);
        }

        &.appeal {
          background: linear-gradient(135deg, #ff416c, #ff4b2b);
        }
      }

      .action-label {
        font-size: 24rpx;
        color: #666;
        margin-top: 15rpx;
      }
    }
  }

  .section-card {
    background-color: #fff;
    border-radius: 12rpx;
    padding: 30rpx;
    margin-bottom: 20rpx;

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20rpx;

      .section-title {
        font-size: 30rpx;
        font-weight: bold;
        color: #333;
      }

      .section-more {
        font-size: 24rpx;
        color: #409eff;
      }
    }

    .tip-list, .announcement-list {
      .tip-item, .announcement-item {
        display: flex;
        align-items: center;
        padding: 20rpx 0;
        border-bottom: 1rpx solid #eee;

        .tip-icon {
          color: #409eff;
          margin-right: 15rpx;
        }

        .tip-content {
          font-size: 26rpx;
          color: #666;
        }
      }

      .announcement-item {
        .announcement-tag {
          padding: 5rpx 15rpx;
          border-radius: 4rpx;
          font-size: 22rpx;
          background-color: #e6f7ff;
          color: #409eff;

          &.urgent {
            background-color: #fff2f0;
            color: #ff4d4f;
          }
        }

        .announcement-title {
          flex: 1;
          margin-left: 15rpx;
          font-size: 26rpx;
          color: #333;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .announcement-time {
          font-size: 24rpx;
          color: #999;
        }
      }

      .empty-tip {
        text-align: center;
        padding: 40rpx;
        color: #999;
        font-size: 26rpx;
      }
    }
  }
}
</style>
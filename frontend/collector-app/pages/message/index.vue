<template>
  <view class="message-page">
    <!-- 消息类型 -->
    <view class="message-tabs">
      <view class="tab-item" :class="{ active: currentTab === 'all' }" @click="changeTab('all')">
        全部
      </view>
      <view class="tab-item" :class="{ active: currentTab === 'system' }" @click="changeTab('system')">
        系统消息
      </view>
      <view class="tab-item" :class="{ active: currentTab === 'task' }" @click="changeTab('task')">
        任务消息
      </view>
      <view class="tab-item" :class="{ active: currentTab === 'announcement' }" @click="changeTab('announcement')">
        公文通告
      </view>
    </view>

    <!-- 消息列表 -->
    <view class="message-list">
      <view v-for="item in messageList" :key="item.id" class="message-item" @click="viewMessage(item)">
        <view class="message-icon" :class="item.type">
          <text class="iconfont">{{ getIcon(item.type) }}</text>
        </view>
        <view class="message-content">
          <view class="message-header">
            <view class="message-title">{{ item.title }}</view>
            <view class="message-time">{{ item.createTime }}</view>
          </view>
          <view class="message-body">{{ item.content }}</view>
          <view class="message-status" v-if="!item.isRead">未读</view>
        </view>
      </view>

      <view v-if="messageList.length === 0" class="empty-state">
        <image src="/static/images/empty.png" mode="aspectFit" />
        <view class="empty-text">暂无消息</view>
      </view>
    </view>
  </view>
</template>

<script>
import { getMessageList } from '../../utils/api'

export default {
  data() {
    return {
      currentTab: 'all',
      messageList: []
    }
  },
  onLoad(options) {
    if (options.type) {
      this.currentTab = options.type
    }
    this.loadMessageList()
  },
  methods: {
    changeTab(tab) {
      this.currentTab = tab
      this.loadMessageList()
    },
    async loadMessageList() {
      try {
        const params = {}
        if (this.currentTab !== 'all') {
          params.type = this.currentTab
        }
        const res = await getMessageList({ pageNum: 1, pageSize: 100, ...params })
        this.messageList = res.data?.list || []
      } catch (error) {
        console.error('获取消息列表失败:', error)
      }
    },
    getIcon(type) {
      const icons = {
        system: '',
        task: '',
        announcement: ''
      }
      return icons[type] || ''
    },
    viewMessage(item) {
      // TODO: 打开消息详情
      uni.showToast({ title: item.title, icon: 'none' })
    }
  }
}
</script>

<style lang="scss" scoped>
.message-page {
  min-height: 100vh;
  background-color: #f8f8f8;

  .message-tabs {
    display: flex;
    background-color: #fff;
    padding: 20rpx;

    .tab-item {
      flex: 1;
      text-align: center;
      padding: 15rpx 0;
      border-radius: 8rpx;
      background-color: #f5f7fa;
      color: #606266;
      font-size: 24rpx;
      margin-right: 15rpx;

      &:last-child {
        margin-right: 0;
      }

      &.active {
        background-color: #409eff;
        color: #fff;
      }
    }
  }

  .message-list {
    padding: 20rpx;

    .message-item {
      display: flex;
      background-color: #fff;
      border-radius: 12rpx;
      padding: 30rpx;
      margin-bottom: 20rpx;

      .message-icon {
        width: 60rpx;
        height: 60rpx;
        border-radius: 30rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 30rpx;
        color: #fff;

        &.system { background-color: #409eff; }
        &.task { background-color: #e6a23c; }
        &.announcement { background-color: #67c23a; }
      }

      .message-content {
        flex: 1;
        margin-left: 20rpx;

        .message-header {
          display: flex;
          justify-content: space-between;
          margin-bottom: 10rpx;

          .message-title {
            font-size: 28rpx;
            color: #303133;
            font-weight: bold;
          }

          .message-time {
            font-size: 24rpx;
            color: #909399;
          }
        }

        .message-body {
          font-size: 26rpx;
          color: #606266;
          overflow: hidden;
          text-overflow: ellipsis;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
        }

        .message-status {
          margin-top: 10rpx;
          padding: 2rpx 10rpx;
          border-radius: 4rpx;
          font-size: 22rpx;
          background-color: #ff4d4f;
          color: #fff;
        }
      }
    }

    .empty-state {
      padding: 100rpx;
      text-align: center;

      image {
        width: 200rpx;
        height: 200rpx;
      }

      .empty-text {
        font-size: 26rpx;
        color: #909399;
        margin-top: 20rpx;
      }
    }
  }
}
</style>
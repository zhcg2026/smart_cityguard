<template>
  <view class="task-page">
    <!-- 任务类型切换 -->
    <view class="task-tabs">
      <view class="tab-item" :class="{ active: currentTab === 'verify' }" @click="changeTab('verify')">
        核查任务
        <view class="tab-count">{{ verifyCount }}</view>
      </view>
      <view class="tab-item" :class="{ active: currentTab === 'check' }" @click="changeTab('check')">
        核实任务
        <view class="tab-count">{{ checkCount }}</view>
      </view>
    </view>

    <!-- 任务列表 -->
    <view class="task-list">
      <view v-for="item in taskList" :key="item.id" class="task-item" @click="goTaskDetail(item.id)">
        <view class="task-header">
          <view class="task-type" :class="currentTab">
            {{ currentTab === 'verify' ? '核查' : '核实' }}
          </view>
          <view class="task-no">{{ item.taskNo }}</view>
          <view class="task-status" :class="item.status">
            {{ item.status === 'PENDING' ? '待执行' : '已完成' }}
          </view>
        </view>

        <view class="task-body">
          <view class="task-case-no">关联案件: {{ item.caseNo }}</view>
          <view class="task-address">{{ item.address }}</view>
          <view class="task-category">{{ item.categorySmallName }}</view>
        </view>

        <view class="task-footer">
          <view class="task-deadline">
            <text class="iconfont">&#xe60a;</text>
            截止: {{ item.deadline }}
          </view>
          <view class="task-remaining" :class="{ overdue: item.isOverdue }">
            {{ item.timeRemaining || '已超时' }}
          </view>
        </view>
      </view>

      <view v-if="taskList.length === 0" class="empty-state">
        <image src="/static/images/empty.png" mode="aspectFit" />
        <view class="empty-text">暂无任务</view>
      </view>
    </view>
  </view>
</template>

<script>
import { getVerifyTaskList, getCheckTaskList } from '../../utils/api'

export default {
  data() {
    return {
      currentTab: 'verify',
      taskList: [],
      verifyCount: 0,
      checkCount: 0
    }
  },
  onLoad(options) {
    if (options.type) {
      this.currentTab = options.type
    }
    this.loadTaskList()
  },
  methods: {
    async changeTab(tab) {
      this.currentTab = tab
      this.loadTaskList()
    },
    async loadTaskList() {
      try {
        const res = this.currentTab === 'verify'
          ? await getVerifyTaskList({ pageNum: 1, pageSize: 100 })
          : await getCheckTaskList({ pageNum: 1, pageSize: 100 })

        this.taskList = res.data?.list || []
        this[this.currentTab === 'verify' ? 'verifyCount' : 'checkCount'] = res.data?.total || 0
      } catch (error) {
        console.error('获取任务列表失败:', error)
      }
    },
    goTaskDetail(id) {
      uni.navigateTo({
        url: `/pages/task/detail?id=${id}&type=${this.currentTab}`
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.task-page {
  min-height: 100vh;
  background-color: #f8f8f8;

  .task-tabs {
    display: flex;
    background-color: #fff;
    padding: 30rpx;

    .tab-item {
      flex: 1;
      text-align: center;
      padding: 20rpx 0;
      border-radius: 8rpx;
      background-color: #f5f7fa;
      color: #606266;
      margin-right: 20rpx;

      &:last-child {
        margin-right: 0;
      }

      .tab-count {
        display: inline-block;
        background-color: #ff4d4f;
        color: #fff;
        font-size: 20rpx;
        padding: 2rpx 10rpx;
        border-radius: 10rpx;
        margin-left: 10rpx;
      }

      &.active {
        background-color: #409eff;
        color: #fff;

        .tab-count {
          background-color: #fff;
          color: #409eff;
        }
      }
    }
  }

  .task-list {
    padding: 20rpx;

    .task-item {
      background-color: #fff;
      border-radius: 12rpx;
      padding: 30rpx;
      margin-bottom: 20rpx;

      .task-header {
        display: flex;
        align-items: center;
        margin-bottom: 20rpx;

        .task-type {
          padding: 5rpx 15rpx;
          border-radius: 4rpx;
          font-size: 22rpx;

          &.verify {
            background-color: #f0f9eb;
            color: #67c23a;
          }

          &.check {
            background-color: #fdf6ec;
            color: #e6a23c;
          }
        }

        .task-no {
          flex: 1;
          font-size: 26rpx;
          color: #606266;
          margin-left: 15rpx;
        }

        .task-status {
          padding: 5rpx 15rpx;
          border-radius: 4rpx;
          font-size: 22rpx;

          &.PENDING {
            background-color: #fff2f0;
            color: #ff4d4f;
          }

          &.COMPLETED {
            background-color: #f0f9eb;
            color: #67c23a;
          }
        }
      }

      .task-body {
        .task-case-no {
          font-size: 24rpx;
          color: #909399;
          margin-bottom: 10rpx;
        }

        .task-address {
          font-size: 28rpx;
          color: #303133;
          margin-bottom: 10rpx;
        }

        .task-category {
          font-size: 24rpx;
          color: #909399;
        }
      }

      .task-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding-top: 20rpx;
        border-top: 1rpx solid #eee;

        .task-deadline {
          display: flex;
          align-items: center;
          font-size: 24rpx;
          color: #909399;

          .iconfont {
            color: #909399;
            margin-right: 5rpx;
          }
        }

        .task-remaining {
          font-size: 26rpx;
          color: #409eff;

          &.overdue {
            color: #ff4d4f;
          }
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
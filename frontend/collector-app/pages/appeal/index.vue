<template>
  <view class="appeal-page">
    <view class="appeal-tabs">
      <view class="tab-item" :class="{ active: currentTab === 'apply' }" @click="changeTab('apply')">
        申请申诉
      </view>
      <view class="tab-item" :class="{ active: currentTab === 'list' }" @click="changeTab('list')">
        我的申诉
      </view>
    </view>

    <!-- 申请申诉 -->
    <view v-if="currentTab === 'apply'" class="apply-form">
      <view class="form-section">
        <view class="section-title">选择申诉类型</view>
        <view class="type-options">
          <view
            v-for="item in appealTypes"
            :key="item.value"
            class="type-item"
            :class="{ selected: formData.appealType === item.value }"
            @click="formData.appealType = item.value"
          >
            <view class="type-label">{{ item.label }}</view>
          </view>
        </view>
      </view>

      <view class="form-section">
        <view class="section-title">关联案件</view>
        <view class="case-select" @click="selectCase">
          <view v-if="formData.caseId" class="case-info">
            <view class="case-no">{{ formData.caseNo }}</view>
            <view class="case-category">{{ formData.caseCategory }}</view>
          </view>
          <view v-else class="case-placeholder">请选择关联案件</view>
          <view class="case-arrow">
            <text class="iconfont">&#xe600;</text>
          </view>
        </view>
      </view>

      <view class="form-section">
        <view class="section-title">申诉理由</view>
        <textarea
          v-model="formData.reason"
          placeholder="请详细描述申诉理由..."
          maxlength="500"
        />
        <view class="word-count">{{ formData.reason.length }}/500</view>
      </view>

      <view class="form-section">
        <view class="section-title">证据材料</view>
        <view class="attachment-grid">
          <view v-for="(item, index) in formData.attachments" :key="index" class="attachment-item">
            <image :src="item" mode="aspectFill" />
            <view class="attachment-delete" @click="removeAttachment(index)">
              <text class="iconfont">&#xe608;</text>
            </view>
          </view>
          <view class="attachment-add" @click="chooseImage">
            <text class="iconfont">&#xe609;</text>
          </view>
        </view>
      </view>

      <button class="btn-submit" :disabled="!formData.caseId || !formData.reason" @click="submitAppeal">
        提交申诉
      </button>
    </view>

    <!-- 我的申诉 -->
    <view v-if="currentTab === 'list'" class="appeal-list">
      <view v-for="item in appealList" :key="item.id" class="appeal-item">
        <view class="appeal-header">
          <view class="appeal-type">{{ item.appealTypeName }}</view>
          <view class="appeal-status" :class="item.status">{{ item.statusName }}</view>
        </view>
        <view class="appeal-body">
          <view class="appeal-case">{{ item.caseNo }}</view>
          <view class="appeal-reason">{{ item.reason }}</view>
          <view class="appeal-time">提交时间: {{ item.createTime }}</view>
        </view>
        <view v-if="item.status === 'REJECTED'" class="appeal-footer">
          <view class="reject-reason">驳回原因: {{ item.rejectReason }}</view>
        </view>
      </view>

      <view v-if="appealList.length === 0" class="empty-state">
        <image src="/static/images/empty.png" mode="aspectFit" />
        <view class="empty-text">暂无申诉记录</view>
      </view>
    </view>
  </view>
</template>

<script>
import { getAppealList, submitAppeal, uploadFile } from '../../utils/api'

export default {
  data() {
    return {
      currentTab: 'apply',
      appealTypes: [
        { value: 'timeout', label: '超时申诉' },
        { value: 'invalid', label: '无效申诉' },
        { value: 'reject', label: '驳回申诉' }
      ],
      formData: {
        appealType: '',
        caseId: '',
        caseNo: '',
        caseCategory: '',
        reason: '',
        attachments: []
      },
      appealList: []
    }
  },
  onLoad() {
    this.loadAppealList()
  },
  methods: {
    changeTab(tab) {
      this.currentTab = tab
      if (tab === 'list') {
        this.loadAppealList()
      }
    },
    async loadAppealList() {
      try {
        const res = await getAppealList({ pageNum: 1, pageSize: 100 })
        this.appealList = res.data?.list || []
      } catch (error) {
        console.error('获取申诉列表失败:', error)
      }
    },
    selectCase() {
      // TODO: 打开案件选择页面
      uni.showToast({ title: '功能开发中', icon: 'none' })
    },
    async chooseImage() {
      try {
        const result = await uni.chooseImage({
          count: 5 - this.formData.attachments.length,
          sizeType: ['compressed'],
          sourceType: ['camera', 'album']
        })

        for (const path of result.tempFilePaths) {
          const uploadRes = await uploadFile(path)
          this.formData.attachments.push(uploadRes.data.url)
        }
      } catch (error) {
        console.error('选择图片失败:', error)
      }
    },
    removeAttachment(index) {
      this.formData.attachments.splice(index, 1)
    },
    async submitAppeal() {
      if (!this.formData.appealType) {
        uni.showToast({ title: '请选择申诉类型', icon: 'none' })
        return
      }
      if (!this.formData.caseId) {
        uni.showToast({ title: '请选择关联案件', icon: 'none' })
        return
      }
      if (!this.formData.reason) {
        uni.showToast({ title: '请填写申诉理由', icon: 'none' })
        return
      }

      uni.showLoading({ title: '提交中...', mask: true })

      try {
        await submitAppeal({
          appealType: this.formData.appealType,
          caseId: this.formData.caseId,
          reason: this.formData.reason,
          attachments: this.formData.attachments
        })

        uni.hideLoading()
        uni.showToast({ title: '提交成功', icon: 'success' })

        // 重置表单
        this.formData = {
          appealType: '',
          caseId: '',
          caseNo: '',
          caseCategory: '',
          reason: '',
          attachments: []
        }

        this.changeTab('list')
      } catch (error) {
        uni.hideLoading()
        console.error('提交申诉失败:', error)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.appeal-page {
  min-height: 100vh;
  background-color: #f8f8f8;

  .appeal-tabs {
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

      &.active {
        background-color: #409eff;
        color: #fff;
      }
    }
  }

  .apply-form {
    padding: 20rpx;

    .form-section {
      background-color: #fff;
      border-radius: 12rpx;
      padding: 30rpx;
      margin-bottom: 20rpx;

      .section-title {
        font-size: 30rpx;
        font-weight: bold;
        color: #303133;
        margin-bottom: 20rpx;
      }

      .type-options {
        display: flex;

        .type-item {
          flex: 1;
          text-align: center;
          padding: 20rpx;
          border-radius: 8rpx;
          background-color: #f5f7fa;
          margin-right: 15rpx;

          &:last-child {
            margin-right: 0;
          }

          .type-label {
            font-size: 26rpx;
            color: #606266;
          }

          &.selected {
            background-color: #ecf5ff;

            .type-label {
              color: #409eff;
            }
          }
        }
      }

      .case-select {
        display: flex;
        align-items: center;
        padding: 25rpx;
        background-color: #f5f7fa;
        border-radius: 8rpx;

        .case-info {
          flex: 1;

          .case-no {
            font-size: 28rpx;
            color: #303133;
          }

          .case-category {
            font-size: 24rpx;
            color: #909399;
            margin-top: 10rpx;
          }
        }

        .case-placeholder {
          flex: 1;
          font-size: 28rpx;
          color: #c0c4cc;
        }

        .case-arrow {
          color: #c0c4cc;
        }
      }

      textarea {
        width: 100%;
        min-height: 200rpx;
        padding: 20rpx;
        border: 1rpx solid #e4e7ed;
        border-radius: 8rpx;
        font-size: 28rpx;
      }

      .word-count {
        text-align: right;
        font-size: 24rpx;
        color: #909399;
        margin-top: 10rpx;
      }

      .attachment-grid {
        display: flex;
        flex-wrap: wrap;

        .attachment-item {
          width: 150rpx;
          height: 150rpx;
          margin-right: 15rpx;
          margin-bottom: 15rpx;
          border-radius: 8rpx;
          overflow: hidden;
          position: relative;

          image {
            width: 100%;
            height: 100%;
          }

          .attachment-delete {
            position: absolute;
            top: 5rpx;
            right: 5rpx;
            width: 30rpx;
            height: 30rpx;
            border-radius: 15rpx;
            background-color: rgba(0, 0, 0, 0.5);
            color: #fff;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20rpx;
          }
        }

        .attachment-add {
          width: 150rpx;
          height: 150rpx;
          border: 1rpx dashed #dcdfe6;
          border-radius: 8rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #909399;
          font-size: 40rpx;
        }
      }
    }

    .btn-submit {
      width: 100%;
      height: 80rpx;
      line-height: 80rpx;
      background-color: #409eff;
      color: #fff;
      border-radius: 8rpx;
      font-size: 32rpx;

      &[disabled] {
        background-color: #a0cfff;
      }
    }
  }

  .appeal-list {
    padding: 20rpx;

    .appeal-item {
      background-color: #fff;
      border-radius: 12rpx;
      padding: 30rpx;
      margin-bottom: 20rpx;

      .appeal-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 20rpx;

        .appeal-type {
          padding: 5rpx 15rpx;
          border-radius: 4rpx;
          font-size: 24rpx;
          background-color: #f5f7fa;
          color: #606266;
        }

        .appeal-status {
          padding: 5rpx 15rpx;
          border-radius: 4rpx;
          font-size: 24rpx;

          &.PENDING {
            background-color: #fff2f0;
            color: #ff4d4f;
          }

          &.APPROVED {
            background-color: #f0f9eb;
            color: #67c23a;
          }

          &.REJECTED {
            background-color: #f4f4f5;
            color: #909399;
          }
        }
      }

      .appeal-body {
        .appeal-case {
          font-size: 26rpx;
          color: #606266;
          margin-bottom: 15rpx;
        }

        .appeal-reason {
          font-size: 28rpx;
          color: #303133;
          margin-bottom: 15rpx;
        }

        .appeal-time {
          font-size: 24rpx;
          color: #909399;
        }
      }

      .appeal-footer {
        padding-top: 20rpx;
        border-top: 1rpx solid #eee;

        .reject-reason {
          font-size: 24rpx;
          color: #ff4d4f;
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
<template>
  <view class="task-detail">
    <!-- 案件信息 -->
    <view class="case-info-card">
      <view class="case-header">
        <view class="case-no">{{ taskInfo.caseNo }}</view>
        <view class="case-status">{{ taskInfo.caseStatusName }}</view>
      </view>

      <view class="case-body">
        <view class="info-row">
          <view class="info-label">大类</view>
          <view class="info-value">{{ taskInfo.categoryBigName }}</view>
        </view>
        <view class="info-row">
          <view class="info-label">小类</view>
          <view class="info-value">{{ taskInfo.categorySmallName }}</view>
        </view>
        <view class="info-row">
          <view class="info-label">地址</view>
          <view class="info-value">{{ taskInfo.address }}</view>
        </view>
        <view class="info-row">
          <view class="info-label">问题描述</view>
          <view class="info-value">{{ taskInfo.description }}</view>
        </view>
        <view class="info-row">
          <view class="info-label">上报时间</view>
          <view class="info-value">{{ taskInfo.reportTime }}</view>
        </view>
      </view>

      <!-- 附件 -->
      <view class="attachment-section">
        <view class="section-title">上报附件</view>
        <view class="attachment-grid">
          <view v-for="item in taskInfo.attachments" :key="item.id" class="attachment-item">
            <image :src="item.url" mode="aspectFill" @click="previewImage(item.url)" />
          </view>
        </view>
      </view>
    </view>

    <!-- 地图定位 -->
    <view class="map-card">
      <view class="section-title">地图定位</view>
      <map
        :longitude="taskInfo.longitude"
        :latitude="taskInfo.latitude"
        :markers="markers"
        style="width: 100%; height: 400rpx;"
      />
    </view>

    <!-- 执行表单 -->
    <view v-if="taskInfo.status === 'PENDING'" class="execute-form">
      <view class="section-title">执行结果</view>

      <view class="result-options">
        <view
          v-for="option in resultOptions"
          :key="option.value"
          class="result-item"
          :class="{ selected: formData.result === option.value }"
          @click="formData.result = option.value"
        >
          <view class="result-icon">
            <text class="iconfont">{{ option.icon }}</text>
          </view>
          <view class="result-label">{{ option.label }}</view>
        </view>
      </view>

      <view class="form-item">
        <view class="form-label">备注说明</view>
        <textarea
          v-model="formData.remark"
          placeholder="请输入备注..."
          maxlength="200"
        />
      </view>

      <view class="form-item">
        <view class="form-label">上传照片</view>
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

      <button class="btn-submit" :disabled="!formData.result" @click="submitExecute">
        提交执行结果
      </button>
    </view>
  </view>
</template>

<script>
import { getTaskDetail, executeVerifyTask, executeCheckTask, uploadFile } from '../../utils/api'

export default {
  data() {
    return {
      taskId: '',
      taskType: 'verify',
      taskInfo: {},
      markers: [],
      formData: {
        result: '',
        remark: '',
        attachments: []
      },
      resultOptions: []
    }
  },
  onLoad(options) {
    this.taskId = options.id
    this.taskType = options.type || 'verify'
    this.setResultOptions()
    this.loadTaskDetail()
  },
  methods: {
    setResultOptions() {
      if (this.taskType === 'verify') {
        this.resultOptions = [
          { value: 'confirmed', label: '确认存在', icon: '' },
          { value: 'not_found', label: '未发现问题', icon: '' }
        ]
      } else {
        this.resultOptions = [
          { value: 'passed', label: '处置到位', icon: '' },
          { value: 'not_passed', label: '处置不到位', icon: '' }
        ]
      }
    },
    async loadTaskDetail() {
      try {
        const res = await getTaskDetail(this.taskId, this.taskType)
        this.taskInfo = res.data || {}

        this.markers = [{
          id: 1,
          longitude: this.taskInfo.longitude,
          latitude: this.taskInfo.latitude,
          iconPath: '/static/images/marker.png',
          width: 30,
          height: 30
        }]
      } catch (error) {
        console.error('获取任务详情失败:', error)
      }
    },
    previewImage(url) {
      const urls = this.taskInfo.attachments.map(a => a.url)
      uni.previewImage({ urls, current: url })
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
    async submitExecute() {
      if (!this.formData.result) {
        uni.showToast({ title: '请选择执行结果', icon: 'none' })
        return
      }

      uni.showLoading({ title: '提交中...', mask: true })

      try {
        const data = {
          taskId: this.taskId,
          result: this.formData.result,
          remark: this.formData.remark,
          attachments: this.formData.attachments
        }

        const res = this.taskType === 'verify'
          ? await executeVerifyTask(data)
          : await executeCheckTask(data)

        uni.hideLoading()
        uni.showToast({ title: '提交成功', icon: 'success' })

        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (error) {
        uni.hideLoading()
        console.error('提交失败:', error)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.task-detail {
  min-height: 100vh;
  background-color: #f8f8f8;
  padding: 20rpx;

  .case-info-card, .map-card, .execute-form {
    background-color: #fff;
    border-radius: 12rpx;
    padding: 30rpx;
    margin-bottom: 20rpx;
  }

  .section-title {
    font-size: 30rpx;
    font-weight: bold;
    color: #303133;
    margin-bottom: 20rpx;
  }

  .case-info-card {
    .case-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 20rpx;
      border-bottom: 1rpx solid #eee;

      .case-no {
        font-size: 32rpx;
        font-weight: bold;
        color: #303133;
      }

      .case-status {
        padding: 5rpx 15rpx;
        border-radius: 4rpx;
        font-size: 24rpx;
        background-color: #409eff;
        color: #fff;
      }
    }

    .case-body {
      padding-top: 20rpx;

      .info-row {
        display: flex;
        padding: 15rpx 0;

        .info-label {
          width: 150rpx;
          font-size: 26rpx;
          color: #909399;
        }

        .info-value {
          flex: 1;
          font-size: 26rpx;
          color: #303133;
        }
      }
    }
  }

  .attachment-section {
    margin-top: 20rpx;
    padding-top: 20rpx;
    border-top: 1rpx solid #eee;

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

        image {
          width: 100%;
          height: 100%;
        }
      }
    }
  }

  .execute-form {
    .result-options {
      display: flex;
      margin-bottom: 30rpx;

      .result-item {
        flex: 1;
        text-align: center;
        padding: 30rpx;
        border-radius: 12rpx;
        background-color: #f5f7fa;
        margin-right: 20rpx;

        &:last-child {
          margin-right: 0;
        }

        .result-icon {
          font-size: 50rpx;
          color: #909399;
        }

        .result-label {
          font-size: 26rpx;
          color: #606266;
          margin-top: 15rpx;
        }

        &.selected {
          background-color: #ecf5ff;

          .result-icon, .result-label {
            color: #409eff;
          }
        }
      }
    }

    .form-item {
      margin-bottom: 30rpx;

      .form-label {
        font-size: 28rpx;
        color: #303133;
        margin-bottom: 15rpx;
      }

      textarea {
        width: 100%;
        min-height: 150rpx;
        padding: 20rpx;
        border: 1rpx solid #dcdfe6;
        border-radius: 8rpx;
        font-size: 26rpx;
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
}
</style>
<template>
  <view class="report-page">
    <!-- 步骤指示器 -->
    <view class="steps">
      <view class="step-item" :class="{ active: currentStep >= 1 }">
        <view class="step-num">1</view>
        <view class="step-label">分类</view>
      </view>
      <view class="step-item" :class="{ active: currentStep >= 2 }">
        <view class="step-num">2</view>
        <view class="step-label">定位</view>
      </view>
      <view class="step-item" :class="{ active: currentStep >= 3 }">
        <view class="step-num">3</view>
        <view class="step-label">描述</view>
      </view>
      <view class="step-item" :class="{ active: currentStep >= 4 }">
        <view class="step-num">4</view>
        <view class="step-label">附件</view>
      </view>
    </view>

    <!-- 步骤1: 选择分类 -->
    <view v-if="currentStep === 1" class="step-content">
      <view class="category-section">
        <view class="section-title">选择大类</view>
        <view class="category-grid">
          <view
            v-for="item in categoryBigList"
            :key="item.id"
            class="category-item"
            :class="{ selected: formData.categoryBigId === item.id }"
            @click="selectCategoryBig(item)"
          >
            <view class="category-icon">{{ item.icon }}</view>
            <view class="category-name">{{ item.name }}</view>
          </view>
        </view>
      </view>

      <view v-if="categorySmallList.length > 0" class="category-section">
        <view class="section-title">选择小类</view>
        <view class="category-list">
          <view
            v-for="item in categorySmallList"
            :key="item.id"
            class="category-small-item"
            :class="{ selected: formData.categorySmallId === item.id }"
            @click="selectCategorySmall(item)"
          >
            <view class="category-name">{{ item.name }}</view>
            <view class="category-arrow">
              <text class="iconfont">&#xe600;</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="conditionList.length > 0" class="category-section">
        <view class="section-title">选择立案条件</view>
        <view class="condition-list">
          <view
            v-for="item in conditionList"
            :key="item.id"
            class="condition-item"
            :class="{ selected: formData.conditionId === item.id }"
            @click="selectCondition(item)"
          >
            <view class="condition-content">{{ item.content }}</view>
            <view class="condition-check" v-if="formData.conditionId === item.id">
              <text class="iconfont">&#xe601;</text>
            </view>
          </view>
        </view>
      </view>

      <view class="btn-group">
        <button class="btn-next" :disabled="!formData.conditionId" @click="nextStep">下一步</button>
      </view>
    </view>

    <!-- 步骤2: 地图定位 -->
    <view v-if="currentStep === 2" class="step-content">
      <view class="map-container">
        <map
          id="report-map"
          :longitude="formData.longitude"
          :latitude="formData.latitude"
          :markers="markers"
          :show-location="true"
          style="width: 100%; height: 500rpx;"
          @markertap="onMarkerTap"
          @regionchange="onRegionChange"
        />
      </view>

      <view class="location-info">
        <view class="location-label">
          <text class="iconfont">&#xe607;</text>
          当前位置
        </view>
        <view class="location-address">{{ formData.address }}</view>
        <view class="location-coords">{{ formData.longitude }} , {{ formData.latitude }}</view>
      </view>

      <view class="btn-group">
        <button class="btn-prev" @click="prevStep">上一步</button>
        <button class="btn-next" @click="nextStep">下一步</button>
      </view>
    </view>

    <!-- 步骤3: 问题描述 -->
    <view v-if="currentStep === 3" class="step-content">
      <view class="form-section">
        <view class="form-item">
          <view class="form-label">问题描述</view>
          <textarea
            class="form-input"
            v-model="formData.description"
            placeholder="请详细描述问题情况..."
            maxlength="500"
          />
          <view class="form-count">{{ formData.description.length }}/500</view>
        </view>
      </view>

      <view class="btn-group">
        <button class="btn-prev" @click="prevStep">上一步</button>
        <button class="btn-next" :disabled="!formData.description" @click="nextStep">下一步</button>
      </view>
    </view>

    <!-- 步骤4: 添加附件 -->
    <view v-if="currentStep === 4" class="step-content">
      <view class="attachment-section">
        <view class="section-title">添加照片/视频</view>

        <view class="attachment-grid">
          <view v-for="(item, index) in formData.attachments" :key="index" class="attachment-item">
            <image v-if="item.type === 'image'" :src="item.url" mode="aspectFill" />
            <video v-else :src="item.url" />
            <view class="attachment-delete" @click="removeAttachment(index)">
              <text class="iconfont">&#xe608;</text>
            </view>
          </view>

          <view class="attachment-add" @click="chooseMedia">
            <view class="add-icon">
              <text class="iconfont">&#xe609;</text>
            </view>
            <view class="add-label">添加</view>
          </view>
        </view>

        <view class="attachment-tip">请上传至少1张现场照片，最多可上传9张照片或1个视频</view>
      </view>

      <view class="btn-group">
        <button class="btn-prev" @click="prevStep">上一步</button>
        <button class="btn-submit" :disabled="formData.attachments.length === 0" @click="submitReport">提交上报</button>
      </view>
    </view>
  </view>
</template>

<script>
import { getCategoryBigList, getCategorySmallList, getConditions, uploadFile, reportCase } from '../../utils/api'

export default {
  data() {
    return {
      currentStep: 1,
      categoryBigList: [],
      categorySmallList: [],
      conditionList: [],
      formData: {
        categoryBigId: '',
        categoryBigName: '',
        categorySmallId: '',
        categorySmallName: '',
        conditionId: '',
        conditionName: '',
        longitude: 0,
        latitude: 0,
        address: '',
        description: '',
        attachments: []
      },
      markers: []
    }
  },
  onLoad() {
    this.loadCategoryBigList()
    this.initLocation()
  },
  methods: {
    async loadCategoryBigList() {
      try {
        const res = await getCategoryBigList()
        this.categoryBigList = res.data || []
      } catch (error) {
        console.error('获取大类失败:', error)
      }
    },
    async selectCategoryBig(item) {
      this.formData.categoryBigId = item.id
      this.formData.categoryBigName = item.name
      this.formData.categorySmallId = ''
      this.formData.conditionId = ''
      this.categorySmallList = []
      this.conditionList = []

      try {
        const res = await getCategorySmallList(item.id)
        this.categorySmallList = res.data || []
      } catch (error) {
        console.error('获取小类失败:', error)
      }
    },
    async selectCategorySmall(item) {
      this.formData.categorySmallId = item.id
      this.formData.categorySmallName = item.name
      this.formData.conditionId = ''
      this.conditionList = []

      try {
        const res = await getConditions(item.id)
        this.conditionList = res.data || []
      } catch (error) {
        console.error('获取立案条件失败:', error)
      }
    },
    selectCondition(item) {
      this.formData.conditionId = item.id
      this.formData.conditionName = item.content
    },
    async initLocation() {
      try {
        const location = await uni.getLocation({ type: 'gcj02' })
        this.formData.longitude = location.longitude
        this.formData.latitude = location.latitude
        this.updateMarker()
        this.getAddress(location.longitude, location.latitude)
      } catch (error) {
        console.error('获取定位失败:', error)
        uni.showToast({ title: '获取定位失败', icon: 'none' })
      }
    },
    updateMarker() {
      this.markers = [{
        id: 1,
        longitude: this.formData.longitude,
        latitude: this.formData.latitude,
        iconPath: '/static/images/marker.png',
        width: 30,
        height: 30
      }]
    },
    async getAddress(lng, lat) {
      // TODO: 调用逆地理编码获取地址
      this.formData.address = '运城市盐湖区解放路1号'
    },
    onMarkerTap() {
      // 标记点点击
    },
    onRegionChange(e) {
      if (e.type === 'end') {
        const center = e.detail.centerLocation
        if (center) {
          this.formData.longitude = center.longitude
          this.formData.latitude = center.latitude
          this.updateMarker()
          this.getAddress(center.longitude, center.latitude)
        }
      }
    },
    nextStep() {
      if (this.currentStep < 4) {
        this.currentStep++
      }
    },
    prevStep() {
      if (this.currentStep > 1) {
        this.currentStep--
      }
    },
    async chooseMedia() {
      try {
        const result = await uni.chooseMedia({
          count: 9 - this.formData.attachments.length,
          mediaType: ['image', 'video'],
          sourceType: ['camera', 'album'],
          maxDuration: 60
        })

        for (const file of result.tempFiles) {
          // 上传文件
          const uploadRes = await uploadFile(file.tempFilePath)
          this.formData.attachments.push({
            type: file.fileType,
            url: uploadRes.data.url,
            localPath: file.tempFilePath
          })
        }
      } catch (error) {
        console.error('选择附件失败:', error)
      }
    },
    removeAttachment(index) {
      this.formData.attachments.splice(index, 1)
    },
    async submitReport() {
      if (this.formData.attachments.length === 0) {
        uni.showToast({ title: '请上传至少1张照片', icon: 'none' })
        return
      }

      uni.showLoading({ title: '正在提交...', mask: true })

      try {
        const data = {
          categoryBigId: this.formData.categoryBigId,
          categorySmallId: this.formData.categorySmallId,
          conditionId: this.formData.conditionId,
          longitude: this.formData.longitude,
          latitude: this.formData.latitude,
          address: this.formData.address,
          description: this.formData.description,
          attachments: this.formData.attachments.map(a => a.url)
        }

        await reportCase(data)

        uni.hideLoading()
        uni.showToast({ title: '上报成功', icon: 'success' })

        setTimeout(() => {
          uni.reLaunch({ url: '/pages/index/index' })
        }, 1500)
      } catch (error) {
        uni.hideLoading()
        console.error('上报失败:', error)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.report-page {
  min-height: 100vh;
  background-color: #f8f8f8;
  padding-bottom: 150rpx;

  .steps {
    display: flex;
    background-color: #fff;
    padding: 30rpx;

    .step-item {
      flex: 1;
      text-align: center;

      .step-num {
        width: 50rpx;
        height: 50rpx;
        border-radius: 25rpx;
        background-color: #e4e7ed;
        color: #c0c4cc;
        margin: 0 auto;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28rpx;
      }

      .step-label {
        font-size: 24rpx;
        color: #c0c4cc;
        margin-top: 15rpx;
      }

      &.active {
        .step-num {
          background-color: #409eff;
          color: #fff;
        }

        .step-label {
          color: #409eff;
        }
      }
    }
  }

  .step-content {
    padding: 30rpx;

    .category-section {
      background-color: #fff;
      border-radius: 12rpx;
      padding: 30rpx;
      margin-bottom: 20rpx;

      .section-title {
        font-size: 30rpx;
        font-weight: bold;
        color: #333;
        margin-bottom: 20rpx;
      }

      .category-grid {
        display: flex;
        flex-wrap: wrap;

        .category-item {
          width: 25%;
          text-align: center;
          padding: 20rpx;

          .category-icon {
            width: 60rpx;
            height: 60rpx;
            border-radius: 30rpx;
            background-color: #f5f7fa;
            margin: 0 auto;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 30rpx;
          }

          .category-name {
            font-size: 24rpx;
            color: #666;
            margin-top: 10rpx;
          }

          &.selected {
            .category-icon {
              background-color: #409eff;
              color: #fff;
            }

            .category-name {
              color: #409eff;
            }
          }
        }
      }

      .category-list {
        .category-small-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 25rpx;
          border-bottom: 1rpx solid #eee;

          .category-name {
            font-size: 28rpx;
            color: #333;
          }

          .category-arrow {
            color: #c0c4cc;
          }

          &.selected {
            background-color: #ecf5ff;

            .category-name {
              color: #409eff;
            }
          }
        }
      }

      .condition-list {
        .condition-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 25rpx;
          border-bottom: 1rpx solid #eee;

          .condition-content {
            font-size: 26rpx;
            color: #666;
            flex: 1;
          }

          .condition-check {
            color: #409eff;
            font-size: 30rpx;
          }

          &.selected {
            background-color: #ecf5ff;

            .condition-content {
              color: #409eff;
            }
          }
        }
      }
    }

    .map-container {
      background-color: #fff;
      border-radius: 12rpx;
      overflow: hidden;
      margin-bottom: 20rpx;
    }

    .location-info {
      background-color: #fff;
      border-radius: 12rpx;
      padding: 30rpx;

      .location-label {
        display: flex;
        align-items: center;
        color: #409eff;
        font-size: 28rpx;
        margin-bottom: 15rpx;
      }

      .location-address {
        font-size: 28rpx;
        color: #333;
        margin-bottom: 10rpx;
      }

      .location-coords {
        font-size: 24rpx;
        color: #999;
      }
    }

    .form-section {
      background-color: #fff;
      border-radius: 12rpx;
      padding: 30rpx;

      .form-item {
        .form-label {
          font-size: 30rpx;
          font-weight: bold;
          color: #333;
          margin-bottom: 20rpx;
        }

        .form-input {
          width: 100%;
          min-height: 200rpx;
          padding: 20rpx;
          border: 1rpx solid #e4e7ed;
          border-radius: 8rpx;
          font-size: 28rpx;
        }

        .form-count {
          text-align: right;
          font-size: 24rpx;
          color: #999;
          margin-top: 10rpx;
        }
      }
    }

    .attachment-section {
      background-color: #fff;
      border-radius: 12rpx;
      padding: 30rpx;

      .section-title {
        font-size: 30rpx;
        font-weight: bold;
        color: #333;
        margin-bottom: 20rpx;
      }

      .attachment-grid {
        display: flex;
        flex-wrap: wrap;

        .attachment-item {
          width: 200rpx;
          height: 200rpx;
          margin-right: 20rpx;
          margin-bottom: 20rpx;
          border-radius: 8rpx;
          overflow: hidden;
          position: relative;

          image, video {
            width: 100%;
            height: 100%;
          }

          .attachment-delete {
            position: absolute;
            top: 10rpx;
            right: 10rpx;
            width: 40rpx;
            height: 40rpx;
            border-radius: 20rpx;
            background-color: rgba(0, 0, 0, 0.5);
            color: #fff;
            display: flex;
            align-items: center;
            justify-content: center;
          }
        }

        .attachment-add {
          width: 200rpx;
          height: 200rpx;
          border: 1rpx dashed #dcdfe6;
          border-radius: 8rpx;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;

          .add-icon {
            font-size: 50rpx;
            color: #c0c4cc;
          }

          .add-label {
            font-size: 24rpx;
            color: #c0c4cc;
            margin-top: 10rpx;
          }
        }
      }

      .attachment-tip {
        font-size: 24rpx;
        color: #999;
        margin-top: 20rpx;
      }
    }

    .btn-group {
      display: flex;
      position: fixed;
      bottom: 0;
      left: 0;
      right: 0;
      padding: 30rpx;
      background-color: #fff;
      box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);

      button {
        flex: 1;
        margin-right: 20rpx;
        border-radius: 8rpx;
        font-size: 32rpx;
        height: 80rpx;
        line-height: 80rpx;

        &.btn-prev {
          background-color: #f5f7fa;
          color: #606266;
        }

        &.btn-next, &.btn-submit {
          background-color: #409eff;
          color: #fff;

          &[disabled] {
            background-color: #a0cfff;
          }
        }
      }
    }
  }
}
</style>
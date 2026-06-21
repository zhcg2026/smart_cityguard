<template>
  <div class="about-page">
    <van-nav-bar title="关于我们" left-arrow @click-left="goBack" />

    <div class="about-content">
      <div class="app-icon">
        <van-icon name="shield-o" size="60" color="#1989fa" />
      </div>
      <div class="app-name">智慧城管移动端</div>
      <div class="app-version">v1.0.0</div>

      <van-cell-group inset class="info-group">
        <van-cell title="系统名称" value="智慧城管系统" />
        <van-cell title="当前版本" value="v1.0.0" />
        <van-cell title="技术支持" value="智慧城管平台" />
      </van-cell-group>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goBack() {
  router.back()
}
</script>

<style scoped>
.about-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.about-content {
  padding: 40px 16px;
  text-align: center;
}

.app-icon {
  margin-bottom: 12px;
}

.app-name {
  font-size: 18px;
  font-weight: bold;
  color: #323233;
  margin-bottom: 4px;
}

.app-version {
  font-size: 14px;
  color: #969799;
  margin-bottom: 24px;
}

.info-group {
  text-align: left;
}
</style>

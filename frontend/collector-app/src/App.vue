<template>
  <router-view />
</template>

<script setup>
import { watch, provide } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/utils/auth'
import { useMessagePoll } from '@/composables/useMessagePoll'

const userStore = useUserStore()
userStore.initUser()

const route = useRoute()
const { unreadCount, startPoll, stopPoll } = useMessagePoll()
provide('unreadCount', unreadCount)

watch(
  () => [route.path, getToken()],
  () => {
    if (getToken() && route.path !== '/login') {
      startPoll()
    } else {
      stopPoll()
    }
  },
  { immediate: true }
)
</script>

<style>
body {
  margin: 0;
  padding: 0;
  background-color: #f7f8fa;
}
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 强制浅色模式，防止手机深色模式影响 */
@media (prefers-color-scheme: dark) {
  :root {
    color-scheme: light !important;
  }
  html, body, #app {
    background-color: #f7f8fa !important;
    color: #323233 !important;
  }
}

/* Vant 组件强制浅色背景 */
.van-cell-group,
.van-field,
.van-cell,
.van-button--default,
.van-picker {
  background-color: #fff !important;
  color: #323233 !important;
}

.van-tabbar {
  background-color: #fff !important;
}

/* Vant Toast 强制深色背景白字 */
.van-toast {
  background: rgba(50, 50, 50, 0.88) !important;
  color: #fff !important;
}
.van-toast--text {
  background: rgba(50, 50, 50, 0.88) !important;
  color: #fff !important;
}
.van-toast__text {
  color: #fff !important;
}

/* 上传失败 Toast：避免默认白底看不清 */
.upload-fail-toast,
.upload-success-toast {
  max-width: 92vw !important;
  padding: 12px 16px !important;
  line-height: 1.45 !important;
  word-break: break-word !important;
}
.upload-fail-toast {
  background: rgba(50, 50, 50, 0.94) !important;
  color: #fff !important;
}
.upload-success-toast {
  background: rgba(7, 193, 96, 0.94) !important;
  color: #fff !important;
}

/* 普通提示 Toast：避免白底白字 */
.collector-app-toast {
  max-width: 92vw !important;
  padding: 12px 16px !important;
  line-height: 1.45 !important;
  word-break: break-word !important;
  background: rgba(50, 50, 50, 0.94) !important;
  color: #fff !important;
}

/* 顶部新消息/任务 Notify：避免白底白字 */
.collector-push-notify {
  max-width: 100% !important;
  padding: 12px 16px !important;
  line-height: 1.45 !important;
  word-break: break-word !important;
  white-space: pre-wrap !important;
  background: #1989fa !important;
  color: #fff !important;
}
</style>

<style>
.upload-error-dialog .van-dialog__message {
  text-align: left;
  white-space: pre-wrap;
  line-height: 1.5;
  max-height: 60vh;
  overflow-y: auto;
}
</style>
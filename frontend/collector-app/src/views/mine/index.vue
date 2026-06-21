<template>
  <div class="mine-page">
    <!-- 用户信息 -->
    <div class="user-card">
      <div class="avatar-wrap" @click="showAvatarSheet = true">
        <van-image round width="60" height="60" :src="userInfo.avatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'" />
        <div class="avatar-edit">
          <van-icon name="photograph" size="14" />
        </div>
      </div>
      <div class="info">
        <div class="name">{{ displayName }}</div>
        <div class="grid">{{ gridDisplayName }}</div>
      </div>
      <van-badge :content="unreadCount || undefined" :max="99">
        <van-icon name="bell" size="24" @click="goMessage" />
      </van-badge>
    </div>

    <van-action-sheet
      v-model:show="showAvatarSheet"
      :actions="avatarActions"
      cancel-text="取消"
      @select="onAvatarAction"
    />

    <!-- 统计数据（采集员） -->
    <van-grid v-if="showCollectorMenu" :column-num="3" class="stat-grid">
      <van-grid-item :text="stats.reportCount">
        <template #icon>
          <van-icon name="edit" color="#1989fa" />
        </template>
        <template #text>
          <div class="stat-num">{{ stats.reportCount }}</div>
          <div class="stat-label">上报数</div>
        </template>
      </van-grid-item>
      <van-grid-item>
        <template #icon>
          <van-icon name="todo-list-o" color="#ff976a" />
        </template>
        <template #text>
          <div class="stat-num">{{ stats.verifyCount }}</div>
          <div class="stat-label">核查数</div>
        </template>
      </van-grid-item>
      <van-grid-item>
        <template #icon>
          <van-icon name="checked" color="#07c160" />
        </template>
        <template #text>
          <div class="stat-num">{{ stats.checkCount }}</div>
          <div class="stat-label">核实数</div>
        </template>
      </van-grid-item>
    </van-grid>

    <van-cell-group v-if="showHandlerStats" inset class="handler-tip">
      <van-cell title="待处置案件" is-link to="/handle" icon="todo-list-o" />
    </van-cell-group>

    <!-- 功能列表 -->
    <van-cell-group inset>
      <van-cell v-if="showCollectorMenu" title="我的上报" is-link to="/mine/report" icon="records" />
      <van-cell title="修改密码" is-link to="/mine/password" icon="lock" />
      <van-cell title="关于我们" is-link to="/mine/about" icon="info-o" />
    </van-cell-group>

    <!-- 退出登录 -->
    <div class="logout-btn">
      <van-button round block type="danger" @click="handleLogout">
        退出登录
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, inject, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog, showToast, showLoadingToast, closeToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { isCollectorMobileUser, isHandlerMobileUser, primaryRoleLabel } from '@/utils/roleAccess'
import { getMyCaseList } from '@/api/case'
import { getCheckTaskList, getVerifyTaskList } from '@/api/task'
import { updateAvatar, uploadFile } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()
const unreadCount = inject('unreadCount', ref(0))

const userInfo = computed(() => userStore.userInfo)
const roles = computed(() => userStore.roles?.length ? userStore.roles : userInfo.value?.roles || [])
const displayName = computed(() => userInfo.value.realName || primaryRoleLabel(roles.value) || '用户')
const gridDisplayName = computed(() => {
  if (isHandlerMobileUser(roles.value) && !isCollectorMobileUser(roles.value)) {
    return userInfo.value?.departmentName?.trim() || '处置部门'
  }
  const name = userInfo.value?.gridName?.trim()
  return name || '未绑定片区'
})
const showCollectorMenu = computed(() => isCollectorMobileUser(roles.value))
const showHandlerStats = computed(() => isHandlerMobileUser(roles.value))

const stats = ref({
  reportCount: 0,
  verifyCount: 0,
  checkCount: 0
})

const showAvatarSheet = ref(false)
const avatarActions = [
  { name: '拍照', value: 'camera' },
  { name: '从相册选择', value: 'album' }
]
const fileInput = ref(null)

function goMessage() {
  router.push('/message')
}

function onAvatarAction(action) {
  showAvatarSheet.value = false
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  if (action.value === 'camera') {
    input.capture = 'environment'
  }
  input.onchange = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    try {
      showLoadingToast({ message: '上传中...', forbidClick: true, duration: 0 })
      const res = await uploadFile(file)
      const url = typeof res.data === 'string' ? res.data : res.data?.url
      if (!url) throw new Error('上传失败')
      await updateAvatar({ avatar: url })
      userStore.userInfo.avatar = url
      showToast('头像已更新')
    } catch {
      showToast('头像上传失败')
    } finally {
      closeToast()
    }
  }
  input.click()
}

async function loadStats() {
  try {
    const [caseRes, checkRes, verifyRes] = await Promise.all([
      getMyCaseList({ pageNum: 1, pageSize: 1 }),
      getCheckTaskList({ pageNum: 1, pageSize: 1 }),
      getVerifyTaskList({ pageNum: 1, pageSize: 1 })
    ])
    stats.value.reportCount = caseRes.data?.total || 0
    stats.value.checkCount = checkRes.data?.total || 0
    stats.value.verifyCount = verifyRes.data?.total || 0
  } catch {
    /* ignore */
  }
}

onMounted(loadStats)

async function handleLogout() {
  try {
    await showDialog({
      title: '提示',
      message: '确定要退出登录吗？',
      showCancelButton: true
    })
    await userStore.logout()
    showToast('已退出登录')
    router.push('/login')
  } catch (error) {
    // 用户取消
  }
}
</script>

<style scoped>
.mine-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.user-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #1989fa, #36cfc9);
  color: #fff;

  .avatar-wrap {
    position: relative;

    .avatar-edit {
      position: absolute;
      bottom: 0;
      right: 0;
      width: 22px;
      height: 22px;
      background: rgba(0, 0, 0, 0.5);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }
  }

  .info {
    flex: 1;
    margin-left: 16px;

    .name {
      font-size: 18px;
      font-weight: bold;
    }

    .grid {
      font-size: 12px;
      margin-top: 4px;
    }
  }

  .van-icon {
    color: #fff;
  }
}

.handler-tip {
  margin: 12px 0;
}

.stat-grid {
  margin: 20px 0;

  .stat-num {
    font-size: 20px;
    font-weight: bold;
  }

  .stat-label {
    font-size: 12px;
    color: #999;
  }
}

.logout-btn {
  padding: 20px 16px;
}
</style>
<template>
  <div class="home-page">
    <!-- 头部 -->
    <div class="header">
      <div class="user-info">
        <van-image round width="40" height="40" :src="userInfo.avatar || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'" />
        <div class="info">
          <div class="name">{{ userInfo.realName || '采集员' }}</div>
          <div class="grid">{{ userInfo.gridName || '未绑定网格' }}</div>
        </div>
      </div>
      <van-icon name="bell" size="24" @click="goMessage" />
    </div>

    <!-- 快捷功能 -->
    <van-grid :column-num="3" class="quick-grid">
      <van-grid-item icon="edit" text="问题上报" to="/report" />
      <van-grid-item icon="todo-list-o" text="核查任务" to="/task" />
      <van-grid-item icon="checked" text="核实任务" to="/task" />
    </van-grid>

    <!-- 今日提示 -->
    <van-cell-group title="今日提示" inset>
      <van-cell
        v-for="tip in dailyTips"
        :key="tip.id"
        :title="tip.title || tip.content"
        :label="tip.title && tip.content ? tip.content : ''"
        icon="info-o"
        is-link
        @click="openContentDetail(tip, 'dailytip')"
      />
      <van-empty v-if="!contentLoading && dailyTips.length === 0" description="暂无提示" image-size="60" />
    </van-cell-group>

    <!-- 公文通告 -->
    <van-cell-group title="公文通告" inset>
      <van-cell
        v-for="item in announcements"
        :key="item.id"
        :title="item.title"
        :value="formatPublishTime(item.publishTime)"
        is-link
        @click="openContentDetail(item, 'announcement')"
      />
      <van-empty v-if="!contentLoading && announcements.length === 0" description="暂无通告" image-size="60" />
    </van-cell-group>

    <van-popup v-model:show="detailVisible" round position="bottom" :style="{ maxHeight: '70%' }">
      <div class="content-detail">
        <div class="content-detail__title">{{ detailItem.title || '详情' }}</div>
        <div v-if="detailItem.publishTime" class="content-detail__meta">
          {{ formatPublishTime(detailItem.publishTime) }}
          <span v-if="detailItem.publisherName"> · {{ detailItem.publisherName }}</span>
        </div>
        <div class="content-detail__body">{{ detailItem.content || '—' }}</div>
        <van-button block type="primary" @click="detailVisible = false">关闭</van-button>
      </div>
    </van-popup>

    <!-- 待办任务 -->
    <van-cell-group title="待办任务" inset>
      <van-cell v-for="task in pendingTasks" :key="task.id" :title="task.title" :label="task.deadline" is-link>
        <template #icon>
          <van-tag :type="task.type === 'verify' ? 'warning' : 'success'">
            {{ task.type === 'verify' ? '核查' : '核实' }}
          </van-tag>
        </template>
      </van-cell>
      <van-empty v-if="pendingTasks.length === 0" description="暂无待办" image-size="60" />
    </van-cell-group>

    <!-- 我的最近上报 -->
    <van-cell-group title="我的最近上报" inset>
      <van-cell
        v-for="c in myRecentCases"
        :key="c.id"
        :title="c.caseCode || '案件'"
        :label="c.address"
        :value="statusLabel(c.caseStatus)"
      />
      <van-empty v-if="myRecentCases.length === 0" description="暂无上报记录" image-size="60" />
    </van-cell-group>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/utils/auth'
import { getMyCaseList } from '@/api/case'
import {
  getDailyTipList,
  getDailyTipDetail,
  getAnnouncementList,
  getAnnouncementDetail
} from '@/api/message'

const router = useRouter()
const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo)
const dailyTips = ref([])
const announcements = ref([])
const pendingTasks = ref([])
const myRecentCases = ref([])
const contentLoading = ref(false)
const detailVisible = ref(false)
const detailItem = ref({})

const statusLabel = (s) =>
  ({
    pending_verify: '待核查',
    pending_register: '待立案',
    pending_dispatch: '待派遣',
    pending_handle: '待处置',
    handle_finish: '待部门确认',
    pending_check: '待核实',
    closed: '已结案',
    not_accepted: '不受理'
  }[s] || s)

function formatPublishTime(value) {
  if (!value) return ''
  const text = String(value)
  return text.length >= 16 ? text.slice(0, 16).replace('T', ' ') : text
}

async function loadHomeContent() {
  contentLoading.value = true
  try {
    const [tipRes, annRes] = await Promise.all([
      getDailyTipList({ limit: 5 }),
      getAnnouncementList({ limit: 5 })
    ])
    dailyTips.value = tipRes.data || []
    announcements.value = annRes.data || []
  } catch {
    dailyTips.value = []
    announcements.value = []
  } finally {
    contentLoading.value = false
  }
}

async function openContentDetail(row, type) {
  if (!row?.id) return
  detailItem.value = { ...row }
  detailVisible.value = true
  try {
    const res =
      type === 'dailytip'
        ? await getDailyTipDetail(row.id)
        : await getAnnouncementDetail(row.id)
    if (res.data) {
      detailItem.value = res.data
    }
  } catch {
    /* 列表数据兜底展示 */
  }
}

onMounted(async () => {
  userStore.initUser()
  if (!getToken()) {
    return
  }
  await loadHomeContent()
  try {
    const res = await getMyCaseList({ pageNum: 1, pageSize: 5 })
    myRecentCases.value = res.data?.records || []
  } catch {
    myRecentCases.value = []
  }
})

function goMessage() {
  router.push('/mine')
}
</script>

<style scoped>
.home-page {
  padding-bottom: 60px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #1989fa, #36cfc9);

  .user-info {
    display: flex;
    align-items: center;

    .info {
      margin-left: 12px;
      color: #fff;

      .name {
        font-size: 16px;
        font-weight: bold;
      }

      .grid {
        font-size: 12px;
        margin-top: 4px;
      }
    }
  }

  .van-icon {
    color: #fff;
  }
}

.quick-grid {
  margin: 20px 0;
}

.content-detail {
  padding: 20px 16px 24px;

  &__title {
    font-size: 17px;
    font-weight: 600;
    margin-bottom: 8px;
  }

  &__meta {
    font-size: 12px;
    color: #969799;
    margin-bottom: 12px;
  }

  &__body {
    font-size: 14px;
    line-height: 1.6;
    color: #323233;
    white-space: pre-wrap;
    max-height: 45vh;
    overflow-y: auto;
    margin-bottom: 16px;
  }
}
</style>
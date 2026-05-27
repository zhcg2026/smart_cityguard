<template>
  <el-dialog
    :model-value="visible"
    :title="dialogTitle"
    width="560px"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-if="item" class="content-detail">
      <div v-if="type === 'announcement'" class="meta-row">
        <el-tag :type="announcementTagType" size="small">{{ announcementTypeLabel }}</el-tag>
        <span v-if="item.docNumber" class="meta-text">文号：{{ item.docNumber }}</span>
      </div>
      <div class="meta-row">
        <span class="meta-text">发布人：{{ item.publisherName || '—' }}</span>
        <span class="meta-text">发布时间：{{ formatDateTime(item.publishTime) }}</span>
      </div>
      <div v-if="item.expireTime" class="meta-row">
        <span class="meta-text">过期时间：{{ formatDateTime(item.expireTime) }}</span>
      </div>
      <div class="content-body">{{ item.content }}</div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { formatDateTime } from '@/utils/dateFormat'

const props = defineProps({
  visible: { type: Boolean, default: false },
  type: { type: String, default: 'announcement' },
  item: { type: Object, default: null }
})

const emit = defineEmits(['update:visible'])

const dialogTitle = computed(() => {
  if (!props.item) return props.type === 'dailytip' ? '今日提示' : '公文通告'
  return props.item.title || (props.type === 'dailytip' ? '今日提示' : '公文通告')
})

const announcementTypeLabel = computed(() => {
  const t = props.item?.announcementType
  return ({ system: '系统通知', business: '业务通告', urgent: '紧急通告' }[t] || '通告')
})

const announcementTagType = computed(() => {
  const t = props.item?.announcementType
  if (t === 'urgent') return 'danger'
  if (t === 'system') return 'warning'
  return 'info'
})
</script>

<style lang="scss" scoped>
.content-detail {
  .meta-row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    color: #909399;
    font-size: 13px;
  }

  .content-body {
    margin-top: 16px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 6px;
    line-height: 1.7;
    white-space: pre-wrap;
    word-break: break-word;
    color: #303133;
  }
}
</style>

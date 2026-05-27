<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="640px"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-if="task" v-loading="loading" class="task-detail">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="任务编号">{{ task.taskCode || task.taskNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">
          <el-tag :type="taskStatusTagType(task.taskStatus)" size="small">
            {{ taskStatusLabel(task.taskStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="关联案件" :span="2">
          <el-button v-if="task.caseId" type="primary" link @click="goCase(task.caseId)">
            {{ task.caseCode || task.caseNo || task.caseId }}
          </el-button>
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="小类">{{ task.smallName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ task.address || '--' }}</el-descriptions-item>
        <el-descriptions-item label="采集员">{{ task.collectorName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="下发人">
          {{ task.assignerName || task.creatorName || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="指派时间">{{ formatDateTime(task.assignTime) }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ formatDateTime(task.deadlineTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDateTime(task.finishTime) }}</el-descriptions-item>
        <el-descriptions-item label="任务结果">
          {{ resultText }}
        </el-descriptions-item>
        <el-descriptions-item v-if="opinionText" label="意见说明" :span="2">
          {{ opinionText }}
        </el-descriptions-item>
      </el-descriptions>
      <p class="hint">现场执行在采集端完成；受理员在案件菜单中跟进立案/结案。</p>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getTaskDetail } from '@/api/task'
import { formatDateTime } from '@/utils/dateFormat'
import {
  taskStatusLabel,
  taskStatusTagType,
  checkResultLabel,
  verifyResultLabel
} from '@/utils/taskLedger'

const props = defineProps({
  visible: { type: Boolean, default: false },
  /** check | verify */
  taskKind: { type: String, required: true },
  taskId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:visible'])

const router = useRouter()
const loading = ref(false)
const task = ref(null)

const title = computed(() => (props.taskKind === 'check' ? '核查任务详情' : '核实任务详情'))

const resultText = computed(() => {
  if (!task.value) return '--'
  if (props.taskKind === 'check') {
    return checkResultLabel(task.value.checkResult)
  }
  return verifyResultLabel(task.value.verifyResult)
})

const opinionText = computed(() => {
  if (!task.value) return ''
  return props.taskKind === 'check' ? task.value.checkOpinion : task.value.verifyOpinion
})

watch(
  () => [props.visible, props.taskId, props.taskKind],
  async ([vis, id]) => {
    if (!vis || !id) {
      task.value = null
      return
    }
    loading.value = true
    try {
      const res = await getTaskDetail(id, props.taskKind)
      task.value = res.data || null
    } catch {
      task.value = null
    } finally {
      loading.value = false
    }
  }
)

function goCase(caseId) {
  emit('update:visible', false)
  router.push(`/case/detail/${caseId}`)
}
</script>

<style lang="scss" scoped>
.task-detail {
  .hint {
    margin-top: 16px;
    font-size: 13px;
    color: #909399;
    line-height: 1.6;
  }
}
</style>

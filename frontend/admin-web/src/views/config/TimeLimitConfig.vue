<template>
  <div class="page-container">
    <el-card>
      <template #header><span>计时规则</span></template>

      <el-alert
        type="info"
        show-icon
        :closable="false"
        class="mb-4"
        title="本页配置计时引擎：时限类型定义、工作时段与节假日。案件处置截止时限请在「案件分类 → 立案条件」中按条维护（与立结案标准一致）。"
      />

      <el-tabs v-model="activeTab">
        <el-tab-pane label="全局规则" name="rules">
          <el-alert
            type="info"
            show-icon
            :closable="false"
            class="mb-4"
            title="含「紧急」的时限类型连续计时；不含「紧急」仅计工作时段（8–12、14–18），不含周末与法定节假日。"
          />
          <el-table v-loading="rulesLoading" :data="ruleList" border stripe>
            <el-table-column prop="typeName" label="类型名称" width="120" />
            <el-table-column prop="timeLimitType" label="类型编码" width="120" />
            <el-table-column label="连续计算" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isContinuous === 1 ? 'danger' : 'info'" size="small">
                  {{ row.isContinuous === 1 ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="含节假日" width="90" align="center">
              <template #default="{ row }">{{ row.includeHoliday === 1 ? '是' : '否' }}</template>
            </el-table-column>
            <el-table-column label="含周末" width="90" align="center">
              <template #default="{ row }">{{ row.includeWeekend === 1 ? '是' : '否' }}</template>
            </el-table-column>
            <el-table-column prop="calcDesc" label="计算说明" min-width="260" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="工作时段" name="worktime">
          <el-alert
            type="info"
            show-icon
            :closable="false"
            class="mb-4"
            title="非连续计时的案件（工作时、工作日）按此处时段折算；1 工作日 = 8 工作小时。"
          />
          <div v-if="workTime" v-loading="workTimeLoading" class="worktime-form">
            <el-form :model="workTime" label-width="100px" style="max-width: 520px">
              <el-form-item label="配置名称">
                <el-input v-model="workTime.configName" :disabled="!isAdmin" />
              </el-form-item>
              <el-form-item label="上午时段">
                <div class="time-range">
                  <el-time-select
                    v-model="workTime.amStartTime"
                    start="06:00"
                    step="00:30"
                    end="12:00"
                    placeholder="开始"
                    :disabled="!isAdmin"
                  />
                  <span class="sep">至</span>
                  <el-time-select
                    v-model="workTime.amEndTime"
                    start="06:00"
                    step="00:30"
                    end="14:00"
                    placeholder="结束"
                    :disabled="!isAdmin"
                  />
                </div>
              </el-form-item>
              <el-form-item label="下午时段">
                <div class="time-range">
                  <el-time-select
                    v-model="workTime.pmStartTime"
                    start="12:00"
                    step="00:30"
                    end="18:00"
                    placeholder="开始"
                    :disabled="!isAdmin"
                  />
                  <span class="sep">至</span>
                  <el-time-select
                    v-model="workTime.pmEndTime"
                    start="12:00"
                    step="00:30"
                    end="22:00"
                    placeholder="结束"
                    :disabled="!isAdmin"
                  />
                </div>
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="workTime.remark" type="textarea" :rows="2" :disabled="!isAdmin" />
              </el-form-item>
              <el-form-item v-if="isAdmin">
                <el-button type="primary" :loading="workTimeSaving" @click="saveWorkTime">保存</el-button>
              </el-form-item>
            </el-form>
          </div>
          <el-empty v-else description="暂无工作时段配置" />
        </el-tab-pane>

        <el-tab-pane label="节假日" name="holiday">
          <div class="toolbar mb-4">
            <el-date-picker
              v-model="holidayYear"
              type="year"
              value-format="YYYY"
              placeholder="选择年份"
              @change="loadHolidays"
            />
            <el-button v-if="isAdmin" type="primary" @click="openHolidayDialog()">新增</el-button>
          </div>
          <el-table v-loading="holidayLoading" :data="holidayList" border stripe>
            <el-table-column prop="holidayDate" label="日期" width="120" />
            <el-table-column label="类型" width="140">
              <template #default="{ row }">
                <el-tag :type="row.holidayType === 'holiday' ? 'warning' : 'success'" size="small">
                  {{ row.holidayType === 'holiday' ? '法定节假日' : '调休工作日' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="holidayName" label="名称" min-width="160" />
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
            <el-table-column v-if="isAdmin" label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openHolidayDialog(row)">编辑</el-button>
                <el-button type="danger" link @click="removeHoliday(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="holidayVisible" :title="holidayForm.id ? '编辑节假日' : '新增节假日'" width="440px">
      <el-form ref="holidayFormRef" :model="holidayForm" :rules="holidayRules" label-width="80px">
        <el-form-item label="日期" prop="holidayDate">
          <el-date-picker
            v-model="holidayForm.holidayDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="类型" prop="holidayType">
          <el-radio-group v-model="holidayForm.holidayType">
            <el-radio value="holiday">法定节假日</el-radio>
            <el-radio value="workday">调休工作日</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="holidayForm.holidayName" placeholder="如：国庆节" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="holidayForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="holidayVisible = false">取消</el-button>
        <el-button type="primary" :loading="holidaySaving" @click="submitHoliday">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { RoleCode } from '@/utils/roleAccess'
import {
  getTimeLimitRules,
  getWorkTimeConfig,
  updateWorkTimeConfig,
  getHolidayConfig,
  saveHoliday,
  deleteHoliday
} from '@/api/config'

const userStore = useUserStore()
const isAdmin = computed(() => (userStore.roles || []).includes(RoleCode.ADMIN))

const activeTab = ref('rules')

const rulesLoading = ref(false)
const ruleList = ref([])

const workTimeLoading = ref(false)
const workTimeSaving = ref(false)
const workTime = ref(null)

const holidayYear = ref(String(new Date().getFullYear()))
const holidayLoading = ref(false)
const holidayList = ref([])
const holidayVisible = ref(false)
const holidaySaving = ref(false)
const holidayFormRef = ref()
const holidayForm = reactive({
  id: null,
  holidayDate: '',
  holidayType: 'holiday',
  holidayName: '',
  remark: ''
})
const holidayRules = {
  holidayDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  holidayType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

onMounted(async () => {
  await Promise.all([loadRules(), loadWorkTime(), loadHolidays()])
})

async function loadRules() {
  rulesLoading.value = true
  try {
    const res = await getTimeLimitRules()
    ruleList.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    rulesLoading.value = false
  }
}

async function loadWorkTime() {
  workTimeLoading.value = true
  try {
    const res = await getWorkTimeConfig()
    const list = res.data || []
    workTime.value = list.find((item) => item.isDefault === 1) || list[0] || null
  } catch (e) {
    console.error(e)
  } finally {
    workTimeLoading.value = false
  }
}

async function saveWorkTime() {
  if (!workTime.value?.id) return
  workTimeSaving.value = true
  try {
    await updateWorkTimeConfig(workTime.value.id, { ...workTime.value })
    ElMessage.success('工作时段已保存')
    await loadWorkTime()
  } catch (e) {
    console.error(e)
  } finally {
    workTimeSaving.value = false
  }
}

async function loadHolidays() {
  holidayLoading.value = true
  try {
    const res = await getHolidayConfig({ year: Number(holidayYear.value) })
    holidayList.value = (res.data || []).sort((a, b) => String(a.holidayDate).localeCompare(String(b.holidayDate)))
  } catch (e) {
    console.error(e)
  } finally {
    holidayLoading.value = false
  }
}

function openHolidayDialog(row) {
  if (row) {
    Object.assign(holidayForm, {
      id: row.id,
      holidayDate: row.holidayDate,
      holidayType: row.holidayType,
      holidayName: row.holidayName || '',
      remark: row.remark || ''
    })
  } else {
    Object.assign(holidayForm, {
      id: null,
      holidayDate: '',
      holidayType: 'holiday',
      holidayName: '',
      remark: ''
    })
  }
  holidayVisible.value = true
}

async function submitHoliday() {
  await holidayFormRef.value?.validate()
  holidaySaving.value = true
  try {
    await saveHoliday({ ...holidayForm })
    ElMessage.success('已保存')
    holidayVisible.value = false
    await loadHolidays()
  } catch (e) {
    console.error(e)
  } finally {
    holidaySaving.value = false
  }
}

async function removeHoliday(row) {
  await ElMessageBox.confirm(`确定删除 ${row.holidayDate} 的配置？`, '提示', { type: 'warning' })
  try {
    await deleteHoliday(row.id)
    ElMessage.success('已删除')
    await loadHolidays()
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped>
.page-container {
  padding: 16px;
}
.mb-4 {
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}
.time-range {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.sep {
  color: var(--el-text-color-secondary);
}
</style>

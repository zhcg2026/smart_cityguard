<template>
  <div class="case-manual-register">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>案件登记</span>
          <span class="sub">受理员电话投诉等人工录入，登记后进入「待立案」，流程与采集上报一致</span>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="register-form">
        <el-divider content-position="left">投诉人信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="投诉人姓名" prop="reporterName">
              <el-input v-model="form.reporterName" placeholder="选填" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系电话" prop="reporterPhone">
              <el-input v-model="form.reporterPhone" placeholder="选填" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="来源说明" prop="sourceDesc">
              <el-input v-model="form.sourceDesc" placeholder="默认：电话投诉" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">案件分类</el-divider>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="事部件类型" prop="categoryType">
              <el-select v-model="form.categoryType" placeholder="请选择" @change="onTypeChange">
                <el-option label="部件" value="component" />
                <el-option label="事件" value="event" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="大类" prop="categoryBigId">
              <el-select
                v-model="form.categoryBigId"
                placeholder="请先选类型"
                :disabled="!form.categoryType"
                filterable
                @change="onBigChange"
              >
                <el-option
                  v-for="item in bigList"
                  :key="item.id"
                  :label="`${item.bigCode || ''} ${item.bigName}`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="小类" prop="categorySmallId">
              <el-select
                v-model="form.categorySmallId"
                placeholder="请先选大类"
                :disabled="!form.categoryBigId"
                filterable
                @change="onSmallChange"
              >
                <el-option
                  v-for="item in smallList"
                  :key="item.id"
                  :label="`${item.smallCode || ''} ${item.smallName}`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="立案条件" prop="conditionId">
              <el-select
                v-model="form.conditionId"
                placeholder="请先选小类"
                :disabled="!form.categorySmallId"
                filterable
                clearable
              >
                <el-option
                  v-for="item in conditionList"
                  :key="item.id"
                  :label="conditionLabel(item)"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">问题信息</el-divider>
        <el-form-item label="问题描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请记录投诉内容" />
        </el-form-item>
        <el-form-item label="发生地址" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="2" placeholder="可在地图点选后自动填入" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="经度">
              <el-input v-model="form.longitude" placeholder="地图点选" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="纬度">
              <el-input v-model="form.latitude" placeholder="地图点选" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="登记备注">
              <el-input v-model="form.remark" placeholder="选填，写入流程记录" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="地图选点">
          <div class="map-wrap">
            <div ref="mapRef" class="map-box" />
            <div class="map-actions">
              <el-button size="small" @click="locateCurrent">定位到当前位置</el-button>
              <span class="map-tip">点击地图设置案发位置</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="现场照片">
          <el-upload
            v-model:file-list="fileList"
            list-type="picture-card"
            :http-request="uploadFile"
            :limit="5"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">选填，最多 5 张；需上传成功后再提交</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交登记</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getCategoryBigList, getCategorySmallList, getConditions } from '@/api/config'
import { acceptorRegisterCase } from '@/api/case'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref()
const mapRef = ref()
const submitting = ref(false)
const bigList = ref([])
const smallList = ref([])
const conditionList = ref([])
const fileList = ref([])

let mapInst = null
let markerInst = null
let geocoderInst = null

const DEFAULT_CENTER = [111.0, 35.03]

const form = reactive({
  reporterName: '',
  reporterPhone: '',
  sourceDesc: '电话投诉',
  categoryType: '',
  categoryBigId: null,
  categoryBigCode: '',
  categoryBigName: '',
  categorySmallId: null,
  categorySmallCode: '',
  categorySmallName: '',
  conditionId: null,
  description: '',
  address: '',
  longitude: '',
  latitude: '',
  remark: '',
  attachments: []
})

const rules = {
  categoryType: [{ required: true, message: '请选择事部件类型', trigger: 'change' }],
  categoryBigId: [{ required: true, message: '请选择大类', trigger: 'change' }],
  categorySmallId: [{ required: true, message: '请选择小类', trigger: 'change' }],
  description: [{ required: true, message: '请填写问题描述', trigger: 'blur' }],
  address: [{ required: true, message: '请填写发生地址', trigger: 'blur' }]
}

function conditionLabel(item) {
  return item?.conditionContent || item?.conditionDesc || item?.conditionName || ''
}

function categoryApiType(type) {
  return type === 'component' ? 1 : 2
}

async function onTypeChange() {
  form.categoryBigId = null
  form.categorySmallId = null
  form.conditionId = null
  bigList.value = []
  smallList.value = []
  conditionList.value = []
  if (!form.categoryType) return
  const res = await getCategoryBigList({ type: categoryApiType(form.categoryType) })
  bigList.value = res.data || []
}

async function onBigChange(bigId) {
  form.categorySmallId = null
  form.conditionId = null
  smallList.value = []
  conditionList.value = []
  const big = bigList.value.find((b) => b.id === bigId)
  form.categoryBigCode = big?.bigCode || ''
  form.categoryBigName = big?.bigName || ''
  if (!bigId) return
  const res = await getCategorySmallList(bigId)
  smallList.value = res.data || []
}

async function onSmallChange(smallId) {
  form.conditionId = null
  conditionList.value = []
  const small = smallList.value.find((s) => s.id === smallId)
  form.categorySmallCode = small?.smallCode || ''
  form.categorySmallName = small?.smallName || ''
  if (!smallId) return
  const res = await getConditions(smallId)
  conditionList.value = res.data || []
}

function setMapPosition(lng, lat) {
  form.longitude = String(lng)
  form.latitude = String(lat)
  if (!mapInst || typeof window.AMap === 'undefined') return
  const pos = [lng, lat]
  if (markerInst) {
    markerInst.setPosition(pos)
  } else {
    markerInst = new window.AMap.Marker({ position: pos })
    mapInst.add(markerInst)
  }
  mapInst.setCenter(pos)
}

function reverseGeocode(lng, lat) {
  if (!geocoderInst || typeof window.AMap === 'undefined') return
  geocoderInst.getAddress([lng, lat], (status, result) => {
    if (status === 'complete' && result?.regeocode?.formattedAddress) {
      form.address = result.regeocode.formattedAddress
    }
  })
}

function destroyMap() {
  if (mapInst) {
    try {
      mapInst.destroy()
    } catch {
      /* ignore */
    }
    mapInst = null
    markerInst = null
  }
  if (mapRef.value) {
    mapRef.value.innerHTML = ''
  }
}

async function initMap() {
  await nextTick()
  if (!mapRef.value || typeof window.AMap === 'undefined') {
    return
  }
  destroyMap()
  mapInst = new window.AMap.Map(mapRef.value, {
    zoom: 13,
    center: DEFAULT_CENTER,
    viewMode: '2D'
  })
  window.AMap.plugin('AMap.Geocoder', () => {
    geocoderInst = new window.AMap.Geocoder()
  })
  mapInst.on('click', (e) => {
    const lng = e.lnglat.getLng()
    const lat = e.lnglat.getLat()
    setMapPosition(lng, lat)
    reverseGeocode(lng, lat)
  })
}

function locateCurrent() {
  if (!navigator.geolocation) {
    ElMessage.warning('浏览器不支持定位')
    return
  }
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const lng = pos.coords.longitude
      const lat = pos.coords.latitude
      setMapPosition(lng, lat)
      reverseGeocode(lng, lat)
    },
    () => ElMessage.warning('定位失败，请直接在地图上点选')
  )
}

async function uploadFile(options) {
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const res = await request({
      url: '/file/upload',
      method: 'post',
      data: fd,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const url = typeof res.data === 'string' ? res.data : ''
    options.onSuccess(res, options.file)
    if (url) {
      options.file.url = url
      if (!form.attachments.includes(url)) {
        form.attachments.push(url)
      }
    }
  } catch (e) {
    options.onError(e)
  }
}

function collectAttachments() {
  return fileList.value
    .map((f) => {
      if (typeof f.url === 'string' && f.url) return f.url
      const d = f.response?.data
      return typeof d === 'string' ? d : ''
    })
    .filter(Boolean)
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const lng = form.longitude ? Number(form.longitude) : null
  const lat = form.latitude ? Number(form.latitude) : null
  if (lng == null || lat == null || !Number.isFinite(lng) || !Number.isFinite(lat)) {
    ElMessage.warning('请在地图上点选案发位置')
    return
  }

  const condition = conditionList.value.find((c) => c.id === form.conditionId)
  const conditionDesc = condition ? conditionLabel(condition) : undefined
  const standardId = form.conditionId != null ? Number(form.conditionId) : null

  submitting.value = true
  try {
    const res = await acceptorRegisterCase({
      categoryType: form.categoryType,
      bigCode: form.categoryBigCode,
      bigName: form.categoryBigName,
      smallCode: form.categorySmallCode,
      smallName: form.categorySmallName,
      smallId: form.categorySmallId,
      standardId,
      conditionDesc,
      description: form.description.trim(),
      address: form.address.trim(),
      longitude: lng,
      latitude: lat,
      reporterName: form.reporterName?.trim() || undefined,
      reporterPhone: form.reporterPhone?.trim() || undefined,
      sourceDesc: form.sourceDesc?.trim() || '电话投诉',
      remark: form.remark?.trim() || undefined,
      attachments: collectAttachments()
    })
    const caseId = res.data?.id
    ElMessage.success('登记成功，案件已进入待立案')
    if (caseId) {
      router.push(`/case/detail/${caseId}`)
    } else {
      router.push('/case/pending-register')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '登记失败')
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  formRef.value?.resetFields()
  form.sourceDesc = '电话投诉'
  form.attachments = []
  fileList.value = []
  bigList.value = []
  smallList.value = []
  conditionList.value = []
  destroyMap()
  initMap()
}

function goBack() {
  router.back()
}

onMounted(() => {
  initMap()
})

onBeforeUnmount(() => {
  destroyMap()
})
</script>

<style lang="scss" scoped>
.case-manual-register {
  .card-header {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .sub {
      font-size: 13px;
      color: #909399;
      font-weight: normal;
    }
  }

  .register-form {
    max-width: 1100px;
  }

  .map-wrap {
    width: 100%;
    max-width: 720px;

    .map-box {
      width: 100%;
      height: 320px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
    }

    .map-actions {
      margin-top: 8px;
      display: flex;
      align-items: center;
      gap: 12px;

      .map-tip {
        font-size: 13px;
        color: #909399;
      }
    }
  }

  .upload-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 8px;
  }
}
</style>

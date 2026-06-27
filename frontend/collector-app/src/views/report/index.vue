<template>
  <div class="report-page">
    <van-nav-bar title="问题上报" left-arrow @click-left="goBack" />

    <div class="form-content">
      <van-cell-group title="案件分类" inset>
        <van-field
          :model-value="categoryTypeLabel"
          is-link
          readonly
          label="事部件类型"
          placeholder="请选择部件或事件"
          @click="openPicker('type')"
        />
        <van-field
          :model-value="form.categoryBigName"
          is-link
          readonly
          label="大类"
          :placeholder="form.categoryType ? '请选择大类' : '请先选择事部件类型'"
          :disabled="!form.categoryType"
          @click="openPicker('big')"
        />
        <van-field
          :model-value="form.categorySmallName"
          is-link
          readonly
          label="小类"
          :placeholder="form.categoryBigId ? '请选择小类' : '请先选择大类'"
          :disabled="!form.categoryBigId"
          @click="openPicker('small')"
        />
        <van-field
          :model-value="conditionDisplay"
          is-link
          readonly
          label="立案条件"
          :placeholder="conditionPlaceholder"
          :disabled="!form.categorySmallId || !conditionsLoaded"
          @click="openPicker('condition')"
        />
      </van-cell-group>

      <van-cell-group title="问题描述" inset>
        <van-field
          v-model="form.description"
          rows="3"
          autosize
          type="textarea"
          placeholder="请描述问题情况..."
        />
      </van-cell-group>

      <van-cell-group title="现场照片" inset>
        <van-notice-bar
          v-if="fileList.length > 0 && form.attachments.length === 0"
          wrapable
          color="#ed6a0c"
          background="#fffbe8"
          left-icon="warning-o"
        >
          照片尚未上传成功。请先启动 MinIO（端口 9000），选图后需看到「上传成功」才能提交。
        </van-notice-bar>
        <van-uploader
          v-model="fileList"
          multiple
          :max-count="5"
          :after-read="afterRead"
          @delete="onUploaderDelete"
        />
        <van-cell
          v-if="form.attachments.length > 0"
          title="已上传"
          :value="`${form.attachments.length} 张`"
        />
      </van-cell-group>

      <van-cell-group title="案件位置（点地图选点）" inset>
        <div :key="mapRenderKey" ref="mapBoxRef" class="amap-box" />
        <div class="map-actions">
          <van-button size="small" type="primary" plain block @click="locateOnMap">
            高德定位
          </van-button>
          <van-button size="small" type="default" plain block @click="browserLocateFallback">
            浏览器定位（备用）
          </van-button>
        </div>
        <van-notice-bar
          v-if="!isSecureContext"
          wrapable
          color="#ed6a0c"
          background="#fffbe8"
          left-icon="warning-o"
        >
          当前为 HTTP 环境，浏览器定位可能受限。建议使用高德定位或在地图上直接点选位置。
        </van-notice-bar>
        <van-notice-bar v-if="!amapKeyConfigured" wrapable left-icon="info-o" color="#1989fa" background="#ecf9ff">
          未配置高德 Key 时地图不可用：请在 frontend/collector-app 下复制 .env.example 为 .env.local，填写 VITE_AMAP_KEY（及安全密钥），重启 dev 服务。
        </van-notice-bar>
        <van-notice-bar
          v-if="assignedGrids.length > 0"
          wrapable
          color="#1989fa"
          background="#ecf9ff"
          left-icon="info-o"
        >
          您的责任片区：{{ assignedGridLabel }}。仅可在该片区的地图范围内选点上报。
        </van-notice-bar>
        <van-notice-bar
          v-else-if="gridsLoaded"
          wrapable
          color="#ed6a0c"
          background="#fffbe8"
          left-icon="warning-o"
        >
          您尚未绑定责任片区，无法上报。请联系管理员在「地理信息-网格管理」中绑定。
        </van-notice-bar>
        <van-notice-bar
          v-if="!locationInAssignedArea && locationCheckHint"
          wrapable
          color="#ee0a24"
          background="#ffe1e1"
          left-icon="warning-o"
        >
          {{ locationCheckHint }}
        </van-notice-bar>
      </van-cell-group>

      <van-cell-group title="发生地址" inset>
        <van-field
          v-model="form.address"
          rows="2"
          autosize
          type="textarea"
          placeholder="在地图上点选后自动填入，可修改补充"
        />
      </van-cell-group>

      <van-cell-group title="坐标信息（GCJ-02，报送系统）" inset>
        <van-cell title="经度" :value="form.lng != null ? String(form.lng) : '未选点'" />
        <van-cell title="纬度" :value="form.lat != null ? String(form.lat) : '未选点'" />
      </van-cell-group>
    </div>

    <Teleport to="body">
      <div v-show="!pickerVisible" class="bottom-btns">
        <van-button
          native-type="button"
          block
          type="primary"
          :loading="submitting"
          @click="onSubmitClick"
        >
          提交上报
        </van-button>
      </div>
    </Teleport>

    <van-popup
      v-model:show="pickerVisible"
      position="bottom"
      round
      destroy-on-close
      teleport="body"
      :z-index="3000"
    >
      <van-picker
        :title="pickerTitle"
        :columns="pickerColumns"
        @confirm="onPickerConfirm"
        @cancel="pickerVisible = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
defineOptions({ name: 'Report' })
import { ref, reactive, onMounted, nextTick, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showLoadingToast, showDialog, closeToast } from 'vant'
import { showUploadFailure, showUploadSuccess } from '@/utils/uploadFeedback'
import { getCategoryBigList, getCategorySmallList, getConditions, reportCase, uploadFile } from '@/api/case'
import { getCollectorRespGrids, checkPointInArea } from '@/api/geo'
import { useUserStore } from '@/stores/user'
import { loadAmapScript, DEFAULT_MAP_CENTER } from '@/utils/amapLoader'

const router = useRouter()
const userStore = useUserStore()
const submitting = ref(false)
const mapBoxRef = ref(null)
const mapRenderKey = ref(0)
let mapInst = null
let markerInst = null
let geocoderInst = null
let AMapRef = null

const amapKeyConfigured = computed(() => Boolean(import.meta.env.VITE_AMAP_KEY))
const isSecureContext = computed(() => window.isSecureContext)

const CATEGORY_TYPE_OPTIONS = [
  { text: '部件', value: 'component', apiType: 1 },
  { text: '事件', value: 'event', apiType: 2 }
]

const form = reactive({
  source: 1,
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
  lng: null,
  lat: null,
  attachments: []
})

const categoryBigList = ref([])
const categorySmallList = ref([])
const conditionList = ref([])
const conditionsLoaded = ref(false)
const fileList = ref([])
const assignedGrids = ref([])
const gridsLoaded = ref(false)
const locationInAssignedArea = ref(true)
const locationCheckHint = ref('')

const pickerVisible = ref(false)
const pickerKind = ref('')
const pickerColumns = ref([])

const assignedGridLabel = computed(() =>
  assignedGrids.value.map((g) => g.respGridName).filter(Boolean).join('、')
)

const categoryTypeLabel = computed(() => {
  const opt = CATEGORY_TYPE_OPTIONS.find((o) => o.value === form.categoryType)
  return opt?.text || ''
})

const conditionDisplay = computed(() => {
  if (form.conditionId == null || form.conditionId === '') {
    return ''
  }
  const item = conditionList.value.find((c) => c.id == form.conditionId)
  return item ? conditionLabel(item) : ''
})

const conditionPlaceholder = computed(() => {
  if (!form.categorySmallId) {
    return '请先选择小类'
  }
  if (!conditionsLoaded.value) {
    return '加载中...'
  }
  if (conditionList.value.length === 0) {
    return '该小类暂无立案条件'
  }
  return '请选择立案条件'
})

const pickerTitle = computed(() => {
  const titles = {
    type: '选择事部件类型',
    big: '选择大类',
    small: '选择小类',
    condition: '选择立案条件'
  }
  return titles[pickerKind.value] || ''
})

function conditionLabel(item) {
  return item?.conditionContent || item?.conditionDesc || ''
}

onMounted(async () => {
  userStore.initUser()
  if (!userStore.userInfo?.id) {
    try {
      await userStore.getUserInfo()
    } catch {
      // 未登录时由路由拦截
    }
  }
  await loadAssignedGrids()
  ensureDefaultCoords()
  mapRenderKey.value += 1
  nextTick(() => {
    window.setTimeout(() => {
      void initAmapIfPossible()
    }, 80)
  })
})

onBeforeUnmount(() => {
  destroyMap()
})

function openPicker(kind) {
  if (kind === 'big' && !form.categoryType) {
    tip('请先选择事部件类型')
    return
  }
  if (kind === 'small' && !form.categoryBigId) {
    tip('请先选择大类')
    return
  }
  if (kind === 'condition') {
    if (!form.categorySmallId) {
      tip('请先选择小类')
      return
    }
    if (!conditionsLoaded.value) {
      return
    }
    if (conditionList.value.length === 0) {
      tip('该小类暂无立案条件')
      return
    }
  }

  pickerKind.value = kind
  if (kind === 'type') {
    pickerColumns.value = CATEGORY_TYPE_OPTIONS.map((o) => ({ text: o.text, value: o.value }))
  } else if (kind === 'big') {
    if (categoryBigList.value.length === 0) {
      tip('该类型下暂无大类，请联系管理员维护')
      return
    }
    pickerColumns.value = categoryBigList.value.map((item) => ({
      text: item.bigName,
      value: item.id
    }))
  } else if (kind === 'small') {
    if (categorySmallList.value.length === 0) {
      tip('该大类下暂无小类，请联系管理员维护')
      return
    }
    pickerColumns.value = categorySmallList.value.map((item) => ({
      text: item.smallName,
      value: item.id
    }))
  } else if (kind === 'condition') {
    pickerColumns.value = conditionList.value.map((item) => ({
      text: conditionLabel(item),
      value: item.id
    }))
  }
  pickerVisible.value = true
}

async function onPickerConfirm({ selectedOptions }) {
  const selected = selectedOptions?.[0]
  if (!selected) {
    pickerVisible.value = false
    return
  }

  if (pickerKind.value === 'type') {
    if (form.categoryType !== selected.value) {
      form.categoryType = selected.value
      resetBigCategory()
      await loadCategoryBigByType(selected.value)
    }
  } else if (pickerKind.value === 'big') {
    const item = categoryBigList.value.find((c) => c.id === selected.value)
    if (item) {
      await selectBigCategory(item)
    }
  } else if (pickerKind.value === 'small') {
    const item = categorySmallList.value.find((c) => c.id === selected.value)
    if (item) {
      await selectSmallCategory(item)
    }
  } else if (pickerKind.value === 'condition') {
    form.conditionId = selected.value
  }

  pickerVisible.value = false
}

function resetBigCategory() {
  form.categoryBigId = null
  form.categoryBigCode = ''
  form.categoryBigName = ''
  categoryBigList.value = []
  resetSmallCategory()
}

function resetSmallCategory() {
  form.categorySmallId = null
  form.categorySmallCode = ''
  form.categorySmallName = ''
  categorySmallList.value = []
  conditionList.value = []
  conditionsLoaded.value = false
  form.conditionId = null
}

async function loadCategoryBigByType(categoryType) {
  const opt = CATEGORY_TYPE_OPTIONS.find((o) => o.value === categoryType)
  if (!opt) {
    return
  }
  try {
    const res = await getCategoryBigList(opt.apiType)
    categoryBigList.value = res.data || []
  } catch {
    categoryBigList.value = []
    showToast('获取大类失败')
  }
}

async function loadCategorySmall(bigId) {
  const res = await getCategorySmallList(bigId)
  categorySmallList.value = res.data || []
}

async function loadConditions(smallId) {
  conditionsLoaded.value = false
  try {
    const res = await getConditions(smallId)
    conditionList.value = res.data || []
  } catch {
    conditionList.value = []
    showToast('获取立案条件失败')
  } finally {
    conditionsLoaded.value = true
  }
}

async function selectBigCategory(item) {
  form.categoryBigId = item.id
  form.categoryBigCode = item.bigCode || ''
  form.categoryBigName = item.bigName || ''
  resetSmallCategory()
  try {
    await loadCategorySmall(item.id)
  } catch {
    categorySmallList.value = []
    showToast({ message: '获取小类失败', position: 'top' })
  }
}

async function selectSmallCategory(item) {
  form.categorySmallId = item.id
  form.categorySmallCode = item.smallCode || ''
  form.categorySmallName = item.smallName || ''
  form.conditionId = null
  await loadConditions(item.id)
}

async function afterRead(file) {
  showLoadingToast({ message: '上传中...', forbidClick: true, duration: 0 })
  const files = Array.isArray(file) ? file : [file]
  try {
    for (const f of files) {
      const res = await uploadFile(f.file)
      const url = typeof res.data === 'string' ? res.data : (res.data?.url || '')
      if (!url) {
        throw new Error('服务器未返回文件地址')
      }
      if (!form.attachments.includes(url)) {
        form.attachments.push(url)
      }
      f.url = url
    }
    syncAttachmentsFromFileList()
    showUploadSuccess()
  } catch (error) {
    for (const f of files) {
      const idx = fileList.value.indexOf(f)
      if (idx >= 0) {
        fileList.value.splice(idx, 1)
      }
    }
    syncAttachmentsFromFileList()
    await showUploadFailure(error)
  }
}

function syncAttachmentsFromFileList() {
  const urls = fileList.value
    .map((f) => f.url)
    .filter(
      (url) =>
        typeof url === 'string' &&
        url.length > 0 &&
        !url.startsWith('blob:') &&
        !url.startsWith('data:')
    )
  if (urls.length > 0) {
    form.attachments = [...new Set(urls)]
  }
}

function hasPendingLocalPhotos() {
  return fileList.value.some(
    (f) => f.url && (String(f.url).startsWith('blob:') || String(f.url).startsWith('data:'))
  )
}

async function loadAssignedGrids() {
  gridsLoaded.value = false
  const userId = userStore.userInfo?.id ?? userStore.userInfo?.userId
  if (!userId) {
    assignedGrids.value = []
    gridsLoaded.value = true
    return
  }
  try {
    const res = await getCollectorRespGrids(userId)
    assignedGrids.value = res.data || []
  } catch {
    assignedGrids.value = []
  } finally {
    gridsLoaded.value = true
  }
}

async function checkReportLocation() {
  if (form.lng == null || form.lat == null) {
    locationInAssignedArea.value = true
    locationCheckHint.value = ''
    return true
  }
  if (assignedGrids.value.length === 0) {
    locationInAssignedArea.value = false
    locationCheckHint.value = '您尚未绑定责任片区，无法上报'
    return false
  }
  const lng = Number(form.lng)
  const lat = Number(form.lat)
  for (const grid of assignedGrids.value) {
    try {
      const res = await checkPointInArea(grid.id, lng, lat)
      if (res.data === true) {
        locationInAssignedArea.value = true
        locationCheckHint.value = ''
        return true
      }
    } catch {
      // 单次校验失败继续尝试其他片区
    }
  }
  locationInAssignedArea.value = false
  locationCheckHint.value = `当前位置不在您的责任片区（${assignedGridLabel.value}）内，无法提交`
  return false
}

function ensureDefaultCoords() {
  if (form.lng == null || form.lat == null) {
    const g = assignedGrids.value[0]
    if (g?.centerLng != null && g?.centerLat != null) {
      form.lng = Number(g.centerLng)
      form.lat = Number(g.centerLat)
    } else {
      form.lng = DEFAULT_MAP_CENTER[0]
      form.lat = DEFAULT_MAP_CENTER[1]
    }
  }
}

async function onSubmitClick() {
  closeToast()
  await submitReport()
}

function onUploaderDelete(item) {
  const url = item?.url
  if (!url) {
    return
  }
  const i = form.attachments.indexOf(url)
  if (i > -1) {
    form.attachments.splice(i, 1)
  }
}

function destroyMap() {
  if (mapInst) {
    try {
      mapInst.destroy()
    } catch {
      // 地图实例可能已销毁
    }
    mapInst = null
    markerInst = null
    geocoderInst = null
    AMapRef = null
  }
  if (mapBoxRef.value) {
    mapBoxRef.value.innerHTML = ''
  }
}

function applyLngLatToForm(lnglat) {
  const lng = typeof lnglat.getLng === 'function' ? lnglat.getLng() : lnglat.lng
  const lat = typeof lnglat.getLat === 'function' ? lnglat.getLat() : lnglat.lat
  form.lng = lng
  form.lat = lat
  if (!geocoderInst) {
    if (!form.address?.trim()) {
      form.address = `${lng.toFixed(6)}, ${lat.toFixed(6)}`
    }
    void checkReportLocation()
    return
  }
  geocoderInst.getAddress([lng, lat], (status, result) => {
    if (status === 'complete' && result?.regeocode?.formattedAddress) {
      form.address = result.regeocode.formattedAddress
    } else if (!form.address?.trim()) {
      form.address = `${lng.toFixed(6)}, ${lat.toFixed(6)}`
    }
    void checkReportLocation()
  })
}

async function initAmapIfPossible() {
  if (!amapKeyConfigured.value) {
    return
  }
  await nextTick()
  if (mapInst) {
    mapInst.resize()
    return
  }
  const el = mapBoxRef.value
  if (!el || el.offsetHeight < 10) {
    return
  }
  let AMap
  try {
    AMap = await loadAmapScript()
  } catch (e) {
    tip(e?.message || '高德地图加载失败，请检查 Key 与安全密钥配置')
    return
  }
  AMapRef = AMap
  const center =
    form.lng != null && form.lat != null
      ? [Number(form.lng), Number(form.lat)]
      : [...DEFAULT_MAP_CENTER]

  mapInst = new AMap.Map(el, {
    zoom: 16,
    center
  })
  mapInst.addControl(new AMap.Scale())
  geocoderInst = new AMap.Geocoder({ city: '全国' })
  markerInst = new AMap.Marker({
    position: center,
    draggable: true
  })
  mapInst.add(markerInst)

  applyLngLatToForm(markerInst.getPosition())

  mapInst.on('click', (e) => {
    markerInst.setPosition(e.lnglat)
    applyLngLatToForm(e.lnglat)
  })
  markerInst.on('dragend', (e) => {
    applyLngLatToForm(e.target.getPosition())
  })
}

function locateOnMap() {
  if (!AMapRef || !mapInst || !markerInst) {
    tip('地图未就绪，请稍候或检查高德 Key 配置')
    return
  }
  showLoadingToast({ message: '定位中...', forbidClick: true, duration: 0 })
  try {
    const gl = new AMapRef.Geolocation({
      enableHighAccuracy: true,
      timeout: 15000,
      buttonPosition: 'RB',
      showCircle: true,
      showMarker: true
    })
    gl.getCurrentPosition((status, result) => {
      closeToast()
      if (status === 'complete' && result?.position) {
        const p = result.position
        mapInst.setZoomAndCenter(16, p)
        markerInst.setPosition(p)
        applyLngLatToForm(p)
        tip('定位成功')
      } else {
        console.warn('高德定位失败:', result?.message || result)
        tip('定位失败，请在地图上点选位置或使用备用定位')
      }
    })
  } catch (e) {
    closeToast()
    console.error('高德定位异常:', e)
    tip('定位功能异常，请在地图上点选位置')
  }
}

function browserLocateFallback() {
  if (!('geolocation' in navigator)) {
    tip('当前浏览器不支持定位')
    return
  }
  showLoadingToast({ message: '定位中...', forbidClick: true, duration: 0 })
  navigator.geolocation.getCurrentPosition(
    (position) => {
      closeToast()
      const lng = position.coords.longitude
      const lat = position.coords.latitude
      form.lng = lng
      form.lat = lat
      if (!form.address?.trim()) {
        form.address = `${lng.toFixed(6)}, ${lat.toFixed(6)}`
      }
      if (mapInst && markerInst && AMapRef) {
        const p = new AMapRef.LngLat(lng, lat)
        mapInst.setZoomAndCenter(16, p)
        markerInst.setPosition(p)
        if (geocoderInst) {
          applyLngLatToForm(p)
        }
      }
      tip('已获取坐标（可能与高德地图有偏差，建议在高德地图上再点选校正）')
    },
    (error) => {
      closeToast()
      let msg = '定位失败，请在地图上点选或手动输入地址'
      if (error.code === 1) {
        msg = '定位被拒绝，请在浏览器设置中允许位置权限'
      } else if (error.code === 2) {
        msg = '无法获取位置信息'
      } else if (error.code === 3) {
        msg = '定位超时，请重试'
      }
      tip(msg)
      console.warn('浏览器定位失败:', error)
    },
    { enableHighAccuracy: true, timeout: 15000 }
  )
}

function tip(msg) {
  showToast({ message: msg, position: 'top', duration: 2500 })
}

function submitBlock(msg) {
  showDialog({
    title: '无法提交',
    message: msg,
    confirmButtonText: '知道了'
  })
}

async function submitReport() {
  if (!form.categoryType) {
    submitBlock('请选择事部件类型（部件或事件）')
    return
  }
  if (!form.categoryBigId) {
    submitBlock('请选择大类')
    return
  }
  if (!form.categorySmallId) {
    submitBlock('请选择小类')
    return
  }
  if (conditionList.value.length > 0 && (form.conditionId == null || form.conditionId === '')) {
    submitBlock('请选择立案条件')
    return
  }
  if (!form.description?.trim()) {
    submitBlock('请填写问题描述')
    return
  }
  if (!form.address?.trim()) {
    submitBlock('请填写发生地址，或在地图上点选位置')
    return
  }
  syncAttachmentsFromFileList()
  if (hasPendingLocalPhotos() || form.attachments.length === 0) {
    submitBlock(
      '现场照片未上传成功，无法提交。\n\n请确认：\n1. 已启动 MinIO（运行项目根目录 start-minio.ps1，端口 9000）\n2. 选图后出现「上传成功」提示\n3. 下方显示「已上传 N 张」'
    )
    return
  }
  if (form.lng == null || form.lat == null) {
    submitBlock('请点「浏览器定位」或在地图上选点后再提交')
    return
  }

  const lng = Number(form.lng)
  const lat = Number(form.lat)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
    submitBlock('经纬度无效，请重新选点')
    return
  }

  let user = userStore.userInfo || {}
  let reporterId = user.id ?? user.userId
  if (reporterId == null || reporterId === '') {
    try {
      await userStore.getUserInfo()
      user = userStore.userInfo || {}
      reporterId = user.id ?? user.userId
    } catch {
      // ignore
    }
  }
  if (reporterId == null || reporterId === '') {
    submitBlock('用户信息缺失，请重新登录')
    return
  }

  if (!(await checkReportLocation())) {
    submitBlock(locationCheckHint.value || '当前位置不在您的责任片区内，无法提交')
    return
  }

  const selectedCondition = conditionList.value.find((c) => c.id == form.conditionId)
  const conditionDesc = selectedCondition ? conditionLabel(selectedCondition) : undefined
  const standardId =
    form.conditionId != null && form.conditionId !== '' ? Number(form.conditionId) : null

  submitting.value = true
  showLoadingToast({ message: '提交中…', forbidClick: true, duration: 0 })
  try {
    await reportCase({
      source: form.source,
      categoryType: form.categoryType,
      bigCode: form.categoryBigCode,
      bigName: form.categoryBigName,
      smallCode: form.categorySmallCode,
      smallName: form.categorySmallName,
      smallId: form.categorySmallId,
      standardId,
      conditionDesc: conditionDesc || undefined,
      description: form.description.trim(),
      address: form.address.trim(),
      longitude: lng,
      latitude: lat,
      attachments: [...form.attachments],
      reporterId,
      reporterName: user.realName || user.username,
      reporterPhone: user.phone || ''
    })
    closeToast()
    destroyMap()
    showSuccessToast({ message: '上报成功', position: 'top' })
    router.push('/home')
  } catch (error) {
    closeToast()
    const apiMsg = error?.response?.data?.message
    tip(apiMsg || '提交失败，请检查网络、登录状态或照片是否已上传成功')
    console.error('reportCase failed', error)
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.back()
}
</script>

<style scoped>
.report-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: calc(80px + env(safe-area-inset-bottom, 0px));
}

.form-content {
  padding: 12px 0 20px;
}

.bottom-btns {
  position: fixed;
  bottom: calc(50px + env(safe-area-inset-bottom, 0px) + 6px);
  left: 0;
  right: 0;
  padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom, 0px));
  background: #fff;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
  z-index: 100;
}

.amap-box {
  position: relative;
  z-index: 0;
  width: 100%;
  height: 200px;
  border-radius: 8px;
  overflow: hidden;
  background: #e8ecf0;
  isolation: isolate;
}

:deep(.amap-container) {
  z-index: 0 !important;
}

.map-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}
</style>

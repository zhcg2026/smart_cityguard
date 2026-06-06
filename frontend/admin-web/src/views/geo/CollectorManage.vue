<template>
  <div class="collector-manage">
    <div class="collector-layout">
      <div class="collector-sidebar">
        <el-card class="sidebar-card">
          <template #header>
            <div class="header-row">
              <span>采集员</span>
              <el-button type="primary" link @click="reload">刷新</el-button>
            </div>
          </template>

          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索姓名/账号"
            class="search-input"
          />

          <div class="map-options">
            <el-checkbox v-model="showCases">显示上报案件</el-checkbox>
            <el-select v-model="caseDays" size="small" style="width: 100px" @change="reload">
              <el-option :value="7" label="近7天" />
              <el-option :value="30" label="近30天" />
              <el-option :value="90" label="近90天" />
            </el-select>
          </div>
          <p v-if="showCases && !selectedCollector" class="case-hint">点选采集员后，地图仅显示该员上报的案件</p>
          <p v-else-if="showCases && selectedCollector" class="case-hint">
            当前 {{ visibleCaseCount }} 条上报案件（近 {{ caseDays }} 天）
          </p>

          <div v-loading="loading" class="collector-list">
            <div
              v-for="c in filteredCollectors"
              :key="c.id"
              class="collector-item"
              :class="{ active: selectedCollectorId === c.id }"
              @click="selectCollector(c)"
            >
              <div class="collector-name">{{ collectorLabel(c) }}</div>
              <div class="collector-meta">
                <span>{{ c.username }}</span>
                <el-tag size="small" type="info">{{ gridCount(c) }} 个片区</el-tag>
              </div>
            </div>
            <el-empty v-if="!loading && filteredCollectors.length === 0" description="暂无采集员" />
          </div>

          <div v-if="selectedCollector" class="collector-detail">
            <div class="detail-title">已绑定片区</div>
            <div v-if="selectedCollectorGrids.length" class="grid-tags">
              <el-tag
                v-for="g in selectedCollectorGrids"
                :key="g.id"
                size="small"
                class="grid-tag"
                @click="focusGrid(g)"
              >
                {{ g.respGridName }}
              </el-tag>
            </div>
            <el-text v-else type="warning" size="small">未绑定任何片区</el-text>
            <el-button
              type="primary"
              size="small"
              class="bind-btn"
              @click="openGridBindDialog"
            >
              管理片区绑定
            </el-button>
          </div>
        </el-card>
      </div>

      <div class="map-panel">
        <div class="map-toolbar">
          <span class="legend-item"><i class="dot grid" /> 责任片区</span>
          <span v-if="showCases && selectedCollector" class="legend-item">
            <i class="dot case" /> 该员上报（{{ visibleCaseCount }}）
          </span>
          <span v-if="selectedCollector" class="legend-hint">
            已高亮 {{ collectorLabel(selectedCollector) }} 的片区
          </span>
        </div>
        <div ref="mapContainer" class="amap-container" />
      </div>
    </div>

    <el-dialog v-model="bindDialogVisible" title="管理片区绑定" width="520px">
      <p class="bind-hint">
        为 <strong>{{ selectedCollector ? collectorLabel(selectedCollector) : '' }}</strong> 选择责任片区（多选）
      </p>
      <el-select
        v-model="bindGridIds"
        multiple
        filterable
        collapse-tags
        collapse-tags-tooltip
        placeholder="选择片区"
        style="width: 100%"
      >
        <el-option
          v-for="g in grids"
          :key="g.id"
          :label="g.respGridName"
          :value="g.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindSubmitting" @click="submitGridBind">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCollectorMapOverview, setRespGridCollectors } from '@/api/geo'
import { formatCaseStatusLabel } from '@/utils/caseStatus'
import { waitForElementSize, waitForGlobalAmap } from '@/utils/mapUtil'

const router = useRouter()

const loading = ref(false)
const keyword = ref('')
const showCases = ref(true)
const caseDays = ref(30)
const collectors = ref([])
const grids = ref([])
const cases = ref([])
const selectedCollectorId = ref(null)

const mapContainer = ref(null)
let map = null
let polygons = []
let caseMarkers = []
let collectorMarkers = []

const bindDialogVisible = ref(false)
const bindGridIds = ref([])
const bindSubmitting = ref(false)

const AREA_COLORS = [
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#B37FEB', '#36CFC9', '#FF85C0'
]

const filteredCollectors = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return collectors.value
  return collectors.value.filter((c) => {
    const name = collectorLabel(c).toLowerCase()
    const user = (c.username || '').toLowerCase()
    return name.includes(kw) || user.includes(kw)
  })
})

const selectedCollector = computed(() =>
  collectors.value.find((c) => c.id === selectedCollectorId.value) || null
)

const selectedCollectorGrids = computed(() => {
  const ids = selectedCollector.value?.respGridIds || []
  return grids.value.filter((g) => ids.includes(g.id))
})

/** 仅展示当前选中采集员上报的案件（reporterId 匹配） */
const visibleCases = computed(() => {
  if (!showCases.value || !selectedCollectorId.value) {
    return []
  }
  const cid = selectedCollectorId.value
  return cases.value.filter(
    (c) => c.reporterId != null && String(c.reporterId) === String(cid)
  )
})

const visibleCaseCount = computed(() => visibleCases.value.length)

function collectorLabel(c) {
  if (!c) return ''
  return (c.realName && String(c.realName).trim()) || c.username || `用户${c.id}`
}

function gridCount(c) {
  return (c.respGridIds && c.respGridIds.length) || 0
}

function selectCollector(c) {
  selectedCollectorId.value = c?.id ?? null
  renderMapLayers()
}

function focusGrid(g) {
  if (!map || !g?.id) return
  const poly = polygons.find((p) => p.getExtData()?.gridId === g.id)
  if (poly) {
    map.setFitView([poly], false, [80, 80, 80, 380])
  }
}

async function reload() {
  loading.value = true
  try {
    const res = await getCollectorMapOverview({
      caseDays: caseDays.value,
      caseLimit: 300
    })
    const data = res.data || {}
    collectors.value = data.collectors || []
    grids.value = data.grids || []
    cases.value = data.cases || []
    if (selectedCollectorId.value && !collectors.value.some((c) => c.id === selectedCollectorId.value)) {
      selectedCollectorId.value = null
    }
    await nextTick()
    renderMapLayers()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function initMap() {
  if (!mapContainer.value || map) return

  const ready = await waitForGlobalAmap()
  if (!ready) {
    ElMessage.warning('高德地图加载失败，请刷新页面或检查网络')
    return
  }
  const sized = await waitForElementSize(mapContainer.value)
  if (!sized) return

  map = new window.AMap.Map(mapContainer.value, {
    zoom: 12,
    center: [111.0, 35.03],
    viewMode: '2D'
  })
}

function clearMapOverlays() {
  if (!map) return
  polygons.forEach((p) => map.remove(p))
  polygons = []
  caseMarkers.forEach((m) => map.remove(m))
  caseMarkers = []
  collectorMarkers.forEach((m) => map.remove(m))
  collectorMarkers = []
}

function renderMapLayers() {
  if (!map) return
  clearMapOverlays()

  const highlightIds = new Set(selectedCollector.value?.respGridIds || [])

  grids.value.forEach((area, index) => {
    if (!area.boundary) return
    try {
      const geometry = typeof area.boundary === 'string' ? JSON.parse(area.boundary) : area.boundary
      if (geometry.type !== 'Polygon') return
      const path = geometry.coordinates[0].map((c) => [c[0], c[1]])
      const isHighlight = !selectedCollectorId.value || highlightIds.has(area.id)
      const color = AREA_COLORS[index % AREA_COLORS.length]
      const polygon = new window.AMap.Polygon({
        extData: { gridId: area.id, gridName: area.respGridName },
        path,
        fillColor: color,
        fillOpacity: isHighlight ? 0.28 : 0.06,
        strokeColor: color,
        strokeWeight: isHighlight ? 3 : 1,
        strokeOpacity: isHighlight ? 0.9 : 0.35
      })
      polygon.on('click', () => {
        const names = (area.collectorUserIds || [])
          .map((uid) => {
            const c = collectors.value.find((x) => x.id === uid)
            return c ? collectorLabel(c) : `用户${uid}`
          })
          .join('、') || '未分配'
        new window.AMap.InfoWindow({
          content: `<div style="padding:4px 0"><b>${area.respGridName}</b><br/>采集员：${names}</div>`
        }).open(map, polygon.getBounds().getCenter())
      })
      map.add(polygon)
      polygons.push(polygon)
    } catch (e) {
      console.warn('渲染片区失败', area.respGridName, e)
    }
  })

  if (showCases.value && selectedCollectorId.value) {
    visibleCases.value.forEach((item) => {
      const marker = new window.AMap.Marker({
        position: [item.longitude, item.latitude],
        title: item.caseCode,
        extData: item,
        anchor: 'bottom-center'
      })
      marker.on('click', () => {
        const status = formatCaseStatusLabel(item.caseStatus)
        const grid = item.respGridName ? `<br/>片区：${item.respGridName}` : ''
        new window.AMap.InfoWindow({
          content: `<div style="padding:4px 0"><b>${item.caseCode}</b><br/>${status}${grid}<br/><a href="javascript:;" id="case-link-${item.id}">查看详情</a></div>`
        }).open(map, marker.getPosition())
        setTimeout(() => {
          const el = document.getElementById(`case-link-${item.id}`)
          if (el) {
            el.onclick = () => router.push(`/case/detail/${item.id}`)
          }
        }, 50)
      })
      map.add(marker)
      caseMarkers.push(marker)
    })
  }

  if (selectedCollector.value) {
    selectedCollectorGrids.value.forEach((g, i) => {
      if (g.centerLng == null || g.centerLat == null) return
      const marker = new window.AMap.Marker({
        position: [Number(g.centerLng), Number(g.centerLat)],
        content: `<div class="collector-pin">${i + 1}</div>`,
        offset: new window.AMap.Pixel(-12, -12),
        zIndex: 200
      })
      map.add(marker)
      collectorMarkers.push(marker)
    })
  }

  const fitTargets = [...polygons, ...caseMarkers]
  if (fitTargets.length) {
    map.setFitView(fitTargets, false, [60, 60, 60, 360])
  }
}

function openGridBindDialog() {
  if (!selectedCollector.value) return
  bindGridIds.value = [...(selectedCollector.value.respGridIds || [])]
  bindDialogVisible.value = true
}

async function submitGridBind() {
  if (!selectedCollector.value) return
  const collectorId = selectedCollector.value.id
  const oldIds = new Set(selectedCollector.value.respGridIds || [])
  const newIds = new Set(bindGridIds.value || [])
  const affected = new Set([...oldIds, ...newIds])
  if (affected.size === 0) {
    bindDialogVisible.value = false
    return
  }
  bindSubmitting.value = true
  try {
    for (const grid of grids.value) {
      if (!affected.has(grid.id)) continue
      const current = new Set(grid.collectorUserIds || [])
      if (newIds.has(grid.id)) {
        current.add(collectorId)
      } else {
        current.delete(collectorId)
      }
      await setRespGridCollectors(grid.id, [...current])
    }
    ElMessage.success('片区绑定已更新')
    bindDialogVisible.value = false
    await reload()
    selectedCollectorId.value = collectorId
  } catch (e) {
    console.error(e)
  } finally {
    bindSubmitting.value = false
  }
}

onMounted(async () => {
  await nextTick()
  await initMap()
  await reload()
})

watch(showCases, () => {
  renderMapLayers()
})

watch(selectedCollectorId, () => {
  renderMapLayers()
})

onBeforeUnmount(() => {
  if (map) {
    map.destroy()
    map = null
  }
})
</script>

<style scoped>
.collector-manage {
  height: calc(100vh - 120px);
  min-height: 520px;
}

.collector-layout {
  display: flex;
  gap: 12px;
  height: 100%;
}

.collector-sidebar {
  width: 320px;
  flex-shrink: 0;
}

.sidebar-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.sidebar-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-input {
  margin-bottom: 10px;
}

.map-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  gap: 8px;
}

.case-hint {
  margin: 0 0 10px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.collector-list {
  flex: 1;
  overflow-y: auto;
  min-height: 120px;
}

.collector-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid transparent;
  margin-bottom: 6px;
}

.collector-item:hover {
  background: #f5f7fa;
}

.collector-item.active {
  background: #ecf5ff;
  border-color: #b3d8ff;
}

.collector-name {
  font-weight: 600;
  color: #303133;
}

.collector-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.collector-detail {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.detail-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.grid-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.grid-tag {
  cursor: pointer;
}

.bind-btn {
  width: 100%;
}

.bind-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: #606266;
}

.map-panel {
  flex: 1;
  position: relative;
  min-width: 0;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.map-toolbar {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.92);
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  display: flex;
  gap: 14px;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.legend-hint {
  color: #409eff;
}

.dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

.dot.grid {
  background: #409eff;
  opacity: 0.5;
}

.dot.case {
  background: #f56c6c;
  border-radius: 50%;
}

.amap-container {
  width: 100%;
  height: 100%;
}

:deep(.collector-pin) {
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background: #e6a23c;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
}
</style>

<template>
  <div class="case-location-map">
    <div v-if="hasCoords" ref="mapBoxRef" class="amap-box" />
    <van-notice-bar
      v-else-if="!amapKeyConfigured"
      wrapable
      left-icon="info-o"
      color="#1989fa"
      background="#ecf9ff"
      text="未配置高德 Key，无法显示地图（请在 .env.local 配置 VITE_AMAP_KEY）"
    />
    <van-empty v-else description="暂无坐标，无法展示地图" image-size="60" />

    <div v-if="hasCoords" class="map-footer">
      <div class="coord-text">经纬度：{{ coordText }}</div>
      <van-button size="small" type="primary" plain icon="guide-o" @click="openNavigation">
        高德导航
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { showToast } from 'vant'
import { loadAmapScript, DEFAULT_MAP_CENTER, waitForElementSize } from '@/utils/amapLoader'

const props = defineProps({
  longitude: { type: [Number, String], default: null },
  latitude: { type: [Number, String], default: null },
  address: { type: String, default: '' }
})

const mapBoxRef = ref(null)
let mapInst = null
let markerInst = null
let initRetryTimer = null
let initRetryCount = 0
const MAX_INIT_RETRY = 8

const amapKeyConfigured = computed(() => Boolean(import.meta.env.VITE_AMAP_KEY))

const lng = computed(() => {
  const v = Number(props.longitude)
  return Number.isFinite(v) ? v : null
})

const lat = computed(() => {
  const v = Number(props.latitude)
  return Number.isFinite(v) ? v : null
})

const hasCoords = computed(() => lng.value != null && lat.value != null)

const coordText = computed(() => {
  if (!hasCoords.value) return '—'
  return `${lng.value.toFixed(6)}, ${lat.value.toFixed(6)}`
})

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
  if (mapBoxRef.value) {
    mapBoxRef.value.innerHTML = ''
  }
}

async function initMap() {
  if (!hasCoords.value || !amapKeyConfigured.value) {
    destroyMap()
    return
  }
  await nextTick()
  const el = mapBoxRef.value
  if (!el) return
  const sized = await waitForElementSize(el)
  if (!sized) {
    if (initRetryCount < MAX_INIT_RETRY) {
      initRetryCount += 1
      clearTimeout(initRetryTimer)
      initRetryTimer = window.setTimeout(() => initMap(), 120)
    }
    return
  }
  initRetryCount = 0

  let AMap
  try {
    AMap = await loadAmapScript()
  } catch (e) {
    showToast(e?.message || '地图加载失败')
    return
  }

  destroyMap()
  const center = [lng.value, lat.value]

  mapInst = new AMap.Map(el, {
    zoom: 16,
    center,
    dragEnable: true,
    zoomEnable: true
  })
  mapInst.addControl(new AMap.Scale())
  markerInst = new AMap.Marker({
    position: center,
    title: props.address || '案件位置'
  })
  mapInst.add(markerInst)
}

function openNavigation() {
  if (!hasCoords.value) return
  const name = encodeURIComponent(props.address || '案件位置')
  const url = `https://uri.amap.com/marker?position=${lng.value},${lat.value}&name=${name}&callnative=1`
  window.open(url, '_blank')
}

watch(
  () => [lng.value, lat.value],
  () => {
    if (hasCoords.value) {
      initMap()
    } else {
      destroyMap()
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  clearTimeout(initRetryTimer)
  destroyMap()
})
</script>

<style scoped>
.case-location-map {
  padding: 0 12px 12px;
}

.amap-box {
  width: 100%;
  height: 220px;
  border-radius: 8px;
  overflow: hidden;
  background: #e8ecf0;
}

:deep(.amap-container) {
  z-index: 0 !important;
}

.map-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
}

.coord-text {
  flex: 1;
  font-size: 12px;
  color: #646566;
  word-break: break-all;
}
</style>

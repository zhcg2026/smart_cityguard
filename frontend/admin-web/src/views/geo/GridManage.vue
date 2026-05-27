<template>
  <div class="grid-manage">
    <!-- 左右布局：片区列表 + 地图 -->
    <div class="grid-layout">
      <!-- 左侧：片区列表 -->
      <div class="grid-sidebar">
        <el-card class="sidebar-card">
          <template #header>
            <div class="header-with-action">
              <span>片区列表</span>
              <div>
                <el-upload
                  :show-file-list="false"
                  accept=".geojson,.json"
                  :before-upload="handleImportGeoJson"
                >
                  <el-button type="success" size="small">导入</el-button>
                </el-upload>
              </div>
            </div>
          </template>

          <div v-loading="listLoading" class="area-list">
            <div
              v-for="item in areaList"
              :key="item.id"
              class="area-item"
              :class="{ active: selectedArea?.id === item.id }"
              @click="handleSelectArea(item)"
            >
              <div class="area-info">
                <div class="area-name">{{ item.respGridName }}</div>
                <div class="area-code">{{ item.respGridCode }}</div>
                <div v-if="collectorCount(item)" class="area-collector">
                  <el-tag
                    v-for="uid in collectorIds(item)"
                    :key="uid"
                    size="small"
                    type="info"
                    class="collector-tag"
                  >{{ collectorShortLabel(uid) }}</el-tag>
                </div>
                <div v-else class="area-collector">
                  <el-tag size="small" type="warning">未分配采集员</el-tag>
                </div>
              </div>
              <div class="area-actions">
                <el-button type="primary" size="small" link @click.stop="handleEdit(item)">编辑</el-button>
                <el-button
                  type="warning"
                  size="small"
                  link
                  @click.stop="handleAssignCollector(item)"
                >分配</el-button>
                <el-button
                  v-if="collectorCount(item)"
                  type="info"
                  size="small"
                  link
                  @click.stop="handleClearCollectors(item)"
                >清空采集员</el-button>
                <el-button type="danger" size="small" link @click.stop="handleDelete(item)">删除</el-button>
              </div>
            </div>
            <el-empty v-if="!listLoading && areaList.length === 0" description="暂无片区数据" />
          </div>
        </el-card>
      </div>

      <!-- 右侧：地图 -->
      <div class="grid-map-container">
        <div ref="mapContainer" class="amap-container"></div>
      </div>
    </div>

    <!-- 新建/编辑片区对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="areaForm" :rules="areaRules" label-width="90px">
        <el-form-item label="片区名称" prop="respGridName">
          <el-input v-model="areaForm.respGridName" placeholder="如：东片区、西片区" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="GeoJSON文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".geojson,.json"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
          >
            <el-button type="primary" size="small">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">选择 QGIS 导出的 GeoJSON 文件</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item v-if="isEdit && areaForm.boundary" label="当前边界">
          <el-tag type="success">已有边界数据</el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配采集员对话框 -->
    <el-dialog v-model="assignDialogVisible" title="分配采集员" width="480px">
      <el-form label-width="80px">
        <el-form-item label="片区">
          <el-input :model-value="assignArea?.respGridName" disabled />
        </el-form-item>
        <el-form-item label="采集员">
          <el-select
            v-model="assignUserIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="可多选采集员"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="u in collectorList"
              :key="u.id"
              :label="collectorLabel(u)"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="handleAssignSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRespGridList,
  createRespGrid,
  updateRespGrid,
  deleteRespGrid,
  importRespGridGeoJson,
  setRespGridCollectors
} from '@/api/geo'
import { getUserList } from '@/api/system'
import { RoleCode } from '@/utils/roleAccess'

// ==================== 数据 ====================
const listLoading = ref(false)
const areaList = ref([])
const selectedArea = ref(null)

// 地图相关
const mapContainer = ref(null)
let map = null
let polygons = [] // 地图上的多边形图层

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const areaForm = ref({
  id: null,
  respGridName: '',
  boundary: null
})
const areaRules = {
  respGridName: [{ required: true, message: '请输入片区名称', trigger: 'blur' }]
}

// 文件上传
const uploadRef = ref(null)
let selectedFile = null

// 分配采集员
const assignDialogVisible = ref(false)
const assignArea = ref(null)
const assignUserIds = ref([])
const collectorList = ref([])
const assignLoading = ref(false)

// ==================== 初始化 ====================
onMounted(async () => {
  await loadAreaList()
  loadCollectorList()
  await nextTick()
  initMap()
})

onBeforeUnmount(() => {
  if (map) {
    map.destroy()
    map = null
  }
})

// ==================== 地图 ====================
function initMap() {
  if (!mapContainer.value) return

  // 运城市中心点：约 111.0, 35.03
  map = new AMap.Map(mapContainer.value, {
    zoom: 12,
    center: [111.0, 35.03],
    viewMode: '2D'
  })

  renderPolygons()
}

function renderPolygons() {
  if (!map) return

  // 清除旧图层
  polygons.forEach(p => map.remove(p))
  polygons = []

  // 运城市区域颜色
  const colors = [
    '#409EFF', // 蓝
    '#67C23A', // 绿
    '#E6A23C', // 橙
    '#F56C6C', // 红
    '#909399', // 灰
    '#B37FEB', // 紫
    '#36CFC9', // 青
    '#FF85C0'  // 粉
  ]

  areaList.value.forEach((area, index) => {
    if (!area.boundary) return

    try {
      const geometry = typeof area.boundary === 'string' ? JSON.parse(area.boundary) : area.boundary
      if (geometry.type !== 'Polygon') return

      // GeoJSON 坐标是 [lng, lat]，高德也是 [lng, lat]
      const path = geometry.coordinates[0].map(c => [c[0], c[1]])

      const color = colors[index % colors.length]

      const polygon = new AMap.Polygon({
        extData: { areaId: area.id },
        path,
        fillColor: color,
        fillOpacity: 0.2,
        strokeColor: color,
        strokeWeight: 2,
        strokeOpacity: 0.8,
        cursor: 'pointer'
      })

      // 点击多边形选中
      polygon.on('click', () => {
        handleSelectArea(area)
      })

      // 悬停显示名称
      polygon.on('mouseover', () => {
        polygon.setOptions({ fillOpacity: 0.35 })
      })
      polygon.on('mouseout', () => {
        const aid = polygon.getExtData()?.areaId
        polygon.setOptions({
          fillOpacity: selectedArea.value?.id === aid ? 0.35 : 0.2
        })
      })

      map.add(polygon)
      polygons.push(polygon)
    } catch (e) {
      console.warn('渲染片区失败:', area.respGridName, e)
    }
  })

  // 自动调整视口
  if (polygons.length > 0) {
    map.setFitView(polygons, false, [50, 50, 50, 50])
  }
}

// ==================== 数据加载 ====================
async function loadAreaList() {
  listLoading.value = true
  try {
    const res = await getRespGridList()
    areaList.value = res.data || []
  } catch (e) {
    console.error('加载片区列表失败:', e)
  } finally {
    listLoading.value = false
  }
}

async function loadCollectorList() {
  try {
    const res = await getUserList({
      pageNum: 1,
      pageSize: 200,
      roleCode: RoleCode.COLLECTOR,
      status: 1
    })
    collectorList.value = res.data?.records || []
  } catch (e) {
    console.error('加载采集员列表失败:', e)
  }
}

/** 用户列表字段为 username / realName（与 SysUser 一致） */
function collectorLabel(u) {
  if (!u) return ''
  const name = (u.realName && String(u.realName).trim()) || u.username || `用户${u.id}`
  const login = (u.username && String(u.username).trim()) || ''
  return login ? `${name} (${login})` : name
}

function collectorIds(item) {
  const fromApi = item.collectorUserIds
  if (Array.isArray(fromApi) && fromApi.length) return fromApi
  if (item.userId) return [item.userId]
  return []
}

function collectorCount(item) {
  return collectorIds(item).length
}

function collectorShortLabel(uid) {
  const u = collectorList.value.find((x) => x.id === uid)
  if (u) return collectorLabel(u)
  return `用户#${uid}`
}

// ==================== 操作 ====================
function handleSelectArea(area) {
  selectedArea.value = area

  // 高亮对应多边形（用 extData 关联片区，避免与列表下标错位）
  polygons.forEach((p) => {
    const aid = p.getExtData()?.areaId
    if (aid === area.id) {
      p.setOptions({ fillOpacity: 0.35, strokeWeight: 3 })
    } else {
      p.setOptions({ fillOpacity: 0.2, strokeWeight: 2 })
    }
  })

  // 定位到该片区
  if (area.centerLng && area.centerLat) {
    map.setCenter([area.centerLng, area.centerLat])
    map.setZoom(14)
  }
}

function handleEdit(area) {
  isEdit.value = true
  dialogTitle.value = '编辑片区'
  areaForm.value = {
    id: area.id,
    respGridName: area.respGridName,
    boundary: area.boundary
  }
  dialogVisible.value = true
}

function handleFileChange(file) {
  selectedFile = file.raw
}

function handleExceed() {
  ElMessage.warning('只能选择一个文件')
}

async function handleSubmit() {
  // 校验表单
  if (!areaForm.value.respGridName) {
    ElMessage.warning('请输入片区名称')
    return
  }

  submitLoading.value = true
  try {
    if (isEdit.value) {
      // 编辑：只更新名称
      await updateRespGrid(areaForm.value.id, {
        respGridName: areaForm.value.respGridName
      })
      ElMessage.success('更新成功')
    } else {
      // 新建：先导入 GeoJSON 文件，再更新名称
      if (selectedFile) {
        const importRes = await importRespGridGeoJson(selectedFile)
        const data = importRes.data
        if (data.success > 0) {
          // 如果用户输入了名称，更新第一个导入的片区名称
          // （这里简化处理：如果只导入了一个，直接更新名称）
          if (data.success === 1) {
            ElMessage.success('导入成功')
          } else {
            ElMessage.success(`成功导入 ${data.success} 个片区`)
          }
        } else {
          ElMessage.warning('导入失败：' + (data.errors?.join('; ') || '未知错误'))
          return
        }
      } else {
        ElMessage.warning('请选择 GeoJSON 文件')
        return
      }
    }

    dialogVisible.value = false
    selectedFile = null
    if (uploadRef.value) uploadRef.value.clearFiles()

    await loadAreaList()
    await nextTick()
    renderPolygons()
  } catch (e) {
    ElMessage.error('操作失败: ' + (e.response?.data?.message || e.message))
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(area) {
  try {
    await ElMessageBox.confirm(`确定删除片区「${area.respGridName}」吗？`, '确认删除', {
      type: 'warning'
    })
    await deleteRespGrid(area.id)
    ElMessage.success('删除成功')
    if (selectedArea.value?.id === area.id) selectedArea.value = null
    await loadAreaList()
    await nextTick()
    renderPolygons()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败: ' + (e.response?.data?.message || e.message))
    }
  }
}

async function handleImportGeoJson(file) {
  // 校验文件类型
  const isValid = file.name.endsWith('.geojson') || file.name.endsWith('.json')
  if (!isValid) {
    ElMessage.error('请选择 GeoJSON 文件（.geojson 或 .json）')
    return false
  }

  try {
    const res = await importRespGridGeoJson(file)
    const data = res.data
    if (data.success > 0) {
      ElMessage.success(`成功导入 ${data.success} 个片区`)
    } else {
      ElMessage.warning('导入失败')
    }
    if (data.errors?.length > 0) {
      ElMessage.warning(data.errors.join('\n'))
    }
    await loadAreaList()
    await nextTick()
    renderPolygons()
  } catch (e) {
    ElMessage.error('导入失败: ' + (e.response?.data?.message || e.message))
  }
  return false // 阻止 el-upload 自动上传
}

function handleAssignCollector(area) {
  assignArea.value = area
  assignUserIds.value = [...collectorIds(area)]
  assignDialogVisible.value = true
  loadCollectorList()
}

async function handleAssignSubmit() {
  assignLoading.value = true
  try {
    await setRespGridCollectors(assignArea.value.id, [...(assignUserIds.value || [])])
    ElMessage.success('保存成功')
    assignDialogVisible.value = false
    await loadAreaList()
    await nextTick()
    renderPolygons()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message))
  } finally {
    assignLoading.value = false
  }
}

async function handleClearCollectors(area) {
  try {
    await ElMessageBox.confirm(`确定清空片区「${area.respGridName}」的全部采集员吗？`, '确认清空', {
      type: 'warning'
    })
    await setRespGridCollectors(area.id, [])
    ElMessage.success('已清空')
    await loadAreaList()
    await nextTick()
    renderPolygons()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败: ' + (e.response?.data?.message || e.message))
    }
  }
}
</script>

<style scoped lang="scss">
.grid-manage {
  height: calc(100vh - 84px);
  padding: 12px;
  box-sizing: border-box;
}

.grid-layout {
  display: flex;
  height: 100%;
  gap: 12px;
}

.grid-sidebar {
  width: 360px;
  flex-shrink: 0;

  .sidebar-card {
    height: 100%;

    :deep(.el-card__body) {
      padding: 12px;
      height: calc(100% - 56px);
      overflow-y: auto;
    }
  }
}

.header-with-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.area-list {
  .area-item {
    padding: 12px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    margin-bottom: 8px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: #409eff;
      background: #f5f7fa;
    }

    &.active {
      border-color: #409eff;
      background: #ecf5ff;
    }

    .area-info {
      .area-name {
        font-size: 15px;
        font-weight: 600;
        color: #303133;
        margin-bottom: 4px;
      }

      .area-code {
        font-size: 12px;
        color: #909399;
        margin-bottom: 4px;
      }

      .area-collector {
        margin-top: 4px;
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
      }

      .collector-tag {
        max-width: 100%;
      }
    }

    .area-actions {
      margin-top: 8px;
      display: flex;
      gap: 4px;
      flex-wrap: wrap;
    }
  }
}

.grid-map-container {
  flex: 1;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ebeef5;

  .amap-container {
    width: 100%;
    height: 100%;
  }
}
</style>
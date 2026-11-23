<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">操作日志</h2>
      <span class="page-tip">仅记录会修改数据的操作</span>
      <el-button @click="exportLogs">导出</el-button>
      <el-button v-permission="'logs:clean'" type="danger" plain @click="openCleanDialog">清理日志</el-button>
    </div>

    <el-dialog title="清理历史日志" v-model="cleanDialogVisible" width="460px">
      <el-form label-width="90px">
        <el-form-item label="清理截止">
          <el-date-picker
            v-model="cleanDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            :disabled-date="disableFutureDate"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <el-alert
        title="将删除所选日期及之前的日志，删除后不可恢复。"
        type="warning"
        :closable="false"
        show-icon
      />
      <template #footer>
        <el-button @click="cleanDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="cleaning" @click="cleanLogs">确认清理</el-button>
      </template>
    </el-dialog>

    <div class="card">
      <div class="search-bar">
        <el-select v-model="searchForm.module" placeholder="全部模块" clearable class="module-select">
          <el-option label="用户管理" value="用户管理" />
          <el-option label="角色管理" value="角色管理" />
          <el-option label="系统设置" value="系统设置" />
        </el-select>
        <el-input
          v-model="searchForm.keyword"
          placeholder="操作人、动作或路径"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          class="time-range"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="logList" border stripe>
        <el-table-column type="expand">
          <template #default="scope">
            <div class="log-detail">
              <div class="detail-row">
                <span class="detail-label">请求参数</span>
                <pre>{{ scope.row.params || '无' }}</pre>
              </div>
              <div v-if="scope.row.errorMsg" class="detail-row">
                <span class="detail-label">错误信息</span>
                <span class="error-text">{{ scope.row.errorMsg }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="170" />
        <el-table-column prop="username" label="操作人" width="110" />
        <el-table-column prop="module" label="模块" width="110" />
        <el-table-column prop="action" label="动作" width="130" />
        <el-table-column prop="method" label="方法" width="80" />
        <el-table-column prop="uri" label="请求路径" min-width="160" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="耗时" width="90">
          <template #default="scope">
            {{ scope.row.durationMs }} ms
          </template>
        </el-table-column>
        <el-table-column label="结果" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.success === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          :current-page="pagination.page"
          :page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getLogsApi, exportLogsApi, cleanLogsApi } from '@/api/log'

const logList = ref([])
const timeRange = ref([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const searchForm = reactive({
  module: '',
  keyword: ''
})

onMounted(() => {
  loadLogs()
})

async function loadLogs() {
  try {
    const res = await getLogsApi({
      page: pagination.page,
      size: pagination.size,
      module: searchForm.module || undefined,
      keyword: searchForm.keyword || undefined,
      startTime: timeRange.value?.[0],
      endTime: timeRange.value?.[1]
    })
    logList.value = res.data.list
    pagination.total = res.data.total
  } catch (error) {
    ElMessage.error('日志加载失败')
  }
}

function handleSearch() {
  pagination.page = 1
  loadLogs()
}

function resetSearch() {
  searchForm.module = ''
  searchForm.keyword = ''
  timeRange.value = []
  pagination.page = 1
  loadLogs()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadLogs()
}

function handleCurrentChange(page) {
  pagination.page = page
  loadLogs()
}

const cleanDialogVisible = ref(false)
const cleanDate = ref('')
const cleaning = ref(false)

function openCleanDialog() {
  cleanDate.value = ''
  cleanDialogVisible.value = true
}

function disableFutureDate(date) {
  return date.getTime() > Date.now()
}

async function cleanLogs() {
  if (!cleanDate.value) {
    ElMessage.warning('请选择清理截止日期')
    return
  }

  cleaning.value = true
  try {
    const res = await cleanLogsApi(`${cleanDate.value} 23:59:59`)
    ElMessage.success(`已清理 ${res.data} 条日志`)
    cleanDialogVisible.value = false
    pagination.page = 1
    loadLogs()
  } catch (error) {
    // 拦截器已提示
  } finally {
    cleaning.value = false
  }
}

async function exportLogs() {
  try {
    const response = await exportLogsApi({
      module: searchForm.module || undefined,
      keyword: searchForm.keyword || undefined,
      startTime: timeRange.value?.[0],
      endTime: timeRange.value?.[1]
    })
    saveBlob(response, 'operation-logs.xlsx')
  } catch (error) {
    // 拦截器已提示
  }
}

function saveBlob(response, fallbackName) {
  const disposition = response.headers['content-disposition'] || ''
  const match = disposition.match(/filename="?([^";]+)"?/)
  const filename = match ? decodeURIComponent(match[1]) : fallbackName
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.page-header {
  justify-content: flex-start;
}

.page-tip {
  margin-right: auto;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.module-select {
  width: 150px;
}

.keyword-input {
  width: 220px;
}

.time-range {
  width: 360px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.log-detail {
  padding: 8px 48px;
}

.detail-row {
  margin-bottom: 8px;
}

.detail-label {
  display: block;
  font-weight: 600;
  color: var(--app-text-regular);
  margin-bottom: 4px;
}

.log-detail pre {
  margin: 0;
  padding: 10px 12px;
  background: var(--app-hover-bg);
  border-radius: 4px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.error-text {
  color: #f56c6c;
}
</style>

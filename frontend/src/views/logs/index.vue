<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('logs.title') }}</h2>
      <span class="page-tip">{{ t('logs.tip') }}</span>
      <el-button @click="exportLogs">{{ t('logs.export') }}</el-button>
      <el-button v-permission="'logs:clean'" type="danger" plain @click="openCleanDialog">{{ t('logs.clean') }}</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <p class="stat-label">{{ t('logs.stats.todayTotal') }}</p>
        <p class="stat-value">{{ stats.todayTotal }}</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">{{ t('logs.stats.todayFailed') }}</p>
        <p class="stat-value is-danger">{{ stats.todayFailed }}</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">{{ t('logs.stats.failureRate') }}</p>
        <p class="stat-value">{{ stats.failureRate }}%</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">{{ t('logs.stats.slowModules') }}</p>
        <ul v-if="stats.slowModules.length" class="slow-list">
          <li v-for="(item, index) in stats.slowModules" :key="item.module">
            <span class="slow-rank">{{ index + 1 }}</span>
            <span class="slow-name">{{ item.module }}</span>
            <span class="slow-value">{{ item.avgDurationMs }} ms</span>
          </li>
        </ul>
        <p v-else class="slow-empty">{{ t('logs.stats.noData') }}</p>
      </div>
    </div>

    <el-dialog :title="t('logs.cleanTitle')" v-model="cleanDialogVisible" width="460px">
      <el-form label-width="90px">
        <el-form-item :label="t('logs.cleanDate')">
          <el-date-picker
            v-model="cleanDate"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="t('logs.cleanDatePlaceholder')"
            :disabled-date="disableFutureDate"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <el-alert
        :title="t('logs.cleanAlert')"
        type="warning"
        :closable="false"
        show-icon
      />
      <template #footer>
        <el-button @click="cleanDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="danger" :loading="cleaning" @click="cleanLogs">{{ t('logs.cleanConfirm') }}</el-button>
      </template>
    </el-dialog>

    <div class="card">
      <div class="search-bar">
        <el-select v-model="searchForm.module" :placeholder="t('logs.modulePlaceholder')" clearable class="module-select">
          <el-option v-for="item in moduleOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input
          v-model="searchForm.keyword"
          :placeholder="t('logs.keywordPlaceholder')"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          :start-placeholder="t('logs.startTime')"
          :end-placeholder="t('logs.endTime')"
          value-format="YYYY-MM-DD HH:mm:ss"
          class="time-range"
        />
        <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
        <el-button @click="resetSearch">{{ t('common.reset') }}</el-button>
      </div>

      <el-table :data="logList" border stripe>
        <el-table-column type="expand">
          <template #default="scope">
            <div class="log-detail">
              <div class="detail-row">
                <span class="detail-label">{{ t('logs.params') }}</span>
                <pre>{{ scope.row.params || t('logs.none') }}</pre>
              </div>
              <div v-if="scope.row.errorMsg" class="detail-row">
                <span class="detail-label">{{ t('logs.errorMsg') }}</span>
                <span class="error-text">{{ scope.row.errorMsg }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('logs.columns.createTime')" width="170" />
        <el-table-column prop="username" :label="t('logs.columns.username')" width="110" />
        <el-table-column prop="module" :label="t('logs.columns.module')" width="110" />
        <el-table-column prop="action" :label="t('logs.columns.action')" width="130" />
        <el-table-column prop="method" :label="t('logs.columns.method')" width="80" />
        <el-table-column prop="uri" :label="t('logs.columns.uri')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="ip" :label="t('logs.columns.ip')" width="130" />
        <el-table-column :label="t('logs.columns.duration')" width="90">
          <template #default="scope">
            {{ scope.row.durationMs }} ms
          </template>
        </el-table-column>
        <el-table-column :label="t('logs.columns.result')" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.success === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.success === 1 ? t('logs.success') : t('logs.failed') }}
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getLogsApi, getLogStatsApi, exportLogsApi, cleanLogsApi } from '@/api/log'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

// 模块值需要与后端记录保持一致，仅切换展示文案
const moduleOptions = computed(() => [
  { value: '用户管理', label: t('logs.moduleUsers') },
  { value: '角色管理', label: t('logs.moduleRoles') },
  { value: '系统设置', label: t('logs.moduleSettings') }
])

const logList = ref([])
const stats = ref({
  todayTotal: 0,
  todaySuccess: 0,
  todayFailed: 0,
  failureRate: 0,
  slowModules: []
})
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
  loadStats()
})

async function loadStats() {
  try {
    const res = await getLogStatsApi()
    stats.value = res.data
  } catch (error) {
    // 拦截器已提示
  }
}

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
    ElMessage.error(t('common.requestFailed'))
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
    ElMessage.warning(t('logs.cleanNoDate'))
    return
  }

  cleaning.value = true
  try {
    const res = await cleanLogsApi(`${cleanDate.value} 23:59:59`)
    ElMessage.success(t('logs.cleanSuccess', { count: res.data }))
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

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  padding: 16px 18px;
  border-radius: 10px;
  background: var(--app-bg-container);
  border: 1px solid var(--app-border);
}

.stat-label {
  margin: 0;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.stat-value {
  margin: 10px 0 0;
  font-size: 26px;
  font-weight: 600;
  color: var(--app-text-primary);
}

.stat-value.is-danger {
  color: #f56c6c;
}

.slow-list {
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
}

.slow-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 0;
  font-size: 13px;
  color: var(--app-text-primary);
}

.slow-rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  font-size: 11px;
  color: #fff;
  background: #e6a23c;
}

.slow-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slow-value {
  color: var(--app-text-secondary);
  white-space: nowrap;
}

.slow-empty {
  margin: 12px 0 0;
  font-size: 13px;
  color: var(--app-text-secondary);
}
</style>

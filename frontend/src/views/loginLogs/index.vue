<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('loginLogs.title') }}</h2>
      <span class="page-tip">{{ t('loginLogs.tip') }}</span>
      <el-button @click="exportLogs">{{ t('loginLogs.export') }}</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <p class="stat-label">{{ t('loginLogs.stats.todaySuccess') }}</p>
        <p class="stat-value is-success">{{ stats.todaySuccess }}</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">{{ t('loginLogs.stats.todayFailed') }}</p>
        <p class="stat-value is-danger">{{ stats.todayFailed }}</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">{{ t('loginLogs.stats.failureRate') }}</p>
        <p class="stat-value">{{ stats.failureRate }}%</p>
      </div>
      <div class="stat-card reasons-card">
        <p class="stat-label">{{ t('loginLogs.stats.topReasons') }}</p>
        <ul v-if="stats.topFailReasons.length" class="reason-list">
          <li v-for="(item, index) in stats.topFailReasons" :key="item.reason">
            <span class="reason-rank">{{ index + 1 }}</span>
            <span class="reason-text" :title="item.reason">{{ item.reason }}</span>
            <span class="reason-count">{{ t('loginLogs.stats.times', { count: item.count }) }}</span>
          </li>
        </ul>
        <p v-else class="reason-empty">{{ t('loginLogs.stats.noFailure') }}</p>
      </div>
    </div>

    <div class="card security-card">
      <div class="security-header">
        <div>
          <h3 class="security-title">{{ t('loginLogs.securityTitle') }}</h3>
          <p class="security-tip">{{ t('loginLogs.securityTip', { count: security.maxFailCount }) }}</p>
        </div>
        <el-button size="small" @click="loadSecurity">{{ t('common.refresh') }}</el-button>
      </div>
      <el-table v-if="security.lockedAccounts.length" :data="security.lockedAccounts" border stripe size="small">
        <el-table-column prop="username" :label="t('loginLogs.columns.username')" min-width="140" />
        <el-table-column prop="failures" :label="t('loginLogs.columns.failures')" width="110" />
        <el-table-column :label="t('loginLogs.columns.remaining')" width="150">
          <template #default="scope">{{ formatRemaining(scope.row.remainingSeconds) }}</template>
        </el-table-column>
        <el-table-column :label="t('loginLogs.columns.actions')" width="110">
          <template #default="scope">
            <el-button v-permission="'logs:unlock'" link type="primary" @click="unlock(scope.row)">
              {{ t('loginLogs.unlock') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else :description="t('loginLogs.noLocked')" :image-size="56" />
    </div>

    <div class="card">
      <div class="search-bar">
        <el-input
          v-model="searchForm.username"
          :placeholder="t('loginLogs.usernamePlaceholder')"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          :start-placeholder="t('loginLogs.startTime')"
          :end-placeholder="t('loginLogs.endTime')"
          value-format="YYYY-MM-DD HH:mm:ss"
          class="time-range"
        />
        <el-select
          v-model="searchForm.success"
          clearable
          :placeholder="t('loginLogs.resultPlaceholder')"
          class="result-select"
        >
          <el-option :label="t('loginLogs.success')" :value="1" />
          <el-option :label="t('loginLogs.failed')" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
        <el-button @click="resetSearch">{{ t('common.reset') }}</el-button>
      </div>

      <el-table :data="logList" border stripe>
        <el-table-column prop="id" :label="t('loginLogs.columns.id')" width="80" />
        <el-table-column prop="username" :label="t('loginLogs.columns.username')" width="140" />
        <el-table-column :label="t('loginLogs.columns.result')" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.success === 0 ? 'danger' : 'success'" size="small">
              {{ scope.row.success === 0 ? t('loginLogs.failed') : t('loginLogs.success') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="failReason"
          :label="t('loginLogs.columns.failReason')"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column prop="loginTime" :label="t('loginLogs.columns.loginTime')" width="180" />
        <el-table-column :label="t('loginLogs.columns.logoutTime')" width="180">
          <template #default="scope">
            <span v-if="scope.row.logoutTime">{{ scope.row.logoutTime }}</span>
            <span v-else-if="scope.row.success === 0">—</span>
            <span v-else class="online-text">{{ t('loginLogs.onlineNow') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('loginLogs.columns.duration')">
          <template #default="scope">
            {{ formatDuration(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('loginLogs.columns.status')" width="110">
          <template #default="scope">
            <span v-if="scope.row.success === 0">—</span>
            <el-tag v-else :type="scope.row.logoutTime ? 'info' : 'success'" size="small">
              {{ scope.row.logoutTime ? t('loginLogs.loggedOut') : t('loginLogs.online') }}
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
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getLoginLogsApi,
  exportLoginLogsApi,
  getLoginSecurityApi,
  getLoginLogStatsApi,
  unlockLoginAccountApi
} from '@/api/loginLog'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const logList = ref([])
const security = ref({
  maxFailCount: 5,
  lockedAccounts: []
})
const stats = ref({
  todayTotal: 0,
  todaySuccess: 0,
  todayFailed: 0,
  failureRate: 0,
  topFailReasons: []
})
const timeRange = ref([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const searchForm = reactive({
  username: '',
  success: null
})

onMounted(() => {
  loadLogs()
  loadSecurity()
  loadStats()
})

async function loadLogs() {
  try {
    const res = await getLoginLogsApi({
      page: pagination.page,
      size: pagination.size,
      username: searchForm.username || undefined,
      success: searchForm.success ?? undefined,
      startTime: timeRange.value?.[0],
      endTime: timeRange.value?.[1]
    })
    logList.value = res.data.list
    pagination.total = res.data.total
  } catch (error) {
    // 拦截器已提示
  }
}

async function loadSecurity() {
  try {
    const res = await getLoginSecurityApi()
    security.value = res.data
  } catch (error) {
    // 拦截器已提示
  }
}

async function loadStats() {
  try {
    const res = await getLoginLogStatsApi()
    stats.value = res.data
  } catch (error) {
    // 拦截器已提示
  }
}

async function unlock(row) {
  try {
    await ElMessageBox.confirm(
      t('loginLogs.unlockConfirm', { name: row.username }),
      t('loginLogs.unlock'),
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    const res = await unlockLoginAccountApi(row.username)
    if (res.data > 0) {
      ElMessage.success(t('loginLogs.unlocked'))
    }
    await loadSecurity()
  } catch (error) {
    // 拦截器已提示
  }
}

function formatRemaining(seconds) {
  if (!seconds || seconds <= 0) {
    return t('loginLogs.expired')
  }
  if (seconds < 60) {
    return t('loginLogs.remainingSeconds', { count: seconds })
  }
  return t('loginLogs.remainingMinutes', { count: Math.ceil(seconds / 60) })
}

function handleSearch() {
  pagination.page = 1
  loadLogs()
}

function resetSearch() {
  searchForm.username = ''
  searchForm.success = null
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

async function exportLogs() {
  try {
    const response = await exportLoginLogsApi({
      username: searchForm.username || undefined,
      success: searchForm.success ?? undefined,
      startTime: timeRange.value?.[0],
      endTime: timeRange.value?.[1]
    })
    saveBlob(response, 'login-logs.xlsx')
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

function formatDuration(row) {  if (!row.logoutTime) {
    return '—'
  }
  // 兼容 "yyyy-MM-dd HH:mm:ss"，Safari 下需要把连字符换成斜杠
  const start = new Date(row.loginTime.replace(/-/g, '/'))
  const end = new Date(row.logoutTime.replace(/-/g, '/'))
  const seconds = Math.max(0, Math.floor((end - start) / 1000))
  if (seconds < 60) {
    return t('loginLogs.seconds', { count: seconds })
  }
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) {
    return t('loginLogs.minutes', { count: minutes })
  }
  const hours = Math.floor(minutes / 60)
  return t('loginLogs.hoursMinutes', { hours, minutes: minutes % 60 })
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

.keyword-input {
  width: 220px;
}

.result-select {
  width: 140px;
}

.security-card {
  margin-bottom: 20px;
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

.stat-value.is-success {
  color: #67c23a;
}

.stat-value.is-danger {
  color: #f56c6c;
}

.reason-list {
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
}

.reason-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 0;
  font-size: 13px;
  color: var(--app-text-primary);
}

.reason-rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  font-size: 11px;
  color: #fff;
  background: #f56c6c;
}

.reason-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reason-count {
  color: var(--app-text-secondary);
  white-space: nowrap;
}

.reason-empty {
  margin: 12px 0 0;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.security-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.security-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-primary);
}

.security-tip {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.time-range {
  width: 360px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.online-text {
  color: #67C23A;
}
</style>

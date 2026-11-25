<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('loginLogs.title') }}</h2>
      <span class="page-tip">{{ t('loginLogs.tip') }}</span>
      <el-button @click="exportLogs">{{ t('loginLogs.export') }}</el-button>
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
        <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
        <el-button @click="resetSearch">{{ t('common.reset') }}</el-button>
      </div>

      <el-table :data="logList" border stripe>
        <el-table-column prop="id" :label="t('loginLogs.columns.id')" width="80" />
        <el-table-column prop="username" :label="t('loginLogs.columns.username')" width="140" />
        <el-table-column prop="loginTime" :label="t('loginLogs.columns.loginTime')" width="180" />
        <el-table-column :label="t('loginLogs.columns.logoutTime')" width="180">
          <template #default="scope">
            <span v-if="scope.row.logoutTime">{{ scope.row.logoutTime }}</span>
            <span v-else class="online-text">{{ t('loginLogs.onlineNow') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('loginLogs.columns.duration')">
          <template #default="scope">
            {{ formatDuration(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('loginLogs.columns.status')" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.logoutTime ? 'info' : 'success'" size="small">
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
import { getLoginLogsApi, exportLoginLogsApi } from '@/api/loginLog'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const logList = ref([])
const timeRange = ref([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const searchForm = reactive({
  username: ''
})

onMounted(() => {
  loadLogs()
})

async function loadLogs() {
  try {
    const res = await getLoginLogsApi({
      page: pagination.page,
      size: pagination.size,
      username: searchForm.username || undefined,
      startTime: timeRange.value?.[0],
      endTime: timeRange.value?.[1]
    })
    logList.value = res.data.list
    pagination.total = res.data.total
  } catch (error) {
    // 拦截器已提示
  }
}

function handleSearch() {
  pagination.page = 1
  loadLogs()
}

function resetSearch() {
  searchForm.username = ''
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

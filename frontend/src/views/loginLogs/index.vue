<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">登录日志</h2>
      <span class="page-tip">记录登录与退出时间，未退出显示在线</span>
    </div>

    <div class="card">
      <div class="search-bar">
        <el-input
          v-model="searchForm.username"
          placeholder="登录名"
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
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="登录名" width="140" />
        <el-table-column prop="loginTime" label="登录时间" width="180" />
        <el-table-column label="退出时间" width="180">
          <template #default="scope">
            <span v-if="scope.row.logoutTime">{{ scope.row.logoutTime }}</span>
            <span v-else class="online-text">在线中</span>
          </template>
        </el-table-column>
        <el-table-column label="在线时长">
          <template #default="scope">
            {{ formatDuration(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.logoutTime ? 'info' : 'success'" size="small">
              {{ scope.row.logoutTime ? '已退出' : '在线' }}
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
import { getLoginLogsApi } from '@/api/loginLog'

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

function formatDuration(row) {
  if (!row.logoutTime) {
    return '—'
  }
  // 兼容 "yyyy-MM-dd HH:mm:ss"，Safari 下需要把连字符换成斜杠
  const start = new Date(row.loginTime.replace(/-/g, '/'))
  const end = new Date(row.logoutTime.replace(/-/g, '/'))
  const seconds = Math.max(0, Math.floor((end - start) / 1000))
  if (seconds < 60) {
    return `${seconds}秒`
  }
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) {
    return `${minutes}分钟`
  }
  const hours = Math.floor(minutes / 60)
  return `${hours}小时${minutes % 60}分`
}
</script>

<style scoped>
.page-tip {
  font-size: 13px;
  color: #909399;
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

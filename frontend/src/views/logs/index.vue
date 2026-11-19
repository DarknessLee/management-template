<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">操作日志</h2>
      <span class="page-tip">仅记录会修改数据的操作</span>
    </div>

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
import { getLogsApi } from '@/api/log'

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
  color: #606266;
  margin-bottom: 4px;
}

.log-detail pre {
  margin: 0;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.error-text {
  color: #f56c6c;
}
</style>

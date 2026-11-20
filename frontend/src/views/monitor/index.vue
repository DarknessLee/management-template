<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">系统监控</h2>
      <el-button :loading="loading" @click="loadInfo">
        <el-icon><component :is="icons.Refresh" /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="card" v-loading="loading">
      <el-row :gutter="20">
        <el-col v-for="stat in stats" :key="stat.label" :xs="24" :sm="12" :lg="6">
          <div class="stat-card">
            <div class="stat-label">{{ stat.label }}</div>
            <div class="stat-value">{{ stat.value }}</div>
            <el-progress
              v-if="stat.percent !== null"
              :percentage="stat.percent"
              :status="stat.status"
              :stroke-width="8"
              :show-text="false"
            />
            <div class="stat-extra">{{ stat.extra }}</div>
          </div>
        </el-col>
      </el-row>

      <el-descriptions title="基础信息" :column="2" border class="info-block">
        <el-descriptions-item label="主机名">{{ info.hostName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作系统">
          {{ info.osName || '-' }}（{{ info.osArch || '-' }}）
        </el-descriptions-item>
        <el-descriptions-item label="Java 版本">{{ info.javaVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="CPU 核心数">{{ info.availableProcessors || '-' }} 核</el-descriptions-item>
        <el-descriptions-item label="当前线程数">{{ info.threadCount || '-' }}</el-descriptions-item>
        <el-descriptions-item label="系统负载（1 分钟）">{{ loadAverageText }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getMonitorApi } from '@/api/monitor'

const icons = { Refresh }

const loading = ref(false)
const info = ref({
  heapUsed: 0,
  heapMax: 0,
  cpuLoad: -1,
  systemLoadAverage: -1,
  diskTotal: 0,
  diskFree: 0,
  uptime: 0,
  threadCount: 0,
  availableProcessors: 0,
  javaVersion: '',
  osName: '',
  osArch: '',
  hostName: ''
})

const loadAverageText = computed(() => {
  const value = info.value.systemLoadAverage
  return value >= 0 ? value.toFixed(2) : '当前平台不支持'
})

const heapPercent = computed(() => toPercent(info.value.heapUsed, info.value.heapMax))
const diskUsed = computed(() => Math.max(info.value.diskTotal - info.value.diskFree, 0))
const diskPercent = computed(() => toPercent(diskUsed.value, info.value.diskTotal))

const stats = computed(() => [
  {
    label: 'CPU 使用率',
    value: info.value.cpuLoad >= 0 ? `${info.value.cpuLoad.toFixed(2)}%` : '--',
    percent: info.value.cpuLoad >= 0 ? Math.round(info.value.cpuLoad) : null,
    status: progressStatus(info.value.cpuLoad),
    extra: `${info.value.availableProcessors || 0} 个可用核心`
  },
  {
    label: 'JVM 堆内存',
    value: `${formatBytes(info.value.heapUsed)} / ${formatBytes(info.value.heapMax)}`,
    percent: heapPercent.value,
    status: progressStatus(heapPercent.value),
    extra: `已用 ${heapPercent.value}%`
  },
  {
    label: '磁盘使用',
    value: `${formatBytes(diskUsed.value)} / ${formatBytes(info.value.diskTotal)}`,
    percent: diskPercent.value,
    status: progressStatus(diskPercent.value),
    extra: `可用 ${formatBytes(info.value.diskFree)}`
  },
  {
    label: '运行时长',
    value: formatUptime(info.value.uptime),
    percent: null,
    status: 'success',
    extra: `${info.value.threadCount || 0} 个线程`
  }
])

onMounted(() => {
  loadInfo()
})

async function loadInfo() {
  loading.value = true
  try {
    const res = await getMonitorApi()
    info.value = res.data
  } catch (error) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function toPercent(used, total) {
  if (!total || total <= 0) return 0
  return Math.min(Math.round((used / total) * 100), 100)
}

function progressStatus(percent) {
  if (percent >= 90) return 'exception'
  if (percent >= 70) return 'warning'
  return 'success'
}

function formatBytes(bytes) {
  if (!bytes || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let value = bytes
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index++
  }
  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

function formatUptime(milliseconds) {
  if (!milliseconds || milliseconds <= 0) return '-'
  const totalMinutes = Math.floor(milliseconds / 60000)
  const days = Math.floor(totalMinutes / 1440)
  const hours = Math.floor((totalMinutes % 1440) / 60)
  const minutes = totalMinutes % 60
  const parts = []
  if (days > 0) parts.push(`${days} 天`)
  if (hours > 0) parts.push(`${hours} 小时`)
  parts.push(`${minutes} 分钟`)
  return parts.join(' ')
}
</script>

<style scoped>
.stat-card {
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 20px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.stat-value {
  margin: 10px 0 14px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.stat-extra {
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
}

.info-block {
  margin-top: 8px;
}
</style>

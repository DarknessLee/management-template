<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('monitor.title') }}</h2>
      <el-button :loading="loading" @click="loadInfo">
        <el-icon><component :is="icons.Refresh" /></el-icon>
        {{ t('monitor.refresh') }}
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

      <el-descriptions :title="t('monitor.basicInfo')" :column="2" border class="info-block">
        <el-descriptions-item :label="t('monitor.hostName')">{{ info.hostName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('monitor.os')">
          {{ osText }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('monitor.javaVersion')">{{ info.javaVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('monitor.cpuCores')">{{ t('monitor.cores', { count: info.availableProcessors || '-' }) }}</el-descriptions-item>
        <el-descriptions-item :label="t('monitor.threadCount')">{{ info.threadCount || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('monitor.loadAverage')">{{ loadAverageText }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getMonitorApi } from '@/api/monitor'
import { useSiteStore } from '@/stores/site'
import { useI18n } from 'vue-i18n'

const icons = { Refresh }

const siteStore = useSiteStore()
const { t } = useI18n()

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

const osText = computed(() => {
  const name = info.value.osName || '-'
  const arch = info.value.osArch || '-'
  return siteStore.locale === 'en' ? `${name} (${arch})` : `${name}（${arch}）`
})

const loadAverageText = computed(() => {
  const value = info.value.systemLoadAverage
  return value >= 0 ? value.toFixed(2) : t('monitor.notSupported')
})

const heapPercent = computed(() => toPercent(info.value.heapUsed, info.value.heapMax))
const diskUsed = computed(() => Math.max(info.value.diskTotal - info.value.diskFree, 0))
const diskPercent = computed(() => toPercent(diskUsed.value, info.value.diskTotal))

const stats = computed(() => [
  {
    label: t('monitor.cpuUsage'),
    value: info.value.cpuLoad >= 0 ? `${info.value.cpuLoad.toFixed(2)}%` : '--',
    percent: info.value.cpuLoad >= 0 ? Math.round(info.value.cpuLoad) : null,
    status: progressStatus(info.value.cpuLoad),
    extra: t('monitor.coresAvailable', { count: info.value.availableProcessors || 0 })
  },
  {
    label: t('monitor.heap'),
    value: `${formatBytes(info.value.heapUsed)} / ${formatBytes(info.value.heapMax)}`,
    percent: heapPercent.value,
    status: progressStatus(heapPercent.value),
    extra: t('monitor.usedPercent', { percent: heapPercent.value })
  },
  {
    label: t('monitor.disk'),
    value: `${formatBytes(diskUsed.value)} / ${formatBytes(info.value.diskTotal)}`,
    percent: diskPercent.value,
    status: progressStatus(diskPercent.value),
    extra: t('monitor.free', { size: formatBytes(info.value.diskFree) })
  },
  {
    label: t('monitor.uptime'),
    value: formatUptime(info.value.uptime),
    percent: null,
    status: 'success',
    extra: t('monitor.threads', { count: info.value.threadCount || 0 })
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
  if (days > 0) parts.push(t('monitor.days', { count: days }))
  if (hours > 0) parts.push(t('monitor.hours', { count: hours }))
  parts.push(t('monitor.minutes', { count: minutes }))
  return parts.join(' ')
}
</script>

<style scoped>
.stat-card {
  padding: 20px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  margin-bottom: 20px;
}

.stat-label {
  font-size: 14px;
  color: var(--app-text-secondary);
}

.stat-value {
  margin: 10px 0 14px;
  font-size: 22px;
  font-weight: 600;
  color: var(--app-text-primary);
}

.stat-extra {
  margin-top: 12px;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.info-block {
  margin-top: 8px;
}
</style>

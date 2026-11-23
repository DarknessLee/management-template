<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('jobs.title') }}</h2>
      <el-button v-permission="'job:edit'" type="primary" @click="openAddDialog">
        <el-icon><component :is="icons.Plus" /></el-icon>
        {{ t('jobs.add') }}
      </el-button>
    </div>

    <div class="card">
      <el-table v-loading="loading" :data="jobs" border stripe>
        <el-table-column prop="name" :label="t('jobs.columns.name')" min-width="140" />
        <el-table-column prop="handlerName" :label="t('jobs.columns.handler')" width="150" />
        <el-table-column prop="cronExpression" :label="t('jobs.columns.cron')" width="140" />
        <el-table-column :label="t('jobs.columns.status')" width="90">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              inline-prompt
              :width="56"
              :active-text="statusText(1)"
              :inactive-text="statusText(0)"
              :disabled="!canEdit"
              @change="value => toggleStatus(scope.row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="lastRunTime" :label="t('jobs.columns.lastRun')" width="170">
          <template #default="scope">{{ scope.row.lastRunTime || '—' }}</template>
        </el-table-column>
        <el-table-column prop="lastResult" :label="t('jobs.columns.result')" min-width="180" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.lastResult || '—' }}</template>
        </el-table-column>
        <el-table-column :label="t('jobs.columns.operation')" width="280" fixed="right">
          <template #default="scope">
            <el-button v-if="canEdit" size="small" @click="runJob(scope.row)">{{ t('jobs.run') }}</el-button>
            <el-button size="small" @click="openLogDialog(scope.row)">{{ t('jobs.records') }}</el-button>
            <el-button v-if="canEdit" size="small" @click="openEditDialog(scope.row)">{{ t('jobs.edit') }}</el-button>
            <el-button v-if="canEdit" size="small" type="danger" @click="deleteJob(scope.row)">{{ t('jobs.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="tip">{{ t('jobs.tip') }}</p>
    </div>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="560px">
      <el-form ref="jobFormRef" :model="jobForm" :rules="formRules" label-width="100px">
        <el-form-item :label="t('jobs.form.handler')" prop="handlerKey">
          <el-select
            v-model="jobForm.handlerKey"
            :disabled="isEdit"
            :placeholder="t('jobs.handlerPlaceholder')"
            style="width: 100%;"
          >
            <el-option
              v-for="item in handlers"
              :key="item.key"
              :label="item.name"
              :value="item.key"
            />
          </el-select>
          <p v-if="handlerTip" class="handler-tip">{{ handlerTip }}</p>
        </el-form-item>
        <el-form-item :label="t('jobs.form.name')" prop="name">
          <el-input v-model="jobForm.name" :placeholder="t('jobs.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('jobs.form.cron')" prop="cronExpression">
          <el-input v-model="jobForm.cronExpression" :placeholder="t('jobs.cronPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('jobs.form.remark')">
          <el-input v-model="jobForm.remark" type="textarea" :rows="2" :placeholder="t('jobs.remarkPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('jobs.form.status')">
          <el-switch v-model="jobForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveJob">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="logVisible"
      :title="logJob ? t('jobs.logTitle', { name: logJob.name }) : t('jobs.logTitleEmpty')"
      size="680px"
    >
      <el-table v-loading="logLoading" :data="logs" border stripe>
        <el-table-column prop="createTime" :label="t('jobs.logColumns.createTime')" width="170" />
        <el-table-column :label="t('jobs.logColumns.triggerType')" width="90">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.triggerType === 2 ? 'warning' : 'info'">
              {{ scope.row.triggerType === 2 ? t('jobs.triggerManual') : t('jobs.triggerSchedule') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('jobs.logColumns.status')" width="90">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? t('jobs.success') : t('jobs.failed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('jobs.logColumns.duration')" width="90">
          <template #default="scope">{{ scope.row.durationMs }} ms</template>
        </el-table-column>
        <el-table-column prop="result" :label="t('jobs.logColumns.result')" min-width="220" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.result || '—' }}</template>
        </el-table-column>
      </el-table>
      <div class="log-pager">
        <el-pagination
          v-model:current-page="logPage"
          :page-size="logSize"
          :total="logTotal"
          layout="total, prev, pager, next"
          @current-change="loadLogs"
        />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getJobsApi,
  getJobHandlersApi,
  createJobApi,
  updateJobApi,
  deleteJobApi,
  runJobApi,
  getJobLogsApi
} from '@/api/job'
import { useUserStore } from '@/stores/user'
import { useSiteStore } from '@/stores/site'
import { useI18n } from 'vue-i18n'
import { useDictOptions, dictLabel } from '@/utils/dict'

const icons = { Plus }

const userStore = useUserStore()
const siteStore = useSiteStore()
const { t } = useI18n()
const canEdit = computed(() => userStore.permissions.includes('job:edit'))

// 任务状态由数据字典驱动，可在「数据字典」页维护
const jobStatusOptions = useDictOptions('job_status',
  [{ label: '启用', value: 1 }, { label: '停用', value: 0 }], Number)

// 状态文案：中文沿用数据字典，英文使用内置词条
function statusText(value) {
  if (siteStore.locale === 'en') {
    return t(value === 1 ? 'common.enabled' : 'common.disabled')
  }
  return dictLabel(jobStatusOptions, value)
}

const jobs = ref([])
const handlers = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = computed(() => isEdit.value ? t('jobs.editTitle') : t('jobs.addTitle'))
const isEdit = ref(false)
const jobFormRef = ref(null)

const logVisible = ref(false)
const logLoading = ref(false)
const logJob = ref(null)
const logs = ref([])
const logPage = ref(1)
const logSize = 10
const logTotal = ref(0)

const jobForm = reactive({
  id: null,
  name: '',
  handlerKey: '',
  cronExpression: '',
  status: 0,
  remark: ''
})

const formRules = computed(() => ({
  handlerKey: [
    { required: true, message: t('jobs.handlerRequired'), trigger: 'change' }
  ],
  name: [
    { required: true, message: t('jobs.nameRequired'), trigger: 'blur' }
  ],
  cronExpression: [
    { required: true, message: t('jobs.cronRequired'), trigger: 'blur' }
  ]
}))

const handlerTip = computed(() => {
  const handler = handlers.value.find(item => item.key === jobForm.handlerKey)
  return handler ? handler.description : ''
})

onMounted(() => {
  loadJobs()
  loadHandlers()
})

async function loadJobs() {
  loading.value = true
  try {
    const res = await getJobsApi()
    jobs.value = res.data
  } catch (error) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function loadHandlers() {
  try {
    const res = await getJobHandlersApi()
    handlers.value = res.data
  } catch (error) {
    handlers.value = []
  }
}

function openAddDialog() {
  isEdit.value = false
  Object.assign(jobForm, {
    id: null,
    name: '',
    handlerKey: '',
    cronExpression: '',
    status: 0,
    remark: ''
  })
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  Object.assign(jobForm, {
    id: row.id,
    name: row.name,
    handlerKey: row.handlerKey,
    cronExpression: row.cronExpression,
    status: row.status,
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

async function saveJob() {
  const valid = await jobFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    name: jobForm.name,
    handlerKey: jobForm.handlerKey,
    cronExpression: jobForm.cronExpression,
    status: jobForm.status,
    remark: jobForm.remark
  }

  try {
    if (isEdit.value) {
      await updateJobApi(jobForm.id, payload)
    } else {
      await createJobApi(payload)
    }
    ElMessage.success(isEdit.value ? t('common.updateSuccess') : t('common.createSuccess'))
    dialogVisible.value = false
    loadJobs()
  } catch (error) {
    // 拦截器已提示
  }
}

async function toggleStatus(row, value) {
  try {
    await updateJobApi(row.id, {
      name: row.name,
      handlerKey: row.handlerKey,
      cronExpression: row.cronExpression,
      status: value,
      remark: row.remark || ''
    })
    ElMessage.success(value === 1 ? t('jobs.enabled') : t('jobs.stopped'))
  } catch (error) {
    // 拦截器已提示
  }
  loadJobs()
}

async function runJob(row) {
  try {
    const res = await runJobApi(row.id)
    ElMessage.success(res.data || t('jobs.runSuccess'))
  } catch (error) {
    // 拦截器已提示
  }
  loadJobs()
  if (logVisible.value && logJob.value && logJob.value.id === row.id) {
    loadLogs()
  }
}

function openLogDialog(row) {
  logJob.value = row
  logPage.value = 1
  logVisible.value = true
  loadLogs()
}

async function loadLogs() {
  if (!logJob.value) return
  logLoading.value = true
  try {
    const res = await getJobLogsApi(logJob.value.id, { page: logPage.value, size: logSize })
    logs.value = res.data.list
    logTotal.value = res.data.total
  } catch (error) {
    logs.value = []
    logTotal.value = 0
  } finally {
    logLoading.value = false
  }
}

async function deleteJob(row) {
  try {
    await ElMessageBox.confirm(t('jobs.deleteConfirm', { name: row.name }), t('common.confirmTitle'), { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteJobApi(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    loadJobs()
  } catch (error) {
    // 拦截器已提示
  }
}
</script>

<style scoped>
.tip {
  margin-top: 12px;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.handler-tip {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.log-pager {
  margin-top: 12px;
  text-align: right;
}
</style>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">定时任务</h2>
      <el-button v-permission="'job:edit'" type="primary" @click="openAddDialog">
        <el-icon><component :is="icons.Plus" /></el-icon>
        新增任务
      </el-button>
    </div>

    <div class="card">
      <el-table v-loading="loading" :data="jobs" border stripe>
        <el-table-column prop="name" label="任务名称" min-width="140" />
        <el-table-column prop="handlerName" label="处理器" width="150" />
        <el-table-column prop="cronExpression" label="Cron 表达式" width="140" />
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              inline-prompt
              :width="56"
              :active-text="dictLabel(jobStatusOptions, 1)"
              :inactive-text="dictLabel(jobStatusOptions, 0)"
              :disabled="!canEdit"
              @change="value => toggleStatus(scope.row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="lastRunTime" label="最近执行" width="170">
          <template #default="scope">{{ scope.row.lastRunTime || '—' }}</template>
        </el-table-column>
        <el-table-column prop="lastResult" label="执行结果" min-width="180" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.lastResult || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button v-if="canEdit" size="small" @click="runJob(scope.row)">执行</el-button>
            <el-button size="small" @click="openLogDialog(scope.row)">记录</el-button>
            <el-button v-if="canEdit" size="small" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button v-if="canEdit" size="small" type="danger" @click="deleteJob(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="tip">Cron 使用 Spring 格式（秒 分 时 日 月 周），如 0 0 3 * * ? 表示每天 03:00。</p>
    </div>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="560px">
      <el-form ref="jobFormRef" :model="jobForm" :rules="formRules" label-width="100px">
        <el-form-item label="任务处理器" prop="handlerKey">
          <el-select
            v-model="jobForm.handlerKey"
            :disabled="isEdit"
            placeholder="请选择处理器"
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
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="jobForm.name" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="Cron 表达式" prop="cronExpression">
          <el-input v-model="jobForm.cronExpression" placeholder="如 0 0 3 * * ?" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="jobForm.remark" type="textarea" :rows="2" placeholder="任务用途说明" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="jobForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveJob">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="logVisible"
      :title="logJob ? `执行记录 - ${logJob.name}` : '执行记录'"
      size="680px"
    >
      <el-table v-loading="logLoading" :data="logs" border stripe>
        <el-table-column prop="createTime" label="执行时间" width="170" />
        <el-table-column label="触发方式" width="90">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.triggerType === 2 ? 'warning' : 'info'">
              {{ scope.row.triggerType === 2 ? '手动' : '定时' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90">
          <template #default="scope">{{ scope.row.durationMs }} ms</template>
        </el-table-column>
        <el-table-column prop="result" label="结果说明" min-width="220" show-overflow-tooltip>
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
import { useDictOptions, dictLabel } from '@/utils/dict'

const icons = { Plus }

const userStore = useUserStore()
const canEdit = computed(() => userStore.permissions.includes('job:edit'))

// 任务状态由数据字典驱动，可在「数据字典」页维护
const jobStatusOptions = useDictOptions('job_status',
  [{ label: '启用', value: 1 }, { label: '停用', value: 0 }], Number)

const jobs = ref([])
const handlers = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增任务')
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

const formRules = {
  handlerKey: [
    { required: true, message: '请选择任务处理器', trigger: 'change' }
  ],
  name: [
    { required: true, message: '请输入任务名称', trigger: 'blur' }
  ],
  cronExpression: [
    { required: true, message: '请输入 Cron 表达式', trigger: 'blur' }
  ]
}

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
  dialogTitle.value = '新增任务'
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
  dialogTitle.value = '编辑任务'
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
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
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
    ElMessage.success(value === 1 ? '已启用' : '已停用')
  } catch (error) {
    // 拦截器已提示
  }
  loadJobs()
}

async function runJob(row) {
  try {
    const res = await runJobApi(row.id)
    ElMessage.success(res.data || '执行成功')
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
    await ElMessageBox.confirm(`确定要删除任务「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteJobApi(row.id)
    ElMessage.success('删除成功')
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

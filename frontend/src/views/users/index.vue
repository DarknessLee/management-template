<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('users.title') }}</h2>
      <div class="header-actions">
        <el-button v-permission="'users:add'" @click="openImportDialog">{{ t('users.import') }}</el-button>
        <el-button v-permission="'users:view'" @click="exportUsers">{{ t('users.export') }}</el-button>
        <el-button v-permission="'users:add'" type="primary" @click="openAddDialog">
          <el-icon><component :is="icons.Plus" /></el-icon>
          {{ t('users.add') }}
        </el-button>
      </div>
    </div>

    <div class="card">
      <div class="search-bar">
        <el-input 
          v-model="searchKeyword" 
          :placeholder="t('users.searchPlaceholder')"
          :prefix-icon="icons.Search"
          clearable
          @keyup.enter="loadUsers"
        />
        <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
        <el-button @click="resetSearch">{{ t('common.reset') }}</el-button>
      </div>

      <div v-if="canEdit" class="batch-bar">
        <span class="batch-count">{{ t('users.selectedCount', { count: selectedIds.length }) }}</span>
        <el-button size="small" :disabled="!selectedIds.length" @click="batchStatus(1)">{{ t('users.batchEnable') }}</el-button>
        <el-button size="small" :disabled="!selectedIds.length" @click="batchStatus(0)">{{ t('users.batchDisable') }}</el-button>
        <el-button size="small" :disabled="!selectedIds.length" @click="openBatchRoleDialog">{{ t('users.batchRole') }}</el-button>
        <el-button size="small" type="danger" plain :disabled="!selectedIds.length" @click="batchResetPassword">
          {{ t('users.batchReset') }}
        </el-button>
      </div>

      <el-table ref="userTableRef" :data="userList" border stripe @selection-change="handleSelectionChange">
        <el-table-column v-if="canEdit" type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" :label="t('users.columns.username')" />
        <el-table-column prop="realName" :label="t('users.columns.realName')" />
        <el-table-column prop="deptName" :label="t('users.columns.dept')" />
        <el-table-column prop="email" :label="t('users.columns.email')" />
        <el-table-column prop="phone" :label="t('users.columns.phone')" />
        <el-table-column prop="role" :label="t('users.columns.role')">
          <template #default="scope">
            <el-tag :type="getRoleType(scope.row.role)">
              {{ getRoleName(scope.row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('users.columns.status')">
          <template #default="scope">
            <el-switch 
              :model-value="scope.row.status === 1" 
              inline-prompt
              :width="52"
              :active-text="statusText(1)"
              :inactive-text="statusText(0)"
              :disabled="!canEdit"
              @change="toggleStatus(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('users.columns.createTime')" />
        <el-table-column v-if="canOperate" :label="t('users.columns.operation')" width="300">
          <template #default="scope">
            <el-button v-permission="'users:edit'" size="small" @click="openEditDialog(scope.row)">{{ t('users.edit') }}</el-button>
            <el-button
              v-if="scope.row.id !== userStore.userInfo?.id"
              v-permission="'users:edit'"
              size="small"
              type="warning"
              @click="kickoutUser(scope.row)"
            >
              {{ t('users.kickout') }}
            </el-button>
            <el-button v-permission="'users:delete'" size="small" type="danger" @click="deleteUser(scope.row)">{{ t('users.delete') }}</el-button>
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

    <el-dialog :title="t('users.import')" v-model="importDialogVisible" width="560px" @closed="resetImport">
      <el-alert
        :title="t('users.importTip')"
        type="info"
        :closable="false"
        show-icon
      />
      <div class="template-line">
        <el-button link type="primary" @click="downloadTemplate">{{ t('users.downloadTemplate') }}</el-button>
      </div>
      <el-upload
        ref="importUploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx"
        :on-change="handleImportFileChange"
        :on-remove="handleImportFileRemove"
      >
        <el-button>{{ t('users.selectFile') }}</el-button>
        <template #tip>
          <div class="el-upload__tip">{{ t('users.importFileTip') }}</div>
        </template>
      </el-upload>

      <div v-if="importResult" class="import-result">
        <el-alert
          :title="t('users.importSummary', { total: importResult.total, success: importResult.success, failed: importResult.failed })"
          :type="importResult.failed > 0 ? 'warning' : 'success'"
          :closable="false"
          show-icon
        />
        <el-table
          v-if="importResult.failed > 0"
          :data="importResult.errors"
          border
          size="small"
          max-height="240"
          class="error-table"
        >
          <el-table-column prop="row" :label="t('users.rowNo')" width="80" />
          <el-table-column prop="message" :label="t('users.failReason')" />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="importDialogVisible = false">{{ t('common.close') }}</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">{{ t('users.startImport') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog 
      :title="dialogTitle" 
      v-model="dialogVisible"
      width="500px"
    >
      <el-form ref="userFormRef" :model="userForm" :rules="formRules" label-width="100px">
        <el-form-item :label="t('users.form.username')" prop="username">
          <el-input v-model="userForm.username" :placeholder="t('users.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('users.form.realName')" prop="realName">
          <el-input v-model="userForm.realName" :placeholder="t('users.realNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('users.form.password')" prop="password">
          <el-input 
            v-model="userForm.password" 
            type="password" 
            :placeholder="isEdit ? t('users.passwordEditPlaceholder') : t('users.passwordPlaceholder')" 
          />
        </el-form-item>
        <el-form-item :label="t('users.form.email')" prop="email">
          <el-input v-model="userForm.email" type="email" :placeholder="t('users.emailPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('users.form.phone')" prop="phone">
          <el-input v-model="userForm.phone" :placeholder="t('users.phonePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('users.form.role')" prop="role">
          <el-select v-model="userForm.role" :placeholder="t('users.rolePlaceholder')">
            <el-option 
              v-for="role in roleOptions" 
              :key="role.value" 
              :label="role.label" 
              :value="role.value" 
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('users.form.dept')">
          <el-tree-select
            v-model="userForm.deptId"
            :data="deptTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            :placeholder="t('users.deptPlaceholder')"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveUser">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchRoleDialogVisible" :title="t('users.batchRole')" width="420px">
      <el-form label-width="80px">
        <el-form-item :label="t('users.form.role')">
          <el-select v-model="batchRole" :placeholder="t('users.rolePlaceholder')" style="width: 100%;">
            <el-option v-for="role in roleOptions" :key="role.value" :label="role.label" :value="role.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchRoleDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="submitBatchRole">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetDialogVisible" :title="t('users.batchResetTitle')" width="600px">
      <el-alert :title="t('users.batchResetTip')" type="warning" :closable="false" show-icon />
      <el-table :data="resetResults" border size="small" class="reset-table">
        <el-table-column prop="username" :label="t('users.columns.username')" />
        <el-table-column prop="realName" :label="t('users.columns.realName')" />
        <el-table-column :label="t('users.password')" min-width="180">
          <template #default="scope">
            <span class="temp-password">{{ scope.row.password }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('users.columns.operation')" width="90">
          <template #default="scope">
            <el-button link type="primary" @click="copyPassword(scope.row.password)">{{ t('users.copy') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button type="primary" @click="resetDialogVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { getUsersApi, createUserApi, updateUserApi, deleteUserApi, kickoutUserApi, importUsersApi, exportUsersApi, downloadUserTemplateApi, batchUpdateUserStatusApi, batchUpdateUserRoleApi, batchResetUserPasswordApi } from '@/api/user'
import { getRolesApi } from '@/api/role'
import { getDeptOptionsApi } from '@/api/dept'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { useSiteStore } from '@/stores/site'
import { useDictOptions, dictLabel } from '@/utils/dict'

const icons = { Plus, Search }

const { t } = useI18n()
const userStore = useUserStore()
const siteStore = useSiteStore()
// 用户状态由数据字典驱动，可在「数据字典」页维护
const userStatusOptions = useDictOptions('user_status',
  [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }], Number)

// 状态文案：中文沿用数据字典，英文使用内置词条
function statusText(value) {
  if (siteStore.locale === 'en') {
    return t(value === 1 ? 'common.enabled' : 'common.disabled')
  }
  return dictLabel(userStatusOptions, value)
}
// 没有任何操作权限时整列隐藏，避免留下一列空白按钮
const canEdit = computed(() => userStore.permissions.includes('users:edit'))
const canOperate = computed(() => canEdit.value || userStore.permissions.includes('users:delete'))

const userList = ref([])
const userTableRef = ref(null)
const selectedIds = ref([])
const batchRoleDialogVisible = ref(false)
const batchRole = ref('')
const batchSubmitting = ref(false)
const resetDialogVisible = ref(false)
const resetResults = ref([])
const searchKeyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = computed(() => isEdit.value ? t('users.editTitle') : t('users.addTitle'))
const userFormRef = ref(null)
const roleOptions = ref([])
const deptOptions = ref([])
const deptTree = computed(() => buildDeptTree(deptOptions.value))

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const userForm = reactive({
  id: null,
  username: '',
  realName: '',
  password: '',
  email: '',
  phone: '',
  role: '',
  deptId: null,
  status: 1
})

const formRules = computed(() => ({
  username: [
    { required: true, message: t('users.usernameRequired'), trigger: 'blur' }
  ],
  realName: [
    { required: true, message: t('users.realNameRequired'), trigger: 'blur' }
  ],
  password: isEdit.value
    ? []
    : [
        { required: true, message: t('users.passwordRequired'), trigger: 'blur' },
        { min: 6, message: t('users.passwordLength'), trigger: 'blur' }
      ],
  email: [
    { required: true, message: t('users.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('users.emailInvalid'), trigger: 'blur' }
  ],
  phone: [
    { required: true, message: t('users.phoneRequired'), trigger: 'blur' }
  ],
  role: [
    { required: true, message: t('users.roleRequired'), trigger: 'change' }
  ]
}))

onMounted(() => {
  loadRoleOptions()
  loadDeptOptions()
  loadUsers()
})

async function loadDeptOptions() {
  try {
    const res = await getDeptOptionsApi()
    deptOptions.value = res.data
  } catch (error) {
    deptOptions.value = []
  }
}

function buildDeptTree(items) {
  const map = new Map()
  items.forEach(item => map.set(item.id, { ...item, children: [] }))

  const roots = []
  map.forEach(node => {
    const parent = map.get(node.parentId)
    if (parent && node.parentId !== 0) {
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

async function loadRoleOptions() {
  try {
    const res = await getRolesApi()
    roleOptions.value = res.data.list.map(role => ({
      label: role.name,
      value: role.code
    }))
  } catch (error) {
    roleOptions.value = []
  }
}

async function loadUsers() {
  try {
    const res = await getUsersApi({
      page: pagination.page,
      size: pagination.size,
      keyword: searchKeyword.value
    })
    userList.value = res.data.list
    pagination.total = res.data.total
  } catch (error) {
    // 拦截器已提示
  }
}

function handleSearch() {
  pagination.page = 1
  loadUsers()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadUsers()
}

function handleCurrentChange(page) {
  pagination.page = page
  loadUsers()
}

function resetSearch() {
  searchKeyword.value = ''
  pagination.page = 1
  loadUsers()
}

function openAddDialog() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  Object.assign(userForm, {
    id: row.id,
    username: row.username,
    realName: row.realName,
    password: '',
    email: row.email,
    phone: row.phone,
    role: row.role,
    deptId: row.deptId || null,
    status: row.status
  })
  dialogVisible.value = true
}

function resetForm() {
  userForm.id = null
  userForm.username = ''
  userForm.realName = ''
  userForm.password = ''
  userForm.email = ''
  userForm.phone = ''
  userForm.role = ''
  userForm.deptId = null
  userForm.status = 1
}

async function saveUser() {
  const valid = await userFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    username: userForm.username,
    realName: userForm.realName,
    email: userForm.email,
    phone: userForm.phone,
    role: userForm.role,
    deptId: userForm.deptId || 0,
    status: userForm.status
  }
  // 编辑时密码留空表示不修改
  if (!isEdit.value || userForm.password) {
    payload.password = userForm.password
  }

  try {
    if (isEdit.value) {
      await updateUserApi(userForm.id, payload)
    } else {
      await createUserApi(payload)
    }
    ElMessage.success(isEdit.value ? t('common.updateSuccess') : t('common.createSuccess'))
    dialogVisible.value = false
    loadUsers()
  } catch (error) {
    // 拦截器已提示
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await updateUserApi(row.id, { status: newStatus })
    row.status = newStatus
    ElMessage.success(t('common.statusUpdated'))
  } catch (error) {
    // 失败时开关保持原状
  }
}

async function deleteUser(row) {
  try {
    await ElMessageBox.confirm(t('users.deleteConfirm'), t('common.confirmTitle'), { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteUserApi(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    // 删除当前页最后一条时回退一页，避免出现空表格
    if (userList.value.length === 1 && pagination.page > 1) {
      pagination.page -= 1
    }
    loadUsers()
  } catch (error) {
    // 拦截器已提示
  }
}

async function kickoutUser(row) {
  try {
    await ElMessageBox.confirm(
      t('users.kickoutConfirm', { name: row.username }),
      t('users.kickout'),
      { type: 'warning' }
    )
  } catch (error) {
    return
  }

  try {
    const res = await kickoutUserApi(row.id)
    ElMessage.success(t('users.kickedOut', { count: res.data }))
  } catch (error) {
    // 拦截器已提示
  }
}

function getRoleName(role) {
  const option = roleOptions.value.find(item => item.value === role)
  return option ? option.label : role
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map(row => row.id)
}

async function batchStatus(status) {
  const action = status === 1 ? t('users.batchEnable') : t('users.batchDisable')
  try {
    await ElMessageBox.confirm(
      t('users.batchStatusConfirm', { action, count: selectedIds.value.length }),
      t('common.confirmTitle'),
      { type: 'warning' }
    )
  } catch (error) {
    return
  }
  try {
    const res = await batchUpdateUserStatusApi({ ids: selectedIds.value, status })
    ElMessage.success(t('users.batchDone', { count: res.data }))
    userTableRef.value?.clearSelection()
    loadUsers()
  } catch (error) {
    // 拦截器已提示
  }
}

function openBatchRoleDialog() {
  batchRole.value = ''
  batchRoleDialogVisible.value = true
}

async function submitBatchRole() {
  if (!batchRole.value) {
    ElMessage.warning(t('users.batchRoleRequired'))
    return
  }
  batchSubmitting.value = true
  try {
    const res = await batchUpdateUserRoleApi({ ids: selectedIds.value, role: batchRole.value })
    ElMessage.success(t('users.batchDone', { count: res.data }))
    batchRoleDialogVisible.value = false
    userTableRef.value?.clearSelection()
    loadUsers()
  } catch (error) {
    // 拦截器已提示
  } finally {
    batchSubmitting.value = false
  }
}

async function batchResetPassword() {
  try {
    await ElMessageBox.confirm(
      t('users.batchResetConfirm', { count: selectedIds.value.length }),
      t('common.confirmTitle'),
      { type: 'warning' }
    )
  } catch (error) {
    return
  }
  try {
    const res = await batchResetUserPasswordApi({ ids: selectedIds.value })
    resetResults.value = res.data
    resetDialogVisible.value = true
    ElMessage.success(t('users.batchResetDone', { count: res.data.length }))
    userTableRef.value?.clearSelection()
    loadUsers()
  } catch (error) {
    // 拦截器已提示
  }
}

async function copyPassword(password) {
  try {
    await navigator.clipboard.writeText(password)
    ElMessage.success(t('users.copied'))
  } catch (error) {
    ElMessage.warning(t('users.copyFailed'))
  }
}

function getRoleType(role) {
  const typeMap = {
    admin: 'danger',
    editor: 'primary',
    viewer: 'info'
  }
  return typeMap[role] || 'info'
}
const importDialogVisible = ref(false)
const importUploadRef = ref(null)
const importFile = ref(null)
const importing = ref(false)
const importResult = ref(null)

function openImportDialog() {
  importDialogVisible.value = true
}

function handleImportFileChange(uploadFile) {
  importFile.value = uploadFile.raw
}

function handleImportFileRemove() {
  importFile.value = null
}

function resetImport() {
  importFile.value = null
  importResult.value = null
  importUploadRef.value?.clearFiles()
}

async function downloadTemplate() {
  try {
    const response = await downloadUserTemplateApi()
    saveBlob(response, 'user-import-template.xlsx')
  } catch (error) {
    // 拦截器已提示
  }
}

async function submitImport() {
  if (!importFile.value) {
    ElMessage.warning(t('users.importNoFile'))
    return
  }

  importing.value = true
  try {
    const res = await importUsersApi(importFile.value)
    importResult.value = res.data
    if (res.data.success > 0) {
      loadUsers()
    }
    if (res.data.failed === 0) {
      ElMessage.success(t('users.importSuccess', { count: res.data.success }))
    }
  } catch (error) {
    // 拦截器已提示
  } finally {
    importing.value = false
  }
}

async function exportUsers() {
  try {
    const response = await exportUsersApi({ keyword: searchKeyword.value || undefined })
    saveBlob(response, 'users.xlsx')
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
.header-actions {
  display: flex;
  align-items: center;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.search-bar .el-input {
  width: 300px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.template-line {
  margin: 12px 0;
  text-align: right;
}

.import-result {
  margin-top: 16px;
}

.error-table {
  margin-top: 12px;
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--app-bg-page, #f5f7fa);
}

.batch-count {
  margin-right: 4px;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.reset-table {
  margin-top: 12px;
}

.temp-password {
  font-family: Consolas, Monaco, monospace;
  letter-spacing: 0.5px;
}
</style>

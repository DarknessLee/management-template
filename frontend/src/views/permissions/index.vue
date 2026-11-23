<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('permissions.title') }}</h2>
      <el-button v-permission="'roles:edit'" type="primary" @click="openAddDialog">
        <el-icon><component :is="icons.Plus" /></el-icon>
        {{ t('permissions.add') }}
      </el-button>
    </div>

    <div class="card">
      <el-table :data="permissionList" border stripe>
        <el-table-column prop="module" :label="t('permissions.columns.module')" width="140">
          <template #default="scope">
            <el-tag size="small" type="info">{{ scope.row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="code" :label="t('permissions.columns.code')" width="220" />
        <el-table-column prop="name" :label="t('permissions.columns.name')" />
        <el-table-column prop="createTime" :label="t('permissions.columns.createTime')" width="180" />
        <el-table-column v-if="canEdit" :label="t('permissions.columns.operation')" width="180">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">{{ t('permissions.edit') }}</el-button>
            <el-button size="small" type="danger" @click="deletePermission(scope.row)">{{ t('permissions.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="tip">{{ t('permissions.tip') }}</p>
    </div>

    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
    >
      <el-form ref="permissionFormRef" :model="permissionForm" :rules="formRules" label-width="100px">
        <el-form-item :label="t('permissions.form.code')" prop="code">
          <el-input
            v-model="permissionForm.code"
            :disabled="isEdit"
            :placeholder="t('permissions.codePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('permissions.form.name')" prop="name">
          <el-input v-model="permissionForm.name" :placeholder="t('permissions.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('permissions.form.module')" prop="module">
          <el-select
            v-model="permissionForm.module"
            filterable
            allow-create
            default-first-option
            :placeholder="t('permissions.modulePlaceholder')"
            style="width: 100%;"
          >
            <el-option v-for="item in moduleOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="savePermission">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getPermissionsApi,
  createPermissionApi,
  updatePermissionApi,
  deletePermissionApi
} from '@/api/permission'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'

const icons = { Plus }

const userStore = useUserStore()
const { t } = useI18n()
const canEdit = computed(() => userStore.permissions.includes('roles:edit'))

const permissionList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = computed(() => isEdit.value ? t('permissions.editTitle') : t('permissions.addTitle'))
const permissionFormRef = ref(null)

const permissionForm = reactive({
  id: null,
  code: '',
  name: '',
  module: ''
})

const formRules = computed(() => ({
  code: [
    { required: true, message: t('permissions.codeRequired'), trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9]*:[a-z][a-z0-9]*$/,
      message: t('permissions.codePattern'),
      trigger: 'blur'
    }
  ],
  name: [
    { required: true, message: t('permissions.nameRequired'), trigger: 'blur' }
  ],
  module: [
    { required: true, message: t('permissions.moduleRequired'), trigger: 'blur' }
  ]
}))

const moduleOptions = computed(() => [...new Set(permissionList.value.map(item => item.module))])

onMounted(() => {
  loadPermissions()
})

async function loadPermissions() {
  try {
    const res = await getPermissionsApi()
    permissionList.value = res.data
  } catch (error) {
    // 拦截器已提示
  }
}

function openAddDialog() {
  isEdit.value = false
  permissionForm.id = null
  permissionForm.code = ''
  permissionForm.name = ''
  permissionForm.module = ''
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  Object.assign(permissionForm, {
    id: row.id,
    code: row.code,
    name: row.name,
    module: row.module
  })
  dialogVisible.value = true
}

async function savePermission() {
  const valid = await permissionFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    code: permissionForm.code,
    name: permissionForm.name,
    module: permissionForm.module
  }

  try {
    if (isEdit.value) {
      await updatePermissionApi(permissionForm.id, payload)
    } else {
      await createPermissionApi(payload)
    }
    ElMessage.success(isEdit.value ? t('common.updateSuccess') : t('common.createSuccess'))
    dialogVisible.value = false
    loadPermissions()
  } catch (error) {
    // 拦截器已提示
  }
}

async function deletePermission(row) {
  try {
    await ElMessageBox.confirm(t('permissions.deleteConfirm', { name: row.name }), t('common.confirmTitle'), { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deletePermissionApi(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    loadPermissions()
  } catch (error) {
    // 拦截器已提示（例如已被角色使用）
  }
}
</script>

<style scoped>
.tip {
  margin-top: 12px;
  font-size: 13px;
  color: var(--app-text-secondary);
}
</style>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('roles.title') }}</h2>
      <el-button v-permission="'roles:add'" type="primary" @click="openAddDialog">
        <el-icon><component :is="icons.Plus" /></el-icon>
        {{ t('roles.add') }}
      </el-button>
    </div>

    <div class="card">
      <el-table :data="roleList" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" :label="t('roles.columns.name')" />
        <el-table-column prop="code" :label="t('roles.columns.code')" />
        <el-table-column prop="description" :label="t('roles.columns.description')" />
        <el-table-column :label="t('roles.columns.dataScope')" width="140">
          <template #default="scope">
            <el-tag size="small" type="info">{{ dataScopeLabel(scope.row.dataScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="permissions" :label="t('roles.columns.permissions')">
          <template #default="scope">
            <el-tag 
              v-for="(perm, index) in scope.row.permissions.slice(0, 3)" 
              :key="index" 
              size="small"
              type="info"
            >
              {{ getPermissionName(perm) }}
            </el-tag>
            <span v-if="scope.row.permissions.length > 3">
              {{ t('roles.permissionMore', { count: scope.row.permissions.length }) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('roles.columns.createTime')" />
        <el-table-column v-if="canOperate" :label="t('roles.columns.operation')" width="200">
          <template #default="scope">
            <el-button v-permission="'roles:edit'" size="small" @click="openEditDialog(scope.row)">{{ t('roles.edit') }}</el-button>
            <el-button v-permission="'roles:delete'" size="small" type="danger" @click="deleteRole(scope.row)">{{ t('roles.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog 
      :title="dialogTitle" 
      v-model="dialogVisible"
      width="600px"
    >
      <el-form ref="roleFormRef" :model="roleForm" :rules="formRules" label-width="100px">
        <el-form-item :label="t('roles.columns.name')" prop="name">
          <el-input v-model="roleForm.name" :placeholder="t('roles.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('roles.columns.code')" prop="code">
          <el-input v-model="roleForm.code" :placeholder="t('roles.codePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('roles.columns.description')" prop="description">
          <el-input 
            v-model="roleForm.description" 
            type="textarea" 
            :placeholder="t('roles.descriptionPlaceholder')"
            :rows="3"
          />
        </el-form-item>
        <el-form-item :label="t('roles.columns.dataScope')">
          <el-select v-model="roleForm.dataScope" style="width: 100%;">
            <el-option
              v-for="item in dataScopeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <div class="form-tip">{{ t('roles.dataScopeTip') }}</div>
        </el-form-item>
        <el-form-item :label="t('roles.columns.permissions')">
          <div class="permission-groups">
            <div v-if="permissionGroups.length === 0" class="permission-empty">
              {{ t('roles.permissionEmpty') }}
            </div>
            <div v-for="group in permissionGroups" :key="group.module" class="permission-group">
              <el-checkbox
                class="permission-group-title"
                :model-value="isGroupChecked(group)"
                :indeterminate="isGroupIndeterminate(group)"
                @change="(checked) => toggleGroup(group, checked)"
              >
                {{ group.module }}
              </el-checkbox>
              <el-checkbox-group v-model="roleForm.permissions">
                <el-checkbox v-for="perm in group.items" :key="perm.code" :value="perm.code">
                  {{ perm.name }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveRole">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getRolesApi, createRoleApi, updateRoleApi, deleteRoleApi } from '@/api/role'
import { getPermissionsApi } from '@/api/permission'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'

const icons = { Plus }

const { t } = useI18n()
const userStore = useUserStore()
const canOperate = computed(() =>
  userStore.permissions.includes('roles:edit') || userStore.permissions.includes('roles:delete')
)

const roleList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = computed(() => isEdit.value ? t('roles.editTitle') : t('roles.addTitle'))
const roleFormRef = ref(null)

const roleForm = reactive({
  id: null,
  name: '',
  code: '',
  description: '',
  dataScope: 1,
  permissions: []
})

const dataScopeOptions = computed(() => [
  { value: 1, label: t('roles.dataScope.all') },
  { value: 2, label: t('roles.dataScope.dept') },
  { value: 3, label: t('roles.dataScope.deptAndChildren') },
  { value: 4, label: t('roles.dataScope.self') }
])

function dataScopeLabel(value) {
  const option = dataScopeOptions.value.find(item => item.value === (value || 1))
  return option ? option.label : t('roles.dataScope.all')
}

const formRules = computed(() => ({
  name: [
    { required: true, message: t('roles.nameRequired'), trigger: 'blur' }
  ],
  code: [
    { required: true, message: t('roles.codeRequired'), trigger: 'blur' },
    { pattern: /^[a-z]+$/, message: t('roles.codePattern'), trigger: 'blur' }
  ]
}))

const permissionGroups = ref([])

const permissionNameMap = computed(() => {
  const map = {}
  permissionGroups.value.forEach(group => {
    group.items.forEach(item => {
      map[item.code] = item.name
    })
  })
  return map
})

onMounted(() => {
  loadRoles()
  loadPermissions()
})

async function loadRoles() {
  try {
    const res = await getRolesApi()
    roleList.value = res.data.list
  } catch (error) {
    // 拦截器已提示
  }
}

// 权限点目录由后端维护，前端只负责按模块分组展示
async function loadPermissions() {
  try {
    const res = await getPermissionsApi()
    const groups = new Map()
    res.data.forEach(item => {
      if (!groups.has(item.module)) {
        groups.set(item.module, [])
      }
      groups.get(item.module).push(item)
    })
    permissionGroups.value = [...groups.entries()].map(([module, items]) => ({ module, items }))
  } catch (error) {
    // 拦截器已提示
  }
}

function isGroupChecked(group) {
  return group.items.length > 0 && group.items.every(item => roleForm.permissions.includes(item.code))
}

function isGroupIndeterminate(group) {
  const checked = group.items.filter(item => roleForm.permissions.includes(item.code)).length
  return checked > 0 && checked < group.items.length
}

function toggleGroup(group, checked) {
  const codes = group.items.map(item => item.code)
  if (checked) {
    roleForm.permissions = [...new Set([...roleForm.permissions, ...codes])]
  } else {
    roleForm.permissions = roleForm.permissions.filter(code => !codes.includes(code))
  }
}

function openAddDialog() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  Object.assign(roleForm, {
    id: row.id,
    name: row.name,
    code: row.code,
    description: row.description,
    dataScope: row.dataScope || 1,
    // 复制数组，避免取消编辑时污染列表数据
    permissions: [...row.permissions]
  })
  dialogVisible.value = true
}

function resetForm() {
  roleForm.id = null
  roleForm.name = ''
  roleForm.code = ''
  roleForm.description = ''
  roleForm.dataScope = 1
  roleForm.permissions = []
}

async function saveRole() {
  const valid = await roleFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    name: roleForm.name,
    code: roleForm.code,
    description: roleForm.description,
    dataScope: roleForm.dataScope,
    permissions: roleForm.permissions
  }

  try {
    if (isEdit.value) {
      await updateRoleApi(roleForm.id, payload)
    } else {
      await createRoleApi(payload)
    }
    ElMessage.success(isEdit.value ? t('common.updateSuccess') : t('common.createSuccess'))
    dialogVisible.value = false
    loadRoles()
  } catch (error) {
    // 拦截器已提示
  }
}

async function deleteRole(row) {
  try {
    await ElMessageBox.confirm(t('roles.deleteConfirm'), t('common.confirmTitle'), { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteRoleApi(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    loadRoles()
  } catch (error) {
    // 拦截器已提示（例如角色下仍有用户）
  }
}

function getPermissionName(perm) {
  return permissionNameMap.value[perm] || perm
}
</script>

<style scoped>
.permission-groups {
  width: 100%;
}

.permission-group {
  padding: 6px 0;
  border-bottom: 1px dashed var(--app-border);
}

.permission-group:last-child {
  border-bottom: none;
}

.permission-group-title {
  font-weight: 600;
}

.permission-group .el-checkbox-group {
  margin-left: 24px;
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px 16px;
}

.permission-empty {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.form-tip {
  font-size: 12px;
  color: var(--app-text-secondary);
  line-height: 1.6;
  margin-top: 4px;
}
</style>
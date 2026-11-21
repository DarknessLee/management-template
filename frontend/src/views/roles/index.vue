<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">角色管理</h2>
      <el-button v-permission="'roles:add'" type="primary" @click="openAddDialog">
        <el-icon><component :is="icons.Plus" /></el-icon>
        新增角色
      </el-button>
    </div>

    <div class="card">
      <el-table :data="roleList" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="角色名称" />
        <el-table-column prop="code" label="角色标识" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="数据范围" width="140">
          <template #default="scope">
            <el-tag size="small" type="info">{{ dataScopeLabel(scope.row.dataScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="permissions" label="权限">
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
              等{{ scope.row.permissions.length }}项
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column v-if="canOperate" label="操作" width="200">
          <template #default="scope">
            <el-button v-permission="'roles:edit'" size="small" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button v-permission="'roles:delete'" size="small" type="danger" @click="deleteRole(scope.row)">删除</el-button>
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
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色标识" prop="code">
          <el-input v-model="roleForm.code" placeholder="请输入角色标识（英文）" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="roleForm.description" 
            type="textarea" 
            placeholder="请输入角色描述"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="roleForm.dataScope" style="width: 100%;">
            <el-option
              v-for="item in dataScopeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <div class="form-tip">控制该角色在用户、日志等列表中能查看的数据范围</div>
        </el-form-item>
        <el-form-item label="权限">
          <div class="permission-groups">
            <div v-if="permissionGroups.length === 0" class="permission-empty">
              暂无可分配的权限点，请先到「权限点管理」维护
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
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRole">确定</el-button>
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
import { useUserStore } from '@/stores/user'

const icons = { Plus }

const userStore = useUserStore()
const canOperate = computed(() =>
  userStore.permissions.includes('roles:edit') || userStore.permissions.includes('roles:delete')
)

const roleList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const isEdit = ref(false)
const roleFormRef = ref(null)

const roleForm = reactive({
  id: null,
  name: '',
  code: '',
  description: '',
  dataScope: 1,
  permissions: []
})

const dataScopeOptions = [
  { value: 1, label: '全部数据' },
  { value: 2, label: '本部门数据' },
  { value: 3, label: '本部门及以下' },
  { value: 4, label: '仅本人' }
]

function dataScopeLabel(value) {
  const option = dataScopeOptions.find(item => item.value === (value || 1))
  return option ? option.label : '全部数据'
}

const formRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入角色标识', trigger: 'blur' },
    { pattern: /^[a-z]+$/, message: '角色标识只能包含小写字母', trigger: 'blur' }
  ]
}

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
  dialogTitle.value = '新增角色'
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  dialogTitle.value = '编辑角色'
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
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadRoles()
  } catch (error) {
    // 拦截器已提示
  }
}

async function deleteRole(row) {
  try {
    await ElMessageBox.confirm('确定要删除该角色吗？', '提示', { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteRoleApi(row.id)
    ElMessage.success('删除成功')
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
  border-bottom: 1px dashed #EBEEF5;
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
  color: #909399;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  margin-top: 4px;
}
</style>
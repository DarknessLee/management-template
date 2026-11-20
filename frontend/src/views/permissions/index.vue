<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">权限点管理</h2>
      <el-button v-permission="'roles:edit'" type="primary" @click="openAddDialog">
        <el-icon><component :is="icons.Plus" /></el-icon>
        新增权限点
      </el-button>
    </div>

    <div class="card">
      <el-table :data="permissionList" border stripe>
        <el-table-column prop="module" label="所属模块" width="140">
          <template #default="scope">
            <el-tag size="small" type="info">{{ scope.row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="权限编码" width="220" />
        <el-table-column prop="name" label="权限名称" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column v-if="canEdit" label="操作" width="180">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deletePermission(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="tip">权限编码在代码中用于接口与菜单/按钮鉴权，创建后不可修改。</p>
    </div>

    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
    >
      <el-form ref="permissionFormRef" :model="permissionForm" :rules="formRules" label-width="100px">
        <el-form-item label="权限编码" prop="code">
          <el-input
            v-model="permissionForm.code"
            :disabled="isEdit"
            placeholder="如 users:view"
          />
        </el-form-item>
        <el-form-item label="权限名称" prop="name">
          <el-input v-model="permissionForm.name" placeholder="如 查看用户" />
        </el-form-item>
        <el-form-item label="所属模块" prop="module">
          <el-select
            v-model="permissionForm.module"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入模块名"
            style="width: 100%;"
          >
            <el-option v-for="item in moduleOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePermission">确定</el-button>
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

const icons = { Plus }

const userStore = useUserStore()
const canEdit = computed(() => userStore.permissions.includes('roles:edit'))

const permissionList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增权限点')
const isEdit = ref(false)
const permissionFormRef = ref(null)

const permissionForm = reactive({
  id: null,
  code: '',
  name: '',
  module: ''
})

const formRules = {
  code: [
    { required: true, message: '请输入权限编码', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9]*:[a-z][a-z0-9]*$/,
      message: '编码格式应为 模块:动作，如 users:view',
      trigger: 'blur'
    }
  ],
  name: [
    { required: true, message: '请输入权限名称', trigger: 'blur' }
  ],
  module: [
    { required: true, message: '请输入所属模块', trigger: 'blur' }
  ]
}

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
  dialogTitle.value = '新增权限点'
  permissionForm.id = null
  permissionForm.code = ''
  permissionForm.name = ''
  permissionForm.module = ''
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  dialogTitle.value = '编辑权限点'
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
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadPermissions()
  } catch (error) {
    // 拦截器已提示
  }
}

async function deletePermission(row) {
  try {
    await ElMessageBox.confirm(`确定要删除权限点「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deletePermissionApi(row.id)
    ElMessage.success('删除成功')
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
  color: #909399;
}
</style>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">部门管理</h2>
      <el-button v-permission="'dept:edit'" type="primary" @click="openAddDialog()">
        <el-icon><component :is="icons.Plus" /></el-icon>
        新增部门
      </el-button>
    </div>

    <div class="card">
      <el-table
        v-loading="loading"
        :data="deptTree"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="部门名称" min-width="220" />
        <el-table-column prop="leader" label="负责人" width="140" />
        <el-table-column prop="phone" label="联系电话" width="160" />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column v-if="canEdit" label="操作" width="250">
          <template #default="scope">
            <el-button size="small" @click="openAddDialog(scope.row)">新增下级</el-button>
            <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteDept(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="tip">删除部门前需先移除其下级部门，并调整该部门下的用户。</p>
    </div>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="520px">
      <el-form ref="deptFormRef" :model="deptForm" :rules="formRules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="deptForm.parentId"
            :data="parentOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="不选则为顶级部门"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="deptForm.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="deptForm.leader" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="deptForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="deptForm.sort" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDept">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getDeptTreeApi,
  createDeptApi,
  updateDeptApi,
  deleteDeptApi
} from '@/api/dept'
import { useUserStore } from '@/stores/user'

const icons = { Plus }

const userStore = useUserStore()
const canEdit = computed(() => userStore.permissions.includes('dept:edit'))

const deptTree = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const isEdit = ref(false)
const deptFormRef = ref(null)

const deptForm = reactive({
  id: null,
  parentId: null,
  name: '',
  leader: '',
  phone: '',
  sort: 0
})

const formRules = {
  name: [
    { required: true, message: '请输入部门名称', trigger: 'blur' }
  ]
}

// 编辑时不能把自己或自己的下级选为上级，否则会形成环
const parentOptions = computed(() => {
  if (!isEdit.value) {
    return deptTree.value
  }
  return filterTree(deptTree.value, deptForm.id)
})

onMounted(() => {
  loadDepts()
})

async function loadDepts() {
  loading.value = true
  try {
    const res = await getDeptTreeApi()
    deptTree.value = res.data
  } catch (error) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function openAddDialog(parent) {
  isEdit.value = false
  dialogTitle.value = parent ? `新增「${parent.name}」下级部门` : '新增部门'
  deptForm.id = null
  deptForm.parentId = parent ? parent.id : null
  deptForm.name = ''
  deptForm.leader = ''
  deptForm.phone = ''
  deptForm.sort = 0
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  dialogTitle.value = '编辑部门'
  Object.assign(deptForm, {
    id: row.id,
    parentId: row.parentId || null,
    name: row.name,
    leader: row.leader,
    phone: row.phone,
    sort: row.sort || 0
  })
  dialogVisible.value = true
}

async function saveDept() {
  const valid = await deptFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    parentId: deptForm.parentId || 0,
    name: deptForm.name,
    leader: deptForm.leader,
    phone: deptForm.phone,
    sort: deptForm.sort
  }

  try {
    if (isEdit.value) {
      await updateDeptApi(deptForm.id, payload)
    } else {
      await createDeptApi(payload)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadDepts()
  } catch (error) {
    // 拦截器已提示
  }
}

async function deleteDept(row) {
  try {
    await ElMessageBox.confirm(`确定要删除部门「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteDeptApi(row.id)
    ElMessage.success('删除成功')
    loadDepts()
  } catch (error) {
    // 拦截器已提示（存在下级或用户时会被拦截）
  }
}

function filterTree(nodes, excludeId) {
  return nodes
    .filter(node => node.id !== excludeId)
    .map(node => ({
      ...node,
      children: node.children ? filterTree(node.children, excludeId) : []
    }))
}
</script>

<style scoped>
.tip {
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
}
</style>

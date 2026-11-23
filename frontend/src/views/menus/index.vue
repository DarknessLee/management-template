<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">菜单管理</h2>
      <el-button v-permission="'menu:edit'" type="primary" @click="openAddDialog">
        <el-icon><component :is="icons.Plus" /></el-icon>
        新增菜单
      </el-button>
    </div>

    <div class="card">
      <el-table
        v-loading="loading"
        :data="menus"
        border
        stripe
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column label="菜单名称" min-width="160">
          <template #default="scope">
            <span class="menu-title">
              <el-icon><component :is="icons[scope.row.icon] || icons.Menu" /></el-icon>
              {{ scope.row.title }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.component ? 'success' : 'info'" size="small">
              {{ scope.row.component ? '菜单' : '目录' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" width="150" />
        <el-table-column prop="component" label="组件路径" min-width="220" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.component || '—' }}</template>
        </el-table-column>
        <el-table-column prop="permission" label="权限标识" width="140">
          <template #default="scope">{{ scope.row.permission || '—' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="显示" width="90">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.visible"
              :active-value="1"
              :inactive-value="0"
              :disabled="!canEdit"
              @change="value => toggleVisible(scope.row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column v-if="canEdit" label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteMenu(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="tip">菜单最多两级：组件路径留空即为目录，用于侧边栏分组；保存后刷新页面生效。</p>
    </div>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="560px">
      <el-form ref="menuFormRef" :model="menuForm" :rules="formRules" label-width="100px">
        <el-form-item label="上级菜单">
          <el-select v-model="menuForm.parentId" style="width: 100%;">
            <el-option :value="0" label="顶级菜单（目录）" />
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单名称" prop="title">
          <el-input v-model="menuForm.title" placeholder="如 用户管理" />
        </el-form-item>
        <el-form-item label="路由路径" prop="path">
          <el-input v-model="menuForm.path" placeholder="如 /users，目录如 /system" />
        </el-form-item>
        <el-form-item label="组件路径" prop="component">
          <el-input v-model="menuForm.component" placeholder="目录留空；菜单如 views/users/index.vue" />
        </el-form-item>
        <el-form-item label="图标">
          <el-select
            v-model="menuForm.icon"
            filterable
            allow-create
            clearable
            default-first-option
            placeholder="选择或输入图标名"
            style="width: 100%;"
          >
            <el-option v-for="name in iconOptions" :key="name" :label="name" :value="name" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限标识">
          <el-select
            v-model="menuForm.permission"
            filterable
            allow-create
            clearable
            default-first-option
            placeholder="为空表示登录即可见"
            style="width: 100%;"
          >
            <el-option
              v-for="item in permissionOptions"
              :key="item.code"
              :label="`${item.code}（${item.name}）`"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="menuForm.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="显示">
          <el-switch v-model="menuForm.visible" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMenu">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Odometer,
  User,
  UserFilled,
  Operation,
  Key,
  Document,
  Clock,
  Bell,
  Message,
  Collection,
  Menu,
  Monitor,
  Connection,
  OfficeBuilding,
  Timer,
  Setting,
  Tickets,
  DataLine,
  ChatDotRound
} from '@element-plus/icons-vue'
import {
  getMenusApi,
  createMenuApi,
  updateMenuApi,
  deleteMenuApi
} from '@/api/menu'
import { getPermissionsApi } from '@/api/permission'
import { useUserStore } from '@/stores/user'

const icons = {
  Plus,
  Odometer,
  User,
  UserFilled,
  Operation,
  Key,
  Document,
  Clock,
  Bell,
  Message,
  Collection,
  Menu,
  Monitor,
  Connection,
  OfficeBuilding,
  Timer,
  Setting,
  Tickets,
  DataLine,
  ChatDotRound
}

const iconOptions = Object.keys(icons).filter(name => name !== 'Plus')

const userStore = useUserStore()
const canEdit = computed(() => userStore.permissions.includes('menu:edit'))

const menus = ref([])
const permissionOptions = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const isEdit = ref(false)
const menuFormRef = ref(null)

const menuForm = reactive({
  id: null,
  parentId: 0,
  title: '',
  path: '',
  component: '',
  icon: 'Menu',
  permission: '',
  sort: 0,
  visible: 1
})

// 只有目录可以作为上级菜单，编辑时排除自己
const parentOptions = computed(() => menus.value.filter(
  item => !item.component && item.id !== menuForm.id
))

const formRules = {
  title: [
    { required: true, message: '请输入菜单名称', trigger: 'blur' }
  ],
  path: [
    { required: true, message: '请输入路由路径', trigger: 'blur' },
    {
      pattern: /^\/[a-zA-Z0-9\-_/]*$/,
      message: '路由路径应以 / 开头，只能包含字母、数字、- 和 _',
      trigger: 'blur'
    }
  ],
  component: [
    {
      validator: (rule, value, callback) => {
        if (menuForm.parentId && !value) {
          callback(new Error('子菜单必须填写组件路径'))
          return
        }
        if (value && !/^views\/[a-zA-Z0-9\-_/]+\.vue$/.test(value)) {
          callback(new Error('格式应为 views/模块/index.vue'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  loadMenus()
  loadPermissionOptions()
})

async function loadMenus() {
  loading.value = true
  try {
    const res = await getMenusApi()
    menus.value = res.data
  } catch (error) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function loadPermissionOptions() {
  try {
    const res = await getPermissionsApi()
    permissionOptions.value = res.data
  } catch (error) {
    permissionOptions.value = []
  }
}

function openAddDialog() {
  isEdit.value = false
  dialogTitle.value = '新增菜单'
  Object.assign(menuForm, {
    id: null,
    parentId: 0,
    title: '',
    path: '',
    component: '',
    icon: 'Menu',
    permission: '',
    sort: 0,
    visible: 1
  })
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  dialogTitle.value = '编辑菜单'
  Object.assign(menuForm, {
    id: row.id,
    parentId: row.parentId || 0,
    title: row.title,
    path: row.path,
    component: row.component || '',
    icon: row.icon || '',
    permission: row.permission || '',
    sort: row.sort || 0,
    visible: row.visible
  })
  dialogVisible.value = true
}

async function saveMenu() {
  const valid = await menuFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    parentId: menuForm.parentId,
    title: menuForm.title,
    path: menuForm.path,
    component: menuForm.component,
    icon: menuForm.icon,
    permission: menuForm.permission,
    sort: menuForm.sort,
    visible: menuForm.visible
  }

  try {
    if (isEdit.value) {
      await updateMenuApi(menuForm.id, payload)
    } else {
      await createMenuApi(payload)
    }
    ElMessage.success('保存成功，刷新页面后菜单生效')
    dialogVisible.value = false
    loadMenus()
  } catch (error) {
    // 拦截器已提示
  }
}

async function toggleVisible(row, value) {
  try {
    await updateMenuApi(row.id, {
      parentId: row.parentId || 0,
      title: row.title,
      path: row.path,
      component: row.component,
      icon: row.icon,
      permission: row.permission,
      sort: row.sort,
      visible: value
    })
    ElMessage.success(value === 1 ? '已显示' : '已隐藏')
  } catch (error) {
    // 拦截器已提示
  }
  loadMenus()
}

async function deleteMenu(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除菜单「${row.title}」吗？删除后对应路由将不可访问。`,
      '提示',
      { type: 'warning' }
    )
  } catch (error) {
    return
  }

  try {
    await deleteMenuApi(row.id)
    ElMessage.success('删除成功')
    loadMenus()
  } catch (error) {
    // 拦截器已提示
  }
}
</script>

<style scoped>
.menu-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tip {
  margin-top: 12px;
  font-size: 13px;
  color: var(--app-text-secondary);
}
</style>

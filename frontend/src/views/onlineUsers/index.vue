<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">在线用户</h2>
      <el-button :loading="loading" @click="loadOnlineUsers">
        <el-icon><component :is="icons.Refresh" /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="card">
      <el-table v-loading="loading" :data="onlineUsers" border stripe>
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="姓名" width="150" />
        <el-table-column label="角色" width="130">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.role || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP 地址" width="170" />
        <el-table-column prop="loginTime" label="最近登录时间" width="180" />
        <el-table-column prop="sessionCount" label="会话数" width="100" />
        <el-table-column label="操作" width="140">
          <template #default="scope">
            <el-button
              v-if="canKick && scope.row.userId !== currentUserId"
              size="small"
              type="danger"
              @click="kickUser(scope.row)"
            >
              强制下线
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="tip">同一用户多次登录会合并为一行，强制下线会清除该用户的全部会话。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getOnlineUsersApi } from '@/api/online'
import { kickoutUserApi } from '@/api/user'
import { useUserStore } from '@/stores/user'

const icons = { Refresh }

const userStore = useUserStore()
const canKick = computed(() => userStore.permissions.includes('users:edit'))
const currentUserId = computed(() => userStore.userInfo?.id)

const onlineUsers = ref([])
const loading = ref(false)

onMounted(() => {
  loadOnlineUsers()
})

async function loadOnlineUsers() {
  loading.value = true
  try {
    const res = await getOnlineUsersApi()
    onlineUsers.value = res.data
  } catch (error) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function kickUser(row) {
  try {
    await ElMessageBox.confirm(
      `确定要强制「${row.username}」下线吗？该用户的全部会话都会被清除。`,
      '提示',
      { type: 'warning' }
    )
  } catch (error) {
    return
  }

  try {
    const res = await kickoutUserApi(row.userId)
    ElMessage.success(`已清理 ${res.data} 个会话`)
    loadOnlineUsers()
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
</style>

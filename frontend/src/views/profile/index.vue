<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">个人中心</h2>
    </div>

    <el-row :gutter="20">
      <el-col :span="7">
        <div class="card profile-card">
          <el-upload
            class="avatar-upload"
            :show-file-list="false"
            :http-request="handleAvatarUpload"
            accept="image/png,image/jpeg,image/gif,image/webp"
          >
            <el-avatar :size="96" :src="assetUrl(profile.avatar)">
              <el-icon :size="32"><component :is="icons.UserFilled" /></el-icon>
            </el-avatar>
            <div class="avatar-tip">点击更换头像</div>
          </el-upload>
          <p class="profile-name">{{ profile.realName || profile.username }}</p>
          <p class="profile-account">账号：{{ profile.username }}</p>
        </div>
      </el-col>

      <el-col :span="17">
        <div class="card">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="基本资料" name="basic">
              <el-form
                ref="profileFormRef"
                :model="profileForm"
                :rules="profileRules"
                label-width="90px"
                class="form-width"
              >
                <el-form-item label="账号">
                  <el-input :model-value="profile.username" disabled />
                </el-form-item>
                <el-form-item label="姓名" prop="realName">
                  <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="password">
              <el-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-width="90px"
                class="form-width"
              >
                <el-form-item label="原密码" prop="oldPassword">
                  <el-input v-model="passwordForm.oldPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="passwordForm.newPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="changing" @click="savePassword">修改密码</el-button>
                </el-form-item>
              </el-form>
              <p class="password-tip">修改成功后所有已登录设备都会失效，需要重新登录。</p>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { assetUrl } from '@/utils/asset'
import { getProfileApi, updateProfileApi, changePasswordApi, uploadAvatarApi } from '@/api/profile'

const router = useRouter()
const userStore = useUserStore()

const icons = { UserFilled }
const activeTab = ref('basic')
const saving = ref(false)
const changing = ref(false)
const profileFormRef = ref(null)
const passwordFormRef = ref(null)

const profile = ref({
  username: '',
  realName: '',
  email: '',
  phone: '',
  avatar: ''
})

const profileForm = reactive({
  realName: '',
  email: '',
  phone: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
  phone: [{ pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '新密码长度需在 6-32 位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  loadProfile()
})

async function loadProfile() {
  const res = await getProfileApi()
  Object.assign(profile.value, res.data)
  profileForm.realName = res.data.realName || ''
  profileForm.email = res.data.email || ''
  profileForm.phone = res.data.phone || ''
}

async function saveProfile() {
  try {
    await profileFormRef.value.validate()
  } catch (error) {
    return
  }
  saving.value = true
  try {
    const res = await updateProfileApi({ ...profileForm })
    Object.assign(profile.value, res.data)
    // 同步刷新 store，让右上角头像和姓名立即更新
    await userStore.getUserInfo()
    ElMessage.success('资料已保存')
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  try {
    await passwordFormRef.value.validate()
  } catch (error) {
    return
  }
  changing.value = true
  try {
    await changePasswordApi({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码已修改，请重新登录')
    userStore.clearAuth()
    router.push('/login')
  } finally {
    changing.value = false
  }
}

async function handleAvatarUpload(options) {
  const file = options.file
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('头像大小不能超过 2MB')
    return
  }
  const formData = new FormData()
  formData.append('file', file)
  const res = await uploadAvatarApi(formData)
  profile.value.avatar = res.data
  await userStore.getUserInfo()
  ElMessage.success('头像已更新')
}
</script>

<style scoped>
.profile-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
}

.avatar-upload {
  text-align: center;
  cursor: pointer;
}

.avatar-tip {
  margin-top: 10px;
  font-size: 13px;
  color: #909399;
}

.profile-name {
  margin: 18px 0 6px;
  font-size: 18px;
  font-weight: 600;
}

.profile-account {
  color: #909399;
  font-size: 13px;
}

.form-width {
  max-width: 420px;
  margin-top: 10px;
}

.password-tip {
  margin-left: 90px;
  font-size: 12px;
  color: #909399;
}
</style>

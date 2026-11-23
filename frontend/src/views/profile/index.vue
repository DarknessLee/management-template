<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('profile.title') }}</h2>
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
            <div class="avatar-tip">{{ t('profile.changeAvatar') }}</div>
          </el-upload>
          <p class="profile-name">{{ profile.realName || profile.username }}</p>
          <p class="profile-account">{{ t('profile.account', { name: profile.username }) }}</p>
        </div>
      </el-col>

      <el-col :span="17">
        <div class="card">
          <el-tabs v-model="activeTab">
            <el-tab-pane :label="t('profile.basicTab')" name="basic">
              <el-form
                ref="profileFormRef"
                :model="profileForm"
                :rules="profileRules"
                label-width="90px"
                class="form-width"
              >
                <el-form-item :label="t('profile.accountLabel')">
                  <el-input :model-value="profile.username" disabled />
                </el-form-item>
                <el-form-item :label="t('profile.realName')" prop="realName">
                  <el-input v-model="profileForm.realName" :placeholder="t('profile.realNamePlaceholder')" />
                </el-form-item>
                <el-form-item :label="t('profile.email')" prop="email">
                  <el-input v-model="profileForm.email" :placeholder="t('profile.emailPlaceholder')" />
                </el-form-item>
                <el-form-item :label="t('profile.phone')" prop="phone">
                  <el-input v-model="profileForm.phone" :placeholder="t('profile.phonePlaceholder')" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="saving" @click="saveProfile">{{ t('profile.save') }}</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane :label="t('profile.passwordTab')" name="password">
              <el-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-width="90px"
                class="form-width"
              >
                <el-form-item :label="t('profile.oldPassword')" prop="oldPassword">
                  <el-input v-model="passwordForm.oldPassword" type="password" show-password />
                </el-form-item>
                <el-form-item :label="t('profile.newPassword')" prop="newPassword">
                  <el-input v-model="passwordForm.newPassword" type="password" show-password />
                </el-form-item>
                <el-form-item :label="t('profile.confirmPassword')" prop="confirmPassword">
                  <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="changing" @click="savePassword">{{ t('profile.changePassword') }}</el-button>
                </el-form-item>
              </el-form>
              <p class="password-tip">{{ t('profile.passwordTip') }}</p>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { assetUrl } from '@/utils/asset'
import { getProfileApi, updateProfileApi, changePasswordApi, uploadAvatarApi } from '@/api/profile'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()

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

const profileRules = computed(() => ({
  realName: [{ required: true, message: t('profile.realNameRequired'), trigger: 'blur' }],
  email: [{ type: 'email', message: t('profile.emailInvalid'), trigger: 'blur' }],
  phone: [{ pattern: /^1\d{10}$/, message: t('profile.phoneInvalid'), trigger: 'blur' }]
}))

const passwordRules = computed(() => ({
  oldPassword: [{ required: true, message: t('profile.oldPasswordRequired'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: t('profile.newPasswordRequired'), trigger: 'blur' },
    { min: 6, max: 32, message: t('profile.newPasswordLength'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: t('profile.confirmRequired'), trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error(t('profile.confirmMismatch')))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}))

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
    ElMessage.success(t('profile.profileSaved'))
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
    ElMessage.success(t('profile.passwordChanged'))
    userStore.clearAuth()
    router.push('/login')
  } finally {
    changing.value = false
  }
}

async function handleAvatarUpload(options) {
  const file = options.file
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error(t('profile.avatarSize'))
    return
  }
  const formData = new FormData()
  formData.append('file', file)
  const res = await uploadAvatarApi(formData)
  profile.value.avatar = res.data
  await userStore.getUserInfo()
  ElMessage.success(t('profile.avatarUpdated'))
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
  color: var(--app-text-secondary);
}

.profile-name {
  margin: 18px 0 6px;
  font-size: 18px;
  font-weight: 600;
}

.profile-account {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.form-width {
  max-width: 420px;
  margin-top: 10px;
}

.password-tip {
  margin-left: 90px;
  font-size: 12px;
  color: var(--app-text-secondary);
}
</style>

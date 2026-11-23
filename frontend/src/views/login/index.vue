<template>
  <div class="login-container">
    <div class="login-wrapper">
      <div class="login-header">
        <img
          v-if="siteStore.config.siteLogo"
          :src="siteStore.config.siteLogo"
          class="login-logo"
          alt="logo"
        />
        <h1>{{ siteStore.config.siteTitle }}</h1>
        <p>{{ t('login.welcome') }}</p>
      </div>
      <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-width="0px" class="login-form">
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            type="text" 
            :placeholder="t('login.usernamePlaceholder')"
            :prefix-icon="icons.User"
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            :placeholder="t('login.passwordPlaceholder')"
            :prefix-icon="icons.Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input
              v-model="loginForm.captchaCode"
              type="text"
              :placeholder="t('login.captchaPlaceholder')"
              size="large"
              maxlength="4"
              @keyup.enter="handleLogin"
            />
            <img
              v-if="captchaImage"
              :src="captchaImage"
              class="captcha-img"
              :alt="t('login.captchaAlt')"
              :title="t('login.captchaRefresh')"
              @click="loadCaptcha"
            />
            <div v-else class="captcha-img captcha-placeholder" @click="loadCaptcha">{{ t('common.loading') }}</div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="loginForm.rememberMe">{{ t('login.rememberMe') }}</el-checkbox>
        </el-form-item>
        <!-- 让回车也触发登录事件 -->
        <el-form-item>
          <el-button 
            type="primary" 
            size="large" 
            :loading="loading"
            @click="handleLogin"
            class="login-btn"
          >
            {{ t('login.submit') }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-tips">
        <p>{{ t('login.testAccounts') }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { useSiteStore } from '@/stores/site'
import { getCaptchaApi } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()
const siteStore = useSiteStore()
const { t } = useI18n()

// 局部注册图标组件，供 el-input 的 prefix-icon 使用
const icons = { User, Lock }

const loginFormRef = ref(null)
const loading = ref(false)
const captchaImage = ref('')
const captchaKey = ref('')

const loginForm = reactive({
  username: '',
  password: '',
  captchaCode: '',
  rememberMe: false
})

// 用 computed 让校验提示跟随语言切换
const rules = computed(() => ({
  username: [
    { required: true, message: t('login.usernameRequired'), trigger: 'blur' },
    { min: 3, max: 20, message: t('login.usernameLength'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('login.passwordRequired'), trigger: 'blur' },
    { min: 6, max: 20, message: t('login.passwordLength'), trigger: 'blur' }
  ],
  captchaCode: [
    { required: true, message: t('login.captchaRequired'), trigger: 'blur' }
  ]
}))

onMounted(() => {
  siteStore.load()
  loadCaptcha()
})

async function loadCaptcha() {
  try {
    const res = await getCaptchaApi()
    captchaKey.value = res.data.key
    captchaImage.value = res.data.image
  } catch (error) {
    // 拦截器已提示；保留占位符让用户可点击重试
  }
}

async function handleLogin() {
  // 回车可能被连续触发，登录中直接忽略，避免重复提交
  if (loading.value) return
  // validate() 校验失败会 reject，用 catch 兜住，避免控制台出现未捕获异常
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const ok = await userStore.login(
      loginForm.username,
      loginForm.password,
      captchaKey.value,
      loginForm.captchaCode,
      loginForm.rememberMe
    )
    if (ok) {
      ElMessage.success(t('login.success'))
      router.push('/dashboard')
    } else {
      // 验证码一次性使用，失败后必须换一张，否则重试也会报"已过期"
      loginForm.captchaCode = ''
      loadCaptcha()
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-wrapper {
  width: 400px;
  padding: 40px;
  background: var(--app-bg-container);
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-logo {
  width: 56px;
  height: 56px;
  object-fit: contain;
  margin-bottom: 10px;
}

.login-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: var(--app-text-primary);
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: var(--app-text-secondary);
}

.login-form {
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 4px;
  cursor: pointer;
  flex-shrink: 0;
}

.captcha-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-hover-bg);
  color: var(--app-text-secondary);
  font-size: 12px;
}

.login-tips {
  text-align: center;
}

.login-tips p {
  font-size: 12px;
  color: var(--app-text-placeholder);
}
</style>
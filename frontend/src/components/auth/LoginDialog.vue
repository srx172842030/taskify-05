<script setup lang="ts">
import { ref, watch } from 'vue'
import { useField, useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { z } from 'zod'
import { RefreshCw, X } from 'lucide-vue-next'
import { useAuth } from '@/stores/auth'
import { authApi, type CaptchaResult } from '@/services/auth'

const open = defineModel<boolean>({ default: false })
const auth = useAuth()

const mode = ref<'login' | 'register'>('login')
const captcha = ref<CaptchaResult | null>(null)
const error = ref('')
const loading = ref(false)

const schema = toTypedSchema(
  z.object({
    username: z.string().min(3, '用户名至少 3 位'),
    password: z
      .string()
      .min(8, '密码至少 8 位')
      .regex(/^(?=.*[A-Za-z])(?=.*\d)/, '密码须含字母与数字'),
    captchaCode: z.string().min(1, '请输入验证码'),
  }),
)

const { handleSubmit, resetForm } = useForm({ validationSchema: schema })
const { value: username, errorMessage: usernameError } = useField<string>('username')
const { value: password, errorMessage: passwordError } = useField<string>('password')
const { value: captchaCode, errorMessage: captchaError } = useField<string>('captchaCode')

async function loadCaptcha() {
  captcha.value = await authApi.captcha()
}

watch(open, (v) => {
  if (v) {
    mode.value = 'login'
    resetForm()
    error.value = ''
    loadCaptcha()
  }
})

const onSubmit = handleSubmit(async (values) => {
  if (!captcha.value) return
  loading.value = true
  error.value = ''
  try {
    const payload = { ...values, captchaId: captcha.value.captchaId }
    if (mode.value === 'login') await auth.login(payload)
    else await auth.register(payload)
    open.value = false
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
    loadCaptcha()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <Transition name="fade">
    <div
      v-if="open"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      @click.self="open = false"
    >
      <div class="w-full max-w-sm rounded-lg border bg-card p-6 shadow-lg">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold">{{ mode === 'login' ? '登录' : '注册' }}</h2>
          <button aria-label="关闭" @click="open = false">
            <X class="h-5 w-5" />
          </button>
        </div>

        <form class="mt-4 space-y-3" @submit="onSubmit">
          <div>
            <input
              v-model="username"
              type="text"
              placeholder="用户名"
              class="w-full rounded-md border bg-background px-3 py-2 text-sm"
            />
            <p v-if="usernameError" class="mt-1 text-xs text-destructive">{{ usernameError }}</p>
          </div>

          <div>
            <input
              v-model="password"
              type="password"
              placeholder="密码"
              class="w-full rounded-md border bg-background px-3 py-2 text-sm"
            />
            <p v-if="passwordError" class="mt-1 text-xs text-destructive">{{ passwordError }}</p>
          </div>

          <div class="flex items-center gap-2">
            <input
              v-model="captchaCode"
              type="text"
              placeholder="验证码"
              class="w-full rounded-md border bg-background px-3 py-2 text-sm"
            />
            <button
              type="button"
              class="shrink-0 overflow-hidden rounded-md border"
              aria-label="刷新验证码"
              @click="loadCaptcha"
            >
              <img v-if="captcha" :src="captcha.image" alt="验证码" class="h-9 w-24" />
              <RefreshCw v-else class="h-9 w-24 p-2" />
            </button>
          </div>
          <p v-if="captchaError" class="text-xs text-destructive">{{ captchaError }}</p>

          <p v-if="error" class="text-xs text-destructive">{{ error }}</p>

          <button
            type="submit"
            :disabled="loading"
            class="w-full rounded-md bg-primary py-2 text-sm text-primary-foreground disabled:opacity-60"
          >
            {{ loading ? '提交中…' : mode === 'login' ? '登录' : '注册' }}
          </button>
        </form>

        <p class="mt-3 text-center text-sm text-muted-foreground">
          {{ mode === 'login' ? '没有账号？' : '已有账号？' }}
          <button
            class="text-primary underline"
            @click="mode = mode === 'login' ? 'register' : 'login'"
          >
            {{ mode === 'login' ? '去注册' : '去登录' }}
          </button>
        </p>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

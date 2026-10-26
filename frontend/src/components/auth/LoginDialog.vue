<script setup lang="ts">
import { ref, watch } from 'vue'
import { useField, useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { z } from 'zod'
import { RefreshCw } from 'lucide-vue-next'
import { useAuth } from '@/stores/auth'
import { authApi, type CaptchaResult } from '@/services/auth'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'

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
  <Dialog v-model:open="open">
    <DialogContent class="max-w-sm">
      <DialogHeader>
        <DialogTitle>{{ mode === 'login' ? '登录' : '注册' }}</DialogTitle>
        <DialogDescription class="sr-only">
          {{ mode === 'login' ? '登录到你的账户' : '创建一个新账户' }}
        </DialogDescription>
      </DialogHeader>

      <form class="space-y-3" @submit="onSubmit">
        <div class="space-y-1">
          <Label for="login-username">用户名</Label>
          <Input id="login-username" v-model="username" type="text" placeholder="用户名" />
          <p v-if="usernameError" class="text-xs text-destructive">{{ usernameError }}</p>
        </div>

        <div class="space-y-1">
          <Label for="login-password">密码</Label>
          <Input id="login-password" v-model="password" type="password" placeholder="密码" />
          <p v-if="passwordError" class="text-xs text-destructive">{{ passwordError }}</p>
        </div>

        <div class="space-y-1">
          <Label for="login-captcha">验证码</Label>
          <div class="flex items-center gap-2">
            <Input id="login-captcha" v-model="captchaCode" type="text" placeholder="验证码" />
            <Button
              type="button"
              variant="outline"
              class="h-9 w-24 shrink-0 overflow-hidden p-0"
              aria-label="刷新验证码"
              @click="loadCaptcha"
            >
              <img v-if="captcha" :src="captcha.image" alt="验证码" class="h-9 w-24" />
              <RefreshCw v-else class="h-9 w-24 p-2" />
            </Button>
          </div>
          <p v-if="captchaError" class="text-xs text-destructive">{{ captchaError }}</p>
        </div>

        <p v-if="error" class="text-xs text-destructive">{{ error }}</p>

        <Button type="submit" :disabled="loading" class="w-full">
          {{ loading ? '提交中…' : mode === 'login' ? '登录' : '注册' }}
        </Button>
      </form>

      <p class="text-center text-sm text-muted-foreground">
        {{ mode === 'login' ? '没有账号？' : '已有账号？' }}
        <Button
          variant="link"
          class="h-auto p-0 align-baseline"
          @click="mode = mode === 'login' ? 'register' : 'login'"
        >
          {{ mode === 'login' ? '去注册' : '去登录' }}
        </Button>
      </p>
    </DialogContent>
  </Dialog>
</template>

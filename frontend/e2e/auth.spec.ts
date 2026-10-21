import { test, expect } from '@playwright/test'

// 完整注册/登录/登出流程需后端以 `taskify.captcha.enabled=false` 启动（测试后门），
// 否则图形验证码无法在 E2E 中自动识别。
const username = `e2e_${Date.now()}`

test('登录弹窗渲染表单', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: '登录' }).click()

  await expect(page.getByRole('heading', { name: '登录' })).toBeVisible()
  await expect(page.getByPlaceholder('用户名')).toBeVisible()
  await expect(page.getByPlaceholder('密码')).toBeVisible()
  await expect(page.getByPlaceholder('验证码')).toBeVisible()
})

test('注册 → 登出 → 登录 全流程（需后端 taskify.captcha.enabled=false 后门）', async ({ page }) => {
  await page.goto('/')

  // 注册
  await page.getByRole('button', { name: '登录' }).click()
  await page.getByRole('button', { name: '去注册' }).click()
  await page.getByPlaceholder('用户名').fill(username)
  await page.getByPlaceholder('密码').fill('secret123')
  await page.getByPlaceholder('验证码').fill('abcd')
  await page.locator('form button[type="submit"]').click()
  await expect(page.getByRole('button', { name: '登出' })).toBeVisible()

  // 登出
  await page.getByRole('button', { name: '登出' }).click()
  await expect(page.getByRole('button', { name: '登录' })).toBeVisible()

  // 登录
  await page.getByRole('button', { name: '登录' }).click()
  await page.getByPlaceholder('用户名').fill(username)
  await page.getByPlaceholder('密码').fill('secret123')
  await page.getByPlaceholder('验证码').fill('abcd')
  await page.locator('form button[type="submit"]').click()
  await expect(page.getByRole('button', { name: '登出' })).toBeVisible()
})

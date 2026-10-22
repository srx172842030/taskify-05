import { test, expect, type Page } from '@playwright/test'

// 依赖：后端以 `taskify.captcha.enabled=false` 启动并已预置种子数据（pm_user / 官网改版）。

const suffix = Date.now()

async function login(page: Page, username = 'pm_user') {
  await page.goto('/')
  await page.getByRole('button', { name: '登录' }).click()
  await page.getByPlaceholder('用户名').fill(username)
  await page.getByPlaceholder('密码').fill('taskify123')
  await page.getByPlaceholder('验证码').fill('abcd')
  await expect(page.getByAltText('验证码')).toBeVisible()
  await page.locator('form button[type="submit"]').click()
  await expect(page.getByRole('button', { name: '登出' })).toBeVisible()
}

test('任务详情抽屉展示时间与依赖编辑', async ({ page }) => {
  await login(page)
  await page.goto('/projects')
  await page.getByRole('link', { name: /官网改版/ }).first().click()

  await page.getByRole('button', { name: '新建任务' }).click()
  await page.getByPlaceholder('任务标题').fill(`排期任务 ${suffix}`)
  await page.getByRole('button', { name: '创建' }).click()

  await page.getByRole('button', { name: `排期任务 ${suffix}` }).first().click()
  await expect(page.getByRole('heading', { name: '任务详情' })).toBeVisible()
  await expect(page.getByText('开始时间')).toBeVisible()
  await expect(page.getByText('截止时间')).toBeVisible()
  await expect(page.getByText('提醒').first()).toBeVisible()
  await expect(page.getByText('阻塞关系')).toBeVisible()
})

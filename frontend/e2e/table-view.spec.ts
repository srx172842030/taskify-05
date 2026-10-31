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

test('表格视图渲染与筛选控件', async ({ page }) => {
  await login(page)
  await page.goto('/projects')
  await page.getByRole('link', { name: /官网改版/ }).first().click()

  await page.getByRole('button', { name: '表格' }).click()
  await expect(page.getByText('只看我的任务')).toBeVisible()
})

test('两视图共享数据源：表格创建任务在看板可见', async ({ page }) => {
  await login(page)
  await page.goto('/projects')
  await page.getByRole('link', { name: /官网改版/ }).first().click()

  await page.getByRole('button', { name: '表格' }).click()
  await page.getByRole('button', { name: '新建任务' }).click()

  const titleInput = page.getByPlaceholder('任务标题')
  await titleInput.fill(`表格任务 ${suffix}`)
  await page.getByRole('button', { name: '创建' }).click()
  // 等待创建完成（表单关闭）
  await expect(titleInput).toBeHidden({ timeout: 10_000 })

  // 切回看板，任务同步可见（共享数据源）
  await page.getByRole('button', { name: '看板' }).click()
  await expect(page.getByRole('button', { name: `表格任务 ${suffix}` }).first()).toBeVisible()
})

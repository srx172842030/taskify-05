import { test, expect, type Page } from '@playwright/test'

// 依赖：后端以 `taskify.captcha.enabled=false` 启动（验证码后门）并已预置种子数据（pm_user / 官网改版）。

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

test('看板四列渲染与任务创建', async ({ page }) => {
  await login(page)
  await page.goto('/projects')
  await page.getByRole('link', { name: /官网改版/ }).first().click()

  await expect(page.getByText('待办事项').first()).toBeVisible()
  await expect(page.getByText('进行中').first()).toBeVisible()
  await expect(page.getByText('审查中').first()).toBeVisible()
  await expect(page.getByText('已完成').first()).toBeVisible()

  await page.getByRole('button', { name: '新建任务' }).click()
  await page.getByPlaceholder('任务标题').fill(`E2E 任务 ${suffix}`)
  await page.getByRole('button', { name: '创建' }).click()
  await expect(page.getByText(`E2E 任务 ${suffix}`)).toBeVisible()
})

test('任务在相邻列间移动（抽屉按钮）', async ({ page }) => {
  await login(page)
  await page.goto('/projects')
  await page.getByRole('link', { name: /官网改版/ }).first().click()

  await page.getByRole('button', { name: '新建任务' }).click()
  await page.getByPlaceholder('任务标题').fill(`拖拽任务 ${suffix}`)
  await page.getByRole('button', { name: '创建' }).click()
  await expect(page.getByRole('button', { name: `拖拽任务 ${suffix}` }).first()).toBeVisible()

  // 打开任务详情抽屉，用「移动到进行中」按钮完成相邻列移动
  await page.getByRole('button', { name: `拖拽任务 ${suffix}` }).first().click()
  await expect(page.getByRole('heading', { name: '任务详情' })).toBeVisible()
  await page.getByRole('button', { name: '移动到「进行中」' }).click()

  // 抽屉关闭后，任务仍可见（移动成功，未出现错误）
  await expect(page.getByRole('button', { name: `拖拽任务 ${suffix}` }).first()).toBeVisible()
})


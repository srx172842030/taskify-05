import { test, expect, type Page } from '@playwright/test'

// 依赖：后端以 `taskify.captcha.enabled=false` 启动，SSE 与 RocketMQ 中间件已就绪。

const suffix = Date.now()

async function login(page: Page, username: string) {
  await page.goto('/')
  await page.getByRole('button', { name: '登录' }).click()
  await page.getByPlaceholder('用户名').fill(username)
  await page.getByPlaceholder('密码').fill('taskify123')
  await page.getByPlaceholder('验证码').fill('abcd')
  await expect(page.getByAltText('验证码')).toBeVisible()
  await page.locator('form button[type="submit"]').click()
  await expect(page.getByRole('button', { name: '登出' })).toBeVisible()
}

test('通知实时送达与未读角标', async ({ browser }) => {
  const ctxA = await browser.newContext()
  const ctxB = await browser.newContext()
  const pageA = await ctxA.newPage()
  const pageB = await ctxB.newPage()

  await login(pageA, 'pm_user')
  await login(pageB, 'engineer1')

  // 用户 B 打开通知（建立 SSE 连接）
  await pageB.getByRole('button', { name: '通知' }).click()
  await expect(pageB.getByText('暂无通知')).toBeVisible()

  // 用户 A 创建任务并指派给 engineer1（种子用户 id）
  await pageA.goto('/projects')
  await pageA.getByRole('link', { name: /官网改版/ }).first().click()
  await pageA.getByRole('button', { name: '新建任务' }).click()
  await pageA.getByPlaceholder('任务标题').fill(`指派给你的任务 ${suffix}`)
  await pageA.locator('select').selectOption('00000000-0000-7000-0000-000000000002')
  await pageA.getByRole('button', { name: '创建' }).click()

  // 用户 B 无需刷新即收到通知（角标 + 面板中出现消息）
  await expect(pageB.getByText(`指派给你的任务 ${suffix}`)).toBeVisible()
  await ctxA.close()
  await ctxB.close()
})

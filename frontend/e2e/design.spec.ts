import { test, expect } from '@playwright/test'

test('设计画廊在两主题下渲染组件', async ({ page }) => {
  await page.goto('/design')
  await expect(page.getByRole('heading', { name: '设计画廊' })).toBeVisible()
  await expect(page.getByRole('button', { name: '主要按钮' })).toBeVisible()

  // 切到暗色，验证画廊仍在且主题类生效
  await page.getByRole('button', { name: '切换主题' }).click()
  await page.getByRole('button', { name: '暗色' }).click()
  await expect(page.locator('html')).toHaveClass(/dark/)
  await expect(page.getByRole('button', { name: '主要按钮' })).toBeVisible()
})

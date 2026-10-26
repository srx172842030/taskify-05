import { test, expect } from '@playwright/test'

test('切换主题并持久化', async ({ page }) => {
  await page.goto('/')

  // 打开主题下拉并切到暗色
  await page.getByRole('button', { name: '切换主题' }).click()
  await page.getByRole('menuitem', { name: '暗色' }).click()
  await expect(page.locator('html')).toHaveClass(/dark/)

  // 刷新后保持暗色
  await page.reload()
  await expect(page.locator('html')).toHaveClass(/dark/)
})

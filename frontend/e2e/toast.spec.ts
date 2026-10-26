import { test, expect } from '@playwright/test'

test('toast 悬浮于视口固定位置而非占据文档流', async ({ page }) => {
  await page.goto('/design')
  await page.getByRole('button', { name: '触发 Toast' }).click()

  const toaster = page.locator('[data-sonner-toaster]')
  await expect(toaster).toHaveCount(1)
  const position = await toaster.evaluate((el) => getComputedStyle(el).position)
  expect(position).toBe('fixed')
})

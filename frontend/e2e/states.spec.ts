import { test, expect } from '@playwright/test'

test('404 状态页', async ({ page }) => {
  await page.goto('/nonexistent-page')
  await expect(page.getByText('页面不存在')).toBeVisible()
  await expect(page.getByText('404')).toBeVisible()
})

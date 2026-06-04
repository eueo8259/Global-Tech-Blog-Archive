import { expect, test } from '@playwright/test';
import { failOnConsoleError } from './helpers/consoleErrors';

test('shows the application shell without browser errors', async ({ page }) => {
  const consoleErrors = failOnConsoleError(page);

  await page.goto('/');

  await expect(page.getByText('Global Tech Blog Archive')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Initial Sources' })).toBeVisible();
  await expect(page.getByText('Netflix')).toBeVisible();

  consoleErrors.assertNoErrors();
});

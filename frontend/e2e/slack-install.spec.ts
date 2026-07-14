import { expect, test } from '@playwright/test';
import { failOnConsoleError } from './helpers/consoleErrors';

test('opens the Slack installation guide from the archive header', async ({ page }) => {
  const consoleErrors = failOnConsoleError(page);
  await page.route('**/api/companies', async (route) => route.fulfill({ json: [] }));
  await page.route('**/api/articles?**', async (route) => {
    await route.fulfill({
      json: {
        articles: [],
        page: 0,
        size: 21,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
      },
    });
  });

  await page.goto('/');
  await page.getByRole('link', { name: 'Slack Bot' }).click();

  await expect(page).toHaveURL('/slack');
  await expect(page.getByRole('heading', { name: /팀이 놓치면 안 될 기술 이야기/ })).toBeVisible();
  await expect(page.getByText('/subscribe', { exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Add to Slack' }).first()).toHaveAttribute(
    'href',
    '/api/slack/oauth/authorize',
  );

  consoleErrors.assertNoErrors();
});

test('keeps the Slack guide usable on a mobile viewport', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/slack');

  await expect(page.getByRole('heading', { name: /팀이 놓치면 안 될 기술 이야기/ })).toBeVisible();
  await expect(
    page.getByRole('heading', { name: '설치부터 구독까지, 3단계면 충분해요.' }),
  ).toBeVisible();
  await expect(page.getByRole('link', { name: 'Add to Slack' }).first()).toBeVisible();
});

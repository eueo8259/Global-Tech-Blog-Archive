import { expect, test } from '@playwright/test';
import { failOnConsoleError } from './helpers/consoleErrors';

const articlePage = {
  articles: [
    {
      id: 1,
      title: '대규모 시스템에서 API 지연 시간을 줄인 방법',
      articleUrl: 'https://example.com/article',
      category: 'BACKEND',
      publishedAt: '2026-05-14T09:30:00',
      companyKey: 'netflix',
      companyName: 'Netflix',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  hasNext: false,
};

test('shows articles returned by the API', async ({ page }) => {
  const consoleErrors = failOnConsoleError(page);
  await page.route('**/api/articles?**', async (route) => {
    await route.fulfill({ json: articlePage });
  });

  await page.goto('/');

  await expect(page.getByRole('link', { name: 'TechPort 홈' })).toBeVisible();
  await expect(page.getByRole('heading', { name: '최신 아티클' })).toBeVisible();
  await expect(page.getByRole('link', { name: /대규모 시스템/ })).toHaveAttribute(
    'target',
    '_blank',
  );
  await expect(page.getByText('Netflix')).toBeVisible();
  await expect(page.getByText('BACKEND')).toBeVisible();
  await expect(page.getByText('2026-05-14')).toBeVisible();

  consoleErrors.assertNoErrors();
});

test('shows loading state while the API response is pending', async ({ page }) => {
  await page.route('**/api/articles?**', async () => {
    await new Promise(() => undefined);
  });

  await page.goto('/');

  await expect(page.getByText('기사를 불러오는 중입니다.')).toBeVisible();
});

test('shows empty state when the API returns no articles', async ({ page }) => {
  await page.route('**/api/articles?**', async (route) => {
    await route.fulfill({ json: { ...articlePage, articles: [], totalElements: 0 } });
  });

  await page.goto('/');

  await expect(page.getByText('해당 카테고리의 기사가 없습니다.')).toBeVisible();
});

test('shows error state when the API request fails', async ({ page }) => {
  await page.route('**/api/articles?**', async (route) => {
    await route.fulfill({ status: 500, body: 'Server error' });
  });

  await page.goto('/');

  await expect(page.getByRole('alert')).toContainText('기사를 불러오지 못했습니다.');
});

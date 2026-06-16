import { expect, test, type Page } from '@playwright/test';
import { failOnConsoleError } from './helpers/consoleErrors';

const companies = [
  { companyName: 'Anthropic', companyKey: 'anthropic' },
  { companyName: 'Netflix', companyKey: 'netflix' },
  { companyName: 'OpenAI', companyKey: 'openai' },
];

const articlePage = {
  articles: [
    {
      id: 1,
      title: '대규모 시스템에서 API 지연 시간을 줄이는 방법',
      articleUrl: 'https://example.com/article',
      category: 'BACKEND',
      publishedAt: '2026-05-14T09:30:00',
      companyKey: 'netflix',
      companyName: 'Netflix',
    },
  ],
  page: 0,
  size: 21,
  totalElements: 1,
  totalPages: 1,
  hasNext: false,
};

async function mockCompanies(page: Page) {
  await page.route('**/api/companies', async (route) => {
    await route.fulfill({ json: companies });
  });
}

test('shows the prototype-style article archive without an ELSE filter', async ({ page }) => {
  const consoleErrors = failOnConsoleError(page);
  await mockCompanies(page);
  await page.route('**/api/articles?**', async (route) => {
    await route.fulfill({ json: articlePage });
  });

  await page.goto('/');

  await expect(page.getByRole('link', { name: 'TechPort 홈' })).toBeVisible();
  await expect(
    page.getByText('세계적인 기술 기업의 엔지니어링 블로그를 한곳에서 만나보세요.'),
  ).toBeVisible();
  await expect(page.getByRole('button', { name: 'ALL' })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByRole('button', { name: 'ELSE' })).toHaveCount(0);
  await expect(page.getByText('1개 아티클')).toBeVisible();
  const articleLink = page.getByRole('link', {
    name: /대규모 시스템에서 API 지연 시간을 줄이는 방법/,
  });
  await expect(articleLink).toHaveAttribute('target', '_blank');
  await expect(page.getByText('Netflix')).toBeVisible();
  await expect(page.getByText('2026-05-14')).toBeVisible();

  consoleErrors.assertNoErrors();
});

test('searches companies and combines company and category filters from page zero', async ({
  page,
}) => {
  const requestedQueries: Array<{ category: string; companyKey: string | null; page: string }> = [];
  await mockCompanies(page);
  await page.route('**/api/articles?**', async (route) => {
    const searchParams = new URL(route.request().url()).searchParams;
    requestedQueries.push({
      category: searchParams.get('category') ?? '',
      companyKey: searchParams.get('companyKey'),
      page: searchParams.get('page') ?? '',
    });
    await route.fulfill({
      json: {
        ...articlePage,
        page: Number(searchParams.get('page')),
        totalElements: 21,
        totalPages: 2,
        hasNext: searchParams.get('page') === '0',
      },
    });
  });

  await page.goto('/');
  await page.getByRole('button', { name: '다음' }).click();
  await expect(page.getByText('2 / 2 페이지')).toBeVisible();

  await page.getByRole('button', { name: '전체 회사' }).click();
  const searchInput = page.getByRole('searchbox', { name: '회사 검색' });
  await expect(searchInput).toBeFocused();
  await searchInput.fill('open');
  await expect(page.getByRole('option', { name: 'OpenAI' })).toBeVisible();
  await expect(page.getByRole('option', { name: 'Netflix' })).toHaveCount(0);
  await page.getByRole('option', { name: 'OpenAI' }).click();
  await page.getByRole('button', { name: 'AI', exact: true }).click();

  await expect(page.getByRole('button', { name: 'OpenAI' })).toBeVisible();
  expect(requestedQueries).toContainEqual({ category: 'AI', companyKey: 'openai', page: '0' });
});

test('clears the company filter with the all companies option', async ({ page }) => {
  const companyKeys: Array<string | null> = [];
  await mockCompanies(page);
  await page.route('**/api/articles?**', async (route) => {
    companyKeys.push(new URL(route.request().url()).searchParams.get('companyKey'));
    await route.fulfill({ json: articlePage });
  });

  await page.goto('/');
  await page.getByRole('button', { name: '전체 회사' }).click();
  await page.getByRole('option', { name: 'Netflix' }).click();
  await page.getByRole('button', { name: 'Netflix' }).click();
  await page.getByRole('option', { name: '전체 회사' }).click();

  await expect(page.getByRole('button', { name: '전체 회사' })).toBeVisible();
  expect(companyKeys).toContain('netflix');
  expect(companyKeys.at(-1)).toBeNull();
});

test('closes the company menu with Escape and shows no search result', async ({ page }) => {
  await mockCompanies(page);
  await page.route('**/api/articles?**', async (route) => route.fulfill({ json: articlePage }));
  await page.goto('/');

  const trigger = page.getByRole('button', { name: '전체 회사' });
  await trigger.click();
  await page.getByRole('searchbox', { name: '회사 검색' }).fill('없는 회사');
  await expect(page.getByText('검색 결과가 없습니다.')).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(page.getByRole('searchbox', { name: '회사 검색' })).toHaveCount(0);
  await expect(trigger).toHaveAttribute('aria-expanded', 'false');
});

test('shows a retry action when the company list request fails', async ({ page }) => {
  let shouldSucceed = false;
  await page.route('**/api/companies', async (route) => {
    if (!shouldSucceed) {
      await route.fulfill({ status: 500, body: 'Server error' });
      return;
    }
    await route.fulfill({ json: companies });
  });
  await page.route('**/api/articles?**', async (route) => route.fulfill({ json: articlePage }));

  await page.goto('/');
  await expect(page.getByRole('alert')).toContainText('회사 목록을 불러오지 못했습니다.');
  shouldSucceed = true;
  await page.getByRole('button', { name: '다시 시도' }).click();
  await expect(page.getByRole('button', { name: '전체 회사' })).toBeVisible();
});

test('shows loading, empty, and error article states', async ({ page }) => {
  await mockCompanies(page);
  await page.route('**/api/articles?**', async () => {
    await new Promise(() => undefined);
  });
  await page.goto('/');
  await expect(page.getByText('기사를 불러오는 중입니다.')).toBeVisible();
});

test('shows empty state for filters with no articles', async ({ page }) => {
  await mockCompanies(page);
  await page.route('**/api/articles?**', async (route) => {
    await route.fulfill({ json: { ...articlePage, articles: [], totalElements: 0 } });
  });
  await page.goto('/');
  await expect(page.getByText('선택한 필터에 해당하는 기사가 없습니다.')).toBeVisible();
});

test('shows article error state when the request fails', async ({ page }) => {
  await mockCompanies(page);
  await page.route('**/api/articles?**', async (route) => {
    await route.fulfill({ status: 500, body: 'Server error' });
  });
  await page.goto('/');
  await expect(page.getByRole('alert')).toContainText('기사를 불러오지 못했습니다.');
});

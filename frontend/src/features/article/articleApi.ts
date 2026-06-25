import type { ArticleCategoryFilter, ArticlePage } from './types';
import { buildApiUrl } from '../../shared/api/client';

interface GetArticlesParams {
  category: ArticleCategoryFilter;
  companyKey: string | null;
  page: number;
  signal?: AbortSignal;
  size: number;
}

export async function getArticles({
  category,
  companyKey,
  page,
  signal,
  size,
}: GetArticlesParams): Promise<ArticlePage> {
  const searchParams = new URLSearchParams({
    category,
    page: page.toString(),
    size: size.toString(),
  });
  if (companyKey) {
    searchParams.set('companyKey', companyKey);
  }
  const response = await fetch(buildApiUrl(`/articles?${searchParams.toString()}`), { signal });

  if (!response.ok) {
    throw new Error(`Failed to load articles: ${response.status}`);
  }

  return (await response.json()) as ArticlePage;
}

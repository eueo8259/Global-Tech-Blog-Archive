import type { ArticleCategoryFilter, ArticlePage } from './types';

interface GetArticlesParams {
  category: ArticleCategoryFilter;
  page: number;
  signal?: AbortSignal;
  size: number;
}

export async function getArticles({
  category,
  page,
  signal,
  size,
}: GetArticlesParams): Promise<ArticlePage> {
  const searchParams = new URLSearchParams({
    category,
    page: page.toString(),
    size: size.toString(),
  });
  const response = await fetch(`/api/articles?${searchParams.toString()}`, { signal });

  if (!response.ok) {
    throw new Error(`Failed to load articles: ${response.status}`);
  }

  return (await response.json()) as ArticlePage;
}

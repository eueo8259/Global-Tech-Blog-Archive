import { useEffect, useState } from 'react';
import { getArticles } from './articleApi';
import type { ArticleCategoryFilter, ArticlePage } from './types';

const emptyArticlePage: ArticlePage = {
  articles: [],
  page: 0,
  size: 21,
  totalElements: 0,
  totalPages: 0,
  hasNext: false,
};

export function useArticles(
  category: ArticleCategoryFilter,
  companyKey: string | null,
  page: number,
) {
  const [articlePage, setArticlePage] = useState<ArticlePage>(emptyArticlePage);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();

    async function loadArticles() {
      setError(null);
      setIsLoading(true);

      try {
        const articlePage = await getArticles({
          category,
          companyKey,
          page,
          signal: controller.signal,
          size: 21,
        });

        if (!controller.signal.aborted) {
          setArticlePage(articlePage);
        }
      } catch {
        if (!controller.signal.aborted) {
          setError('Failed to load articles');
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }

    void loadArticles();

    return () => controller.abort();
  }, [category, companyKey, page]);

  return { articlePage, error, isLoading };
}

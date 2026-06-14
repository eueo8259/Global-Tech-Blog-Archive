import { useEffect, useState } from 'react';
import { getArticles } from './articleApi';
import type { Article } from './types';

export function useArticles() {
  const [articles, setArticles] = useState<Article[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();

    async function loadArticles() {
      try {
        const articlePage = await getArticles({
          category: 'ALL',
          page: 0,
          signal: controller.signal,
          size: 20,
        });

        if (!controller.signal.aborted) {
          setArticles(articlePage.articles);
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
  }, []);

  return { articles, error, isLoading };
}

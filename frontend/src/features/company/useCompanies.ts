import { useEffect, useState } from 'react';
import { getCompanies } from './companyApi';
import type { Company } from './types';

export function useCompanies() {
  const [companies, setCompanies] = useState<Company[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadCompanies() {
      setError(null);
      setIsLoading(true);

      try {
        const companies = await getCompanies(controller.signal);
        if (!controller.signal.aborted) {
          setCompanies(companies);
        }
      } catch {
        if (!controller.signal.aborted) {
          setError('회사 목록을 불러오지 못했습니다.');
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }

    void loadCompanies();
    return () => controller.abort();
  }, [requestVersion]);

  return {
    companies,
    error,
    isLoading,
    retry: () => setRequestVersion((version) => version + 1),
  };
}

import type { Company } from './types';

export async function getCompanies(signal?: AbortSignal): Promise<Company[]> {
  const response = await fetch('/api/companies', { signal });

  if (!response.ok) {
    throw new Error(`Failed to load companies: ${response.status}`);
  }

  return (await response.json()) as Company[];
}

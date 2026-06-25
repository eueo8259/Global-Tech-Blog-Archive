import type { Company } from './types';
import { buildApiUrl } from '../../shared/api/client';

export async function getCompanies(signal?: AbortSignal): Promise<Company[]> {
  const response = await fetch(buildApiUrl('/companies'), { signal });

  if (!response.ok) {
    throw new Error(`Failed to load companies: ${response.status}`);
  }

  return (await response.json()) as Company[];
}

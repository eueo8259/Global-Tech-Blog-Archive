import { useEffect, useMemo, useRef, useState } from 'react';
import type { Company, CompanyFilterValue } from './types';

interface CompanyFilterProps {
  companies: Company[];
  error: string | null;
  isLoading: boolean;
  onRetry: () => void;
  onSelect: (companyKey: CompanyFilterValue) => void;
  selectedCompanyKey: CompanyFilterValue;
}

export function CompanyFilter({
  companies,
  error,
  isLoading,
  onRetry,
  onSelect,
  selectedCompanyKey,
}: CompanyFilterProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const rootRef = useRef<HTMLDivElement>(null);
  const searchRef = useRef<HTMLInputElement>(null);
  const selectedCompany = companies.find((company) => company.companyKey === selectedCompanyKey);
  const filteredCompanies = useMemo(() => {
    const normalizedSearchTerm = searchTerm.trim().toLocaleLowerCase();
    if (!normalizedSearchTerm) {
      return companies;
    }
    return companies.filter((company) =>
      company.companyName.toLocaleLowerCase().includes(normalizedSearchTerm),
    );
  }, [companies, searchTerm]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    searchRef.current?.focus();

    function handlePointerDown(event: PointerEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setIsOpen(false);
        setSearchTerm('');
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsOpen(false);
        setSearchTerm('');
      }
    }

    document.addEventListener('pointerdown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  function selectCompany(companyKey: CompanyFilterValue) {
    onSelect(companyKey);
    setIsOpen(false);
    setSearchTerm('');
  }

  if (error) {
    return (
      <div className="company-filter-error" role="alert">
        <span>{error}</span>
        <button onClick={onRetry} type="button">
          다시 시도
        </button>
      </div>
    );
  }

  return (
    <div className="company-filter" ref={rootRef}>
      <button
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        className="company-filter-trigger"
        disabled={isLoading}
        onClick={() => setIsOpen((open) => !open)}
        type="button"
      >
        <span>
          {isLoading ? '회사 불러오는 중...' : (selectedCompany?.companyName ?? '전체 회사')}
        </span>
        <span aria-hidden="true" className="company-filter-chevron">
          {isOpen ? '▲' : '▼'}
        </span>
      </button>

      {isOpen && (
        <div className="company-filter-menu">
          <input
            aria-label="회사 검색"
            className="company-filter-search"
            onChange={(event) => setSearchTerm(event.target.value)}
            placeholder="회사 검색..."
            ref={searchRef}
            type="search"
            value={searchTerm}
          />
          <div aria-label="회사 목록" className="company-filter-options" role="listbox">
            <button
              aria-selected={selectedCompanyKey === null}
              className="company-filter-option"
              onClick={() => selectCompany(null)}
              role="option"
              type="button"
            >
              전체 회사
            </button>
            {filteredCompanies.map((company) => (
              <button
                aria-selected={selectedCompanyKey === company.companyKey}
                className="company-filter-option"
                key={company.companyKey}
                onClick={() => selectCompany(company.companyKey)}
                role="option"
                type="button"
              >
                {company.companyName}
              </button>
            ))}
            {filteredCompanies.length === 0 && (
              <p className="company-filter-empty">검색 결과가 없습니다.</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

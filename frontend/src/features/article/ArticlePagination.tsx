import { useEffect, useRef, useState } from 'react';

interface ArticlePaginationProps {
  currentPage: number;
  hasNext: boolean;
  onPageChange: (page: number) => void;
  totalPages: number;
}

export function ArticlePagination({
  currentPage,
  hasNext,
  onPageChange,
  totalPages,
}: ArticlePaginationProps) {
  const [isOpen, setIsOpen] = useState(false);
  const pickerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    function handlePointerDown(event: PointerEvent) {
      if (!pickerRef.current?.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsOpen(false);
      }
    }

    document.addEventListener('pointerdown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  if (totalPages <= 1) {
    return null;
  }

  function selectPage(page: number) {
    onPageChange(page);
    setIsOpen(false);
  }

  return (
    <nav className="article-pagination" aria-label="기사 페이지 이동">
      <button
        className="pagination-text-button"
        disabled={currentPage === 0}
        onClick={() => onPageChange(currentPage - 1)}
        type="button"
      >
        Prev
      </button>
      <div className="pagination-page-picker" ref={pickerRef}>
        <button
          aria-expanded={isOpen}
          aria-haspopup="listbox"
          aria-label="페이지 선택"
          className="pagination-page-trigger"
          onClick={() => setIsOpen((open) => !open)}
          type="button"
        >
          <span>{currentPage + 1}</span>
          <span aria-hidden="true" className="pagination-page-chevron">
            ▾
          </span>
        </button>
        {isOpen && (
          <div className="pagination-page-menu" role="listbox" aria-label="페이지 목록">
            {Array.from({ length: totalPages }, (_, index) => (
              <button
                aria-selected={currentPage === index}
                className="pagination-page-option"
                key={index}
                onClick={() => selectPage(index)}
                role="option"
                type="button"
              >
                {index + 1}
              </button>
            ))}
          </div>
        )}
      </div>
      <span className="pagination-total" aria-live="polite">
        of {totalPages}
      </span>
      <button
        className="pagination-text-button"
        disabled={!hasNext}
        onClick={() => onPageChange(currentPage + 1)}
        type="button"
      >
        Next
      </button>
    </nav>
  );
}

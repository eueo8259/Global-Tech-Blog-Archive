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
  if (totalPages <= 1) {
    return null;
  }

  return (
    <nav className="article-pagination" aria-label="기사 페이지 이동">
      <button
        className="pagination-button"
        disabled={currentPage === 0}
        onClick={() => onPageChange(currentPage - 1)}
        type="button"
      >
        이전
      </button>
      <span className="pagination-status" aria-live="polite">
        {currentPage + 1} / {totalPages} 페이지
      </span>
      <button
        className="pagination-button"
        disabled={!hasNext}
        onClick={() => onPageChange(currentPage + 1)}
        type="button"
      >
        다음
      </button>
    </nav>
  );
}

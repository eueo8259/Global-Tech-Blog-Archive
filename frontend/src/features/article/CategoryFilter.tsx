import type { ArticleCategoryFilter } from './types';

const categories: ArticleCategoryFilter[] = [
  'ALL',
  'FRONTEND',
  'BACKEND',
  'DEVOPS',
  'ARCHITECTURE',
  'AI',
];

const categoryLabels: Record<ArticleCategoryFilter, string> = {
  ALL: 'All articles',
  FRONTEND: 'Frontend',
  BACKEND: 'Backend',
  DEVOPS: 'DevOps',
  ARCHITECTURE: 'Architecture',
  AI: 'AI',
};

interface CategoryFilterProps {
  selectedCategory: ArticleCategoryFilter;
  onSelect: (category: ArticleCategoryFilter) => void;
}

export function CategoryFilter({ selectedCategory, onSelect }: CategoryFilterProps) {
  return (
    <div className="category-filter" aria-label="기사 카테고리">
      {categories.map((category) => (
        <button
          aria-pressed={selectedCategory === category}
          className="category-filter-button"
          key={category}
          onClick={() => onSelect(category)}
          type="button"
        >
          <span className="category-filter-label">{categoryLabels[category]}</span>
        </button>
      ))}
    </div>
  );
}

import { useState } from 'react';
import { ArticleList } from './features/article/ArticleList';
import { ArticlePagination } from './features/article/ArticlePagination';
import { CategoryFilter } from './features/article/CategoryFilter';
import type { ArticleCategoryFilter } from './features/article/types';
import { useArticles } from './features/article/useArticles';

function App() {
  const [selectedCategory, setSelectedCategory] = useState<ArticleCategoryFilter>('ALL');
  const [selectedPage, setSelectedPage] = useState(0);
  const { articlePage, error, isLoading } = useArticles(selectedCategory, selectedPage);

  function handleCategorySelect(category: ArticleCategoryFilter) {
    setSelectedCategory(category);
    setSelectedPage(0);
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <a className="site-logo" href="/" aria-label="TechPort 홈">
          TechPort
        </a>
      </header>

      <main className="page-content">
        <section className="article-section" aria-labelledby="article-heading">
          <div className="section-heading">
            <p className="eyebrow">GLOBAL ENGINEERING BLOGS</p>
            <h1 id="article-heading">최신 아티클</h1>
          </div>

          <CategoryFilter selectedCategory={selectedCategory} onSelect={handleCategorySelect} />
          <ArticleList articles={articlePage.articles} error={error} isLoading={isLoading} />
          {!isLoading && !error && articlePage.articles.length > 0 && (
            <ArticlePagination
              currentPage={articlePage.page}
              hasNext={articlePage.hasNext}
              onPageChange={setSelectedPage}
              totalPages={articlePage.totalPages}
            />
          )}
        </section>
      </main>
    </div>
  );
}

export default App;

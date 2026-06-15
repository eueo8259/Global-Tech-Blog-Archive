import { useState } from 'react';
import { ArticleList } from './features/article/ArticleList';
import { CategoryFilter } from './features/article/CategoryFilter';
import type { ArticleCategoryFilter } from './features/article/types';
import { useArticles } from './features/article/useArticles';

function App() {
  const [selectedCategory, setSelectedCategory] = useState<ArticleCategoryFilter>('ALL');
  const { articles, error, isLoading } = useArticles(selectedCategory);

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

          <CategoryFilter selectedCategory={selectedCategory} onSelect={setSelectedCategory} />
          <ArticleList articles={articles} error={error} isLoading={isLoading} />
        </section>
      </main>
    </div>
  );
}

export default App;

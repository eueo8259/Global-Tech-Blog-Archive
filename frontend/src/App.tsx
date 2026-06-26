import { useState } from 'react';
import { ArticleList } from './features/article/ArticleList';
import { ArticlePagination } from './features/article/ArticlePagination';
import { CategoryFilter } from './features/article/CategoryFilter';
import type { ArticleCategoryFilter } from './features/article/types';
import { useArticles } from './features/article/useArticles';
import { CompanyFilter } from './features/company/CompanyFilter';
import type { CompanyFilterValue } from './features/company/types';
import { useCompanies } from './features/company/useCompanies';

function formatArticleCount(totalElements: number) {
  const articleLabel = totalElements === 1 ? 'article' : 'articles';

  return `${totalElements} ${articleLabel}`;
}

function App() {
  const [selectedCategory, setSelectedCategory] = useState<ArticleCategoryFilter>('ALL');
  const [selectedCompanyKey, setSelectedCompanyKey] = useState<CompanyFilterValue>(null);
  const [selectedPage, setSelectedPage] = useState(0);
  const { articlePage, error, isLoading } = useArticles(
    selectedCategory,
    selectedCompanyKey,
    selectedPage,
  );
  const companies = useCompanies();

  function handleCategorySelect(category: ArticleCategoryFilter) {
    setSelectedCategory(category);
    setSelectedPage(0);
  }

  function handleCompanySelect(companyKey: CompanyFilterValue) {
    setSelectedCompanyKey(companyKey);
    setSelectedPage(0);
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="header-content">
          <a className="site-logo" href="/" aria-label="TechPort 홈">
            <img
              className="site-logo-icon"
              src="/techport-icon.png"
              alt=""
              width="40"
              height="40"
            />
            TechPort
          </a>
          <p className="site-description">
            세계적인 기술 기업의 엔지니어링 블로그를 한곳에서 만나보세요.
          </p>

          <div className="filter-bar">
            <CategoryFilter selectedCategory={selectedCategory} onSelect={handleCategorySelect} />
            <CompanyFilter
              companies={companies.companies}
              error={companies.error}
              isLoading={companies.isLoading}
              onRetry={companies.retry}
              onSelect={handleCompanySelect}
              selectedCompanyKey={selectedCompanyKey}
            />
          </div>
        </div>
      </header>

      <main className="page-content">
        <section className="article-section" aria-labelledby="article-heading">
          <h1 className="visually-hidden" id="article-heading">
            최신 아티클
          </h1>
          {!isLoading && !error && (
            <p className="article-count" aria-live="polite">
              {formatArticleCount(articlePage.totalElements)}
            </p>
          )}
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

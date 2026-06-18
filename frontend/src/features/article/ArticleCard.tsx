import type { Article } from './types';

interface ArticleCardProps {
  article: Article;
}

export function ArticleCard({ article }: ArticleCardProps) {
  return (
    <article className="article-card">
      <a
        className="article-card-link"
        href={article.articleUrl}
        target="_blank"
        rel="noopener noreferrer"
      >
        <div className="article-card-meta">
          <span className="company-name">{article.companyName}</span>
          <span className="category-badge" data-category={article.category}>
            {article.category}
          </span>
        </div>
        <h2>{article.title}</h2>
        <time dateTime={article.publishedAt}>{article.publishedAt.slice(0, 10)}</time>
      </a>
    </article>
  );
}

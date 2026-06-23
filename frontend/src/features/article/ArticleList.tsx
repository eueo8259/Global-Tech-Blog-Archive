import { ArticleCard } from './ArticleCard';
import type { Article } from './types';

interface ArticleListProps {
  articles: Article[];
  error: string | null;
  isLoading: boolean;
}

export function ArticleList({ articles, error, isLoading }: ArticleListProps) {
  if (isLoading) {
    return <p className="status-message">기사를 불러오는 중입니다.</p>;
  }

  if (error) {
    return (
      <div className="status-message" role="alert">
        <strong>기사를 불러오지 못했습니다.</strong>
        <span>잠시 후 다시 시도해 주세요.</span>
      </div>
    );
  }

  if (articles.length === 0) {
    return <p className="status-message">선택한 필터에 해당하는 기사가 없습니다.</p>;
  }

  return (
    <div className="article-grid">
      {articles.map((article) => (
        <ArticleCard article={article} key={article.id} />
      ))}
    </div>
  );
}

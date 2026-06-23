export type ArticleCategory = 'FRONTEND' | 'BACKEND' | 'DEVOPS' | 'ARCHITECTURE' | 'AI' | 'ELSE';

export type ArticleCategoryFilter = 'ALL' | Exclude<ArticleCategory, 'ELSE'>;

export interface Article {
  id: number;
  title: string;
  articleUrl: string;
  category: ArticleCategory;
  publishedAt: string;
  companyKey: string;
  companyName: string;
}

export interface ArticlePage {
  articles: Article[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

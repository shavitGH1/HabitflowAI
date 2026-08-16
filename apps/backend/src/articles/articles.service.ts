import { Injectable } from '@nestjs/common';
import { AiService } from '../ai/ai.service';
import { RankedArticle } from '../ai/features/rag-search.feature';
import { ArticleRepository } from './article.repository';

@Injectable()
export class ArticlesService {
  constructor(
    private readonly articleRepository: ArticleRepository,
    private readonly ai: AiService,
  ) {}

  async search(query: string, topK?: number): Promise<RankedArticle[]> {
    const articles = await this.articleRepository.findAll();
    return this.ai.searchArticles({ query, articles, topK });
  }
}

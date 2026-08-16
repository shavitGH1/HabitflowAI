import { Injectable } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { cosineSimilarity } from './cosine-similarity.util';

export interface ArticleCandidate {
  id: string;
  title: string;
  url: string;
  embedding: number[];
}

export interface RankedArticle {
  id: string;
  title: string;
  url: string;
  score: number;
}

export interface RagSearchInput {
  query: string;
  articles: ArticleCandidate[];
  topK?: number;
}

const DEFAULT_TOP_K = 3;

@Injectable()
export class RagSearchFeature {
  constructor(private readonly gemini: GeminiClient) {}

  async search(input: RagSearchInput): Promise<RankedArticle[]> {
    if (!input.articles.length) return [];

    const queryEmbedding = await this.gemini.embedContent(input.query);
    return input.articles
      .map(article => ({
        id: article.id,
        title: article.title,
        url: article.url,
        score: cosineSimilarity(queryEmbedding, article.embedding),
      }))
      .sort((a, b) => b.score - a.score)
      .slice(0, input.topK ?? DEFAULT_TOP_K);
  }
}

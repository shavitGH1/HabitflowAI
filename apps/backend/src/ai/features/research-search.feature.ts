import { Injectable } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { logger } from '../../logger';
import { cosineSimilarity } from './cosine-similarity.util';

export interface ResearchChunkCandidate {
  id: string;
  sourceTitle: string;
  section: string;
  content: string;
  embedding: number[];
}

export interface RankedResearchChunk {
  id: string;
  sourceTitle: string;
  section: string;
  content: string;
  score: number;
}

export interface ResearchSearchInput {
  query: string;
  chunks: ResearchChunkCandidate[];
  topK?: number;
}

const DEFAULT_TOP_K = 3;

@Injectable()
export class ResearchSearchFeature {
  constructor(private readonly gemini: GeminiClient) {}

  async search(input: ResearchSearchInput): Promise<RankedResearchChunk[]> {
    if (!input.chunks.length) return [];

    try {
      const queryEmbedding = await this.gemini.embedContent(input.query);
      return input.chunks
        .map(chunk => ({
          id: chunk.id,
          sourceTitle: chunk.sourceTitle,
          section: chunk.section,
          content: chunk.content,
          score: cosineSimilarity(queryEmbedding, chunk.embedding),
        }))
        .sort((a, b) => b.score - a.score)
        .slice(0, input.topK ?? DEFAULT_TOP_K);
    } catch (error) {
      logger.warn({ err: error }, 'research chunk search embedding failed, returning no results');
      return [];
    }
  }
}

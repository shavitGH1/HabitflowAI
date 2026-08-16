import { Injectable } from '@nestjs/common';
import { AiService } from '../ai/ai.service';
import { RankedResearchChunk } from '../ai/features/research-search.feature';
import { ResearchChunkRepository } from './research-chunk.repository';

@Injectable()
export class ResearchChunksService {
  constructor(
    private readonly researchChunkRepository: ResearchChunkRepository,
    private readonly ai: AiService,
  ) {}

  async search(query: string, topK?: number): Promise<RankedResearchChunk[]> {
    const chunks = await this.researchChunkRepository.findAll();
    return this.ai.searchResearch({ query, chunks, topK });
  }
}

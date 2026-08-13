import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { ResearchChunk, ResearchChunkDocument } from './schemas/research-chunk.schema';

export interface ResearchChunkData {
  id: string;
  sourceTitle: string;
  section: string;
  content: string;
  embedding: number[];
}

@Injectable()
export class ResearchChunkRepository {
  constructor(
    @InjectModel(ResearchChunk.name)
    private readonly researchChunkModel: Model<ResearchChunkDocument>,
  ) {}

  async findAll(): Promise<ResearchChunkData[]> {
    const docs = await this.researchChunkModel.find().exec();
    return docs.map(doc => ({
      id: (doc._id as { toString(): string }).toString(),
      sourceTitle: doc.sourceTitle,
      section: doc.section,
      content: doc.content,
      embedding: doc.embedding,
    }));
  }
}

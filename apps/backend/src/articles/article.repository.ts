import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Article, ArticleDocument } from './schemas/article.schema';

export interface ArticleData {
  id: string;
  title: string;
  url: string;
  embedding: number[];
}

@Injectable()
export class ArticleRepository {
  constructor(
    @InjectModel(Article.name) private readonly articleModel: Model<ArticleDocument>,
  ) {}

  async findAll(): Promise<ArticleData[]> {
    const docs = await this.articleModel.find().exec();
    return docs.map(doc => ({
      id: (doc._id as { toString(): string }).toString(),
      title: doc.title,
      url: doc.url,
      embedding: doc.embedding,
    }));
  }
}

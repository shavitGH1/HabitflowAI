import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from '../auth/auth.module';
import { AiModule } from '../ai/ai.module';
import { Article, ArticleSchema } from './schemas/article.schema';
import { ArticleRepository } from './article.repository';
import { ArticlesController } from './articles.controller';
import { ArticlesService } from './articles.service';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Article.name, schema: ArticleSchema }]),
    AuthModule,
    AiModule,
  ],
  providers: [ArticleRepository, ArticlesService],
  controllers: [ArticlesController],
  exports: [ArticlesService],
})
export class ArticlesModule {}

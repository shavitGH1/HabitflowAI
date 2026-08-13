import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from '../auth/auth.module';
import { AiModule } from '../ai/ai.module';
import { ResearchChunk, ResearchChunkSchema } from './schemas/research-chunk.schema';
import { ResearchChunkRepository } from './research-chunk.repository';
import { ResearchChunksController } from './research-chunks.controller';
import { ResearchChunksService } from './research-chunks.service';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: ResearchChunk.name, schema: ResearchChunkSchema }]),
    AuthModule,
    AiModule,
  ],
  providers: [ResearchChunkRepository, ResearchChunksService],
  controllers: [ResearchChunksController],
})
export class ResearchChunksModule {}

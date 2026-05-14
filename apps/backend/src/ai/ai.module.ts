import { Module } from '@nestjs/common';
import { AiService } from './ai.service';
import { GeminiClient } from './gemini.client';

@Module({
  providers: [GeminiClient, AiService],
  exports: [AiService],
})
export class AiModule {}

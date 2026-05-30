import { Module } from '@nestjs/common';
import { AiService } from './ai.service';
import { PersonaClassifierFeature } from './features/persona-classifier.feature';
import { PortfolioGeneratorFeature } from './features/portfolio-generator.feature';
import { GeminiClient } from './gemini.client';

@Module({
  providers: [GeminiClient, AiService, PersonaClassifierFeature, PortfolioGeneratorFeature],
  exports: [AiService, PersonaClassifierFeature, PortfolioGeneratorFeature],
})
export class AiModule {}

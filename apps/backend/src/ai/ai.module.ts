import { Module } from '@nestjs/common';
import { PersonaClassifierFeature } from './features/persona-classifier.feature';
import { PortfolioGeneratorFeature } from './features/portfolio-generator.feature';
import { GeminiClient } from './gemini.client';

@Module({
  providers: [GeminiClient, PersonaClassifierFeature, PortfolioGeneratorFeature],
  exports: [PersonaClassifierFeature, PortfolioGeneratorFeature],
})
export class AiModule {}

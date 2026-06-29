import { Module } from '@nestjs/common';
import { AiService } from './ai.service';
import { PersonaClassifierFeature } from './features/persona-classifier.feature';
import { PortfolioGeneratorFeature } from './features/portfolio-generator.feature';
import { DailyMotivationFeature } from './features/daily-motivation.feature';
import { PersonaDriftDetectorFeature } from './features/persona-drift-detector.feature';
import { HabitInsightsFeature } from './features/habit-insights.feature';
import { MotivationFeedbackStore } from './feedback/motivation-feedback.store';
import { GeminiClient } from './gemini.client';

@Module({
  providers: [
    GeminiClient,
    AiService,
    PersonaClassifierFeature,
    PortfolioGeneratorFeature,
    DailyMotivationFeature,
    PersonaDriftDetectorFeature,
    HabitInsightsFeature,
    MotivationFeedbackStore,
  ],
  exports: [
    AiService,
    PersonaClassifierFeature,
    PortfolioGeneratorFeature,
    DailyMotivationFeature,
    PersonaDriftDetectorFeature,
    HabitInsightsFeature,
  ],
})
export class AiModule {}

import { Module } from '@nestjs/common';
import { AiService } from './ai.service';
import { PersonaClassifierFeature } from './features/persona-classifier.feature';
import { PortfolioGeneratorFeature } from './features/portfolio-generator.feature';
import { DailyMotivationFeature } from './features/daily-motivation.feature';
import { PersonaDriftDetectorFeature } from './features/persona-drift-detector.feature';
import { HabitInsightsFeature } from './features/habit-insights.feature';
import { CoachPhrasingFeature } from './features/coach-phrasing.feature';
import { HabitGoalRelevanceFeature } from './features/habit-goal-relevance.feature';
import { TaskVerificationFeature } from './features/task-verification.feature';
import { CoachingAgentFeature } from './features/coaching-agent.feature';
import { RagSearchFeature } from './features/rag-search.feature';
import { ResearchSearchFeature } from './features/research-search.feature';
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
    CoachPhrasingFeature,
    HabitGoalRelevanceFeature,
    TaskVerificationFeature,
    CoachingAgentFeature,
    RagSearchFeature,
    ResearchSearchFeature,
    MotivationFeedbackStore,
  ],
  exports: [
    AiService,
    PersonaClassifierFeature,
    PortfolioGeneratorFeature,
    DailyMotivationFeature,
    PersonaDriftDetectorFeature,
    HabitInsightsFeature,
    CoachPhrasingFeature,
    HabitGoalRelevanceFeature,
    TaskVerificationFeature,
    CoachingAgentFeature,
  ],
})
export class AiModule {}

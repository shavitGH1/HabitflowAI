import { Module } from '@nestjs/common';
import { AiService } from './ai.service';
import { AgentLoop } from './agent/agent-loop';
import { PersonaClassifierFeature } from './features/persona-classifier.feature';
import { PortfolioGeneratorFeature } from './features/portfolio-generator.feature';
import { DailyMotivationFeature } from './features/daily-motivation.feature';
import { PersonaDriftDetectorFeature } from './features/persona-drift-detector.feature';
import { HabitInsightsFeature } from './features/habit-insights.feature';
import { CoachPhrasingFeature } from './features/coach-phrasing.feature';
import { HabitGoalRelevanceFeature } from './features/habit-goal-relevance.feature';
import { TaskVerificationFeature } from './features/task-verification.feature';
import { OnboardingSuggestionsFeature } from './features/onboarding-suggestions.feature';
import { MotivationFeedbackStore } from './feedback/motivation-feedback.store';
import { GeminiClient } from './gemini.client';

@Module({
  providers: [
    GeminiClient,
    AiService,
    AgentLoop,
    PersonaClassifierFeature,
    PortfolioGeneratorFeature,
    DailyMotivationFeature,
    PersonaDriftDetectorFeature,
    HabitInsightsFeature,
    CoachPhrasingFeature,
    HabitGoalRelevanceFeature,
    TaskVerificationFeature,
    OnboardingSuggestionsFeature,
    MotivationFeedbackStore,
  ],
  exports: [
    AiService,
    AgentLoop,
    PersonaClassifierFeature,
    PortfolioGeneratorFeature,
    DailyMotivationFeature,
    PersonaDriftDetectorFeature,
    HabitInsightsFeature,
    CoachPhrasingFeature,
    HabitGoalRelevanceFeature,
    TaskVerificationFeature,
    OnboardingSuggestionsFeature,
  ],
})
export class AiModule {}

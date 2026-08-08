import { Injectable } from '@nestjs/common';
import { GenerateGoalsResponse, GoalTask } from '../dto/goal.dto';
import { UserData } from '../users/user.repository';
import { generateDailyVariations, generateInitialGoals } from './features/daily-motivation';
import { PersonaResult, classifyPersona } from './features/persona-classifier';
import { PersonaClassifierFeature, PersonaClassifierInput } from './features/persona-classifier.feature';
import { PortfolioGeneratorFeature, PortfolioGeneratorInput } from './features/portfolio-generator.feature';
import { DailyMotivationFeature, DailyMotivationInput } from './features/daily-motivation.feature';
import {
  PersonaDriftDetectorFeature,
  DriftDetectorInput,
  DriftResult,
} from './features/persona-drift-detector.feature';
import { HabitInsightsFeature, HabitInsightsInput } from './features/habit-insights.feature';
import { CoachPhrasingFeature, CoachPhrasingInput } from './features/coach-phrasing.feature';
import {
  HabitGoalRelevanceFeature,
  HabitGoalRelevanceInput,
} from './features/habit-goal-relevance.feature';
import {
  MotivationFeedbackStore,
  MotivationVote,
  FeedbackTally,
} from './feedback/motivation-feedback.store';
import { PersonaClassifierOutput } from './schemas/persona-classifier.schema';
import { PortfolioGeneratorOutput } from './schemas/portfolio-generator.schema';
import { DailyMotivationOutput } from './schemas/daily-motivation.schema';
import { HabitInsightsOutput } from './schemas/habit-insights.schema';
import { HabitGoalRelevanceOutput } from './schemas/habit-goal-relevance.schema';
import { GeminiClient } from './gemini.client';

type GoalInput = Pick<UserData, 'goal' | 'personaType' | 'email'>;

@Injectable()
export class AiService {
  constructor(
    private readonly client: GeminiClient,
    private readonly personaClassifier: PersonaClassifierFeature,
    private readonly portfolioGenerator: PortfolioGeneratorFeature,
    private readonly dailyMotivation: DailyMotivationFeature,
    private readonly driftDetector: PersonaDriftDetectorFeature,
    private readonly habitInsights: HabitInsightsFeature,
    private readonly coachPhrasing: CoachPhrasingFeature,
    private readonly habitGoalRelevance: HabitGoalRelevanceFeature,
    private readonly feedbackStore: MotivationFeedbackStore,
  ) {}

  classifyPersona(goal: string, answers: string[]): Promise<PersonaResult> {
    return classifyPersona(this.client, goal, answers);
  }

  classifyPersonaWeighted(input: PersonaClassifierInput): Promise<PersonaClassifierOutput> {
    return this.personaClassifier.classify(input);
  }

  generatePortfolio(input: PortfolioGeneratorInput): Promise<PortfolioGeneratorOutput> {
    return this.portfolioGenerator.generate(input);
  }

  generateInitialGoals(user: GoalInput, dayOfWeek: number): Promise<GenerateGoalsResponse> {
    return generateInitialGoals(this.client, user, dayOfWeek);
  }

  generateDailyVariations(user: UserData, dayOfWeek: number): Promise<GoalTask[]> {
    return generateDailyVariations(this.client, user, dayOfWeek);
  }

  getDailyMotivation(input: DailyMotivationInput): Promise<DailyMotivationOutput> {
    return this.dailyMotivation.generate(input);
  }

  detectDrift(input: DriftDetectorInput): Promise<DriftResult> {
    return this.driftDetector.detect(input);
  }

  getWeeklyInsights(input: HabitInsightsInput): Promise<HabitInsightsOutput> {
    return this.habitInsights.generate(input);
  }

  phraseCoachMessage(input: CoachPhrasingInput): Promise<string> {
    return this.coachPhrasing.phrase(input);
  }

  checkHabitGoalRelevance(input: HabitGoalRelevanceInput): Promise<HabitGoalRelevanceOutput> {
    return this.habitGoalRelevance.check(input);
  }

  recordMotivationFeedback(userId: string, vote: MotivationVote): FeedbackTally {
    return this.feedbackStore.record(userId, vote);
  }

  getFeedbackTally(userId: string): FeedbackTally {
    return this.feedbackStore.get(userId);
  }
}

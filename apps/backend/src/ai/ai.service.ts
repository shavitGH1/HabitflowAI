import { Injectable } from '@nestjs/common';
import { GenerateGoalsResponse, GoalTask } from '../dto/goal.dto';
import { UserData } from '../users/user.repository';
import { generateDailyVariations, generateInitialGoals } from './features/daily-motivation';
import { PersonaResult, classifyPersona } from './features/persona-classifier';
import { PersonaClassifierFeature, PersonaClassifierInput } from './features/persona-classifier.feature';
import { PortfolioGeneratorFeature, PortfolioGeneratorInput } from './features/portfolio-generator.feature';
import { PersonaClassifierOutput } from './schemas/persona-classifier.schema';
import { PortfolioGeneratorOutput } from './schemas/portfolio-generator.schema';
import { GeminiClient } from './gemini.client';

type GoalInput = Pick<UserData, 'goal' | 'personaType' | 'email'>;

@Injectable()
export class AiService {
  constructor(
    private readonly client: GeminiClient,
    private readonly personaClassifier: PersonaClassifierFeature,
    private readonly portfolioGenerator: PortfolioGeneratorFeature,
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
}

import { Injectable } from '@nestjs/common';
import { GenerateGoalsResponse, GoalTask } from '../dto/goal.dto';
import { UserData } from '../users/user.repository';
import { generateDailyVariations, generateInitialGoals } from './features/daily-motivation';
import { PersonaResult, classifyPersona } from './features/persona-classifier';
import { GeminiClient } from './gemini.client';

type GoalInput = Pick<UserData, 'goal' | 'personaType' | 'email'>;

@Injectable()
export class AiService {
  constructor(private readonly client: GeminiClient) {}

  classifyPersona(goal: string, answers: string[]): Promise<PersonaResult> {
    return classifyPersona(this.client, goal, answers);
  }

  generateInitialGoals(user: GoalInput, dayOfWeek: number): Promise<GenerateGoalsResponse> {
    return generateInitialGoals(this.client, user, dayOfWeek);
  }

  generateDailyVariations(user: UserData, dayOfWeek: number): Promise<GoalTask[]> {
    return generateDailyVariations(this.client, user, dayOfWeek);
  }
}

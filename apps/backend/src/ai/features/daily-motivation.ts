import { GenerateGoalsResponse, GoalTask } from '../../dto/goal.dto';
import { UserData } from '../../users/user.repository';
import { GeminiClient } from '../gemini.client';
import { buildDailyVariationsPrompt, buildInitialGoalsPrompt } from '../prompts/daily-motivation.prompt';

type GoalInput = Pick<UserData, 'goal' | 'personaType' | 'email'>;

export const generateInitialGoals = (
  client: GeminiClient,
  user: GoalInput,
  dayOfWeek: number,
): Promise<GenerateGoalsResponse> =>
  client.generateJson<GenerateGoalsResponse>(
    buildInitialGoalsPrompt(user.personaType, user.goal, dayOfWeek),
  );

export const generateDailyVariations = async (
  client: GeminiClient,
  user: UserData,
  dayOfWeek: number,
): Promise<GoalTask[]> => {
  const response = await client.generateJson<{ dailyVariations: GoalTask[] }>(
    buildDailyVariationsPrompt(user.personaType, user.goal, dayOfWeek),
  );
  return response.dailyVariations;
};

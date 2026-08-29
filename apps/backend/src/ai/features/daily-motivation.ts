import { GenerateGoalsResponse, GoalTask } from '../../dto/goal.dto';
import { UserData } from '../../users/user.repository';
import { GeminiClient } from '../gemini.client';
import { buildDailyVariationsPrompt, buildInitialGoalsPrompt } from '../prompts/daily-motivation.prompt';
import { dailyVariationsOutputSchema } from '../schemas/daily-variations.schema';

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
  habits: { id: string; title: string }[] = [],
  difficultyBias?: 'increase' | 'decrease',
): Promise<Omit<GoalTask, 'id' | 'completed'>[]> => {
  const response = await client.generateJson(
    buildDailyVariationsPrompt(user.personaType, user.goal, dayOfWeek, habits, difficultyBias),
    dailyVariationsOutputSchema,
  );
  return response.dailyVariations;
};

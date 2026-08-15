export type GoalTaskGenre = 'goal' | 'persona';

export interface GoalTask {
  id: string;
  description: string;
  points: number;
  completed: boolean;
  genre: GoalTaskGenre;
}

export interface GenerateGoalsResponse {
  isValid: boolean;
  errorReason?: string;
  personaType?: string;
  motivationalMessage?: string;
  coreGoals?: GoalTask[];
  dailyVariations?: GoalTask[];
}

import { GoalTask } from '../../dto/goal.dto';

export const MAX_MAIN_GOAL_TASKS_PER_DAY = 5;

export const capGoalTasks = (
  coreGoals: GoalTask[],
  dailyVariations: GoalTask[],
  max = MAX_MAIN_GOAL_TASKS_PER_DAY,
): { coreGoals: GoalTask[]; dailyVariations: GoalTask[] } => {
  const goalTasks = [...coreGoals, ...dailyVariations].filter(t => t.genre === 'goal');
  const completed = goalTasks.filter(t => t.completed);
  const incomplete = goalTasks.filter(t => !t.completed);
  const keepCount = Math.max(0, max - completed.length);

  const keptIds = new Set([...completed, ...incomplete.slice(0, keepCount)].map(t => t.id));
  const keep = (t: GoalTask) => t.genre !== 'goal' || keptIds.has(t.id);

  return {
    coreGoals: coreGoals.filter(keep),
    dailyVariations: dailyVariations.filter(keep),
  };
};

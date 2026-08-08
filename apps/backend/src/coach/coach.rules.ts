import { HabitData } from '../habits/habit.repository';
import { CoachBand, CoachTipId } from './coach.templates';

const DAY_MS = 86_400_000;
const WINDOW_DAYS = 7;

export const PERSONA_REVIEW_DAYS = 30;

export interface CoachStats {
  habitCount: number;
  completionRate7d: number;
  streak: number;
  oldestHabitAgeDays: number;
  completedToday: string[];
}

export const toDateKey = (date: Date): string => date.toISOString().split('T')[0];

export const computeStats = (habits: HabitData[], now: Date): CoachStats => {
  const window = new Set<string>();
  for (let i = 0; i < WINDOW_DAYS; i++) {
    window.add(toDateKey(new Date(now.getTime() - i * DAY_MS)));
  }

  const completions = habits.reduce(
    (sum, habit) => sum + habit.completionHistory.filter((day) => window.has(day)).length,
    0,
  );
  const expected = habits.length * WINDOW_DAYS;
  const today = toDateKey(now);

  return {
    habitCount: habits.length,
    completionRate7d: expected ? completions / expected : 0,
    streak: habits.reduce((max, habit) => Math.max(max, habit.streak), 0),
    oldestHabitAgeDays: oldestHabitAgeDays(habits, now),
    completedToday: habits
      .filter((habit) => habit.completionHistory.includes(today))
      .map((habit) => habit.title),
  };
};

export const pickBand = (completionRate7d: number): CoachBand => {
  if (completionRate7d >= 0.8) return 'EXCELLENT';
  if (completionRate7d >= 0.5) return 'GOOD';
  if (completionRate7d >= 0.2) return 'SLIPPING';
  return 'AT_RISK';
};

export const pickTip = ({ habitCount, streak, completionRate7d }: CoachStats): CoachTipId | null => {
  if (!habitCount) return null;
  if (streak === 0) return 'RESTART_SMALL';
  if (completionRate7d < 0.5) return 'NARROW_FOCUS';
  if (completionRate7d >= 0.8) return 'ADD_ONE';
  return null;
};

export const isDueForPersonaReview = ({ habitCount, oldestHabitAgeDays }: CoachStats): boolean =>
  habitCount > 0 && oldestHabitAgeDays >= PERSONA_REVIEW_DAYS;

const oldestHabitAgeDays = (habits: HabitData[], now: Date): number => {
  const created = habits.map((habit) => Date.parse(habit.createdAt)).filter((ms) => !Number.isNaN(ms));
  if (!created.length) return 0;
  return Math.floor((now.getTime() - Math.min(...created)) / DAY_MS);
};

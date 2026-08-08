import { HabitData } from '../habits/habit.repository';
import { computeStats, isDueForPersonaReview, pickBand, pickTip } from './coach.rules';

const NOW = new Date('2026-08-08T12:00:00.000Z');

const dayBefore = (days: number): string =>
  new Date(NOW.getTime() - days * 86_400_000).toISOString().split('T')[0];

const habit = (overrides: Partial<HabitData> = {}): HabitData => ({
  id: 'h1',
  userId: 'u1',
  title: 'Morning run',
  description: '',
  frequency: 'daily',
  targetCount: 1,
  streak: 0,
  completionHistory: [],
  persona: 'Achiever',
  isArchived: false,
  createdAt: NOW.toISOString(),
  ...overrides,
});

describe('computeStats', () => {
  it('returns a zeroed report when the user has no habits', () => {
    const stats = computeStats([], NOW);

    expect(stats).toEqual({
      habitCount: 0,
      completionRate7d: 0,
      streak: 0,
      oldestHabitAgeDays: 0,
      completedToday: [],
    });
  });

  it('counts only completions inside the 7 day window', () => {
    const stats = computeStats(
      [habit({ completionHistory: [dayBefore(0), dayBefore(6), dayBefore(9)] })],
      NOW,
    );

    expect(stats.completionRate7d).toBeCloseTo(2 / 7);
  });

  it('reports the highest streak and the habits completed today', () => {
    const stats = computeStats(
      [
        habit({ id: 'a', title: 'Run', streak: 3, completionHistory: [dayBefore(0)] }),
        habit({ id: 'b', title: 'Read', streak: 9, completionHistory: [dayBefore(1)] }),
      ],
      NOW,
    );

    expect(stats.streak).toBe(9);
    expect(stats.completedToday).toEqual(['Run']);
  });

  it('measures the age of the oldest habit', () => {
    const stats = computeStats(
      [
        habit({ id: 'a', createdAt: new Date(NOW.getTime() - 40 * 86_400_000).toISOString() }),
        habit({ id: 'b' }),
      ],
      NOW,
    );

    expect(stats.oldestHabitAgeDays).toBe(40);
  });
});

describe('pickBand', () => {
  it.each([
    [1, 'EXCELLENT'],
    [0.8, 'EXCELLENT'],
    [0.79, 'GOOD'],
    [0.5, 'GOOD'],
    [0.49, 'SLIPPING'],
    [0.2, 'SLIPPING'],
    [0.19, 'AT_RISK'],
    [0, 'AT_RISK'],
  ])('maps a rate of %s to %s', (rate, expected) => {
    expect(pickBand(rate as number)).toBe(expected);
  });
});

describe('pickTip', () => {
  const stats = (completionRate7d: number, streak: number) => ({
    habitCount: 2,
    completionRate7d,
    streak,
    oldestHabitAgeDays: 5,
    completedToday: [],
  });

  it('tells a user with no streak to restart small', () => {
    expect(pickTip(stats(0.9, 0))).toBe('RESTART_SMALL');
  });

  it('tells a user holding a streak but missing the rest to narrow down', () => {
    expect(pickTip(stats(0.3, 4))).toBe('NARROW_FOCUS');
  });

  it('tells a consistent user to add one habit', () => {
    expect(pickTip(stats(0.9, 4))).toBe('ADD_ONE');
  });

  it('returns no tip in the middle band', () => {
    expect(pickTip(stats(0.6, 4))).toBeNull();
  });

  it('returns no tip when the user has no habits', () => {
    expect(pickTip({ ...stats(0, 0), habitCount: 0 })).toBeNull();
  });
});

describe('isDueForPersonaReview', () => {
  const base = { habitCount: 1, completionRate7d: 0.5, streak: 1, completedToday: [] };

  it('is due once the oldest habit reaches 30 days', () => {
    expect(isDueForPersonaReview({ ...base, oldestHabitAgeDays: 30 })).toBe(true);
  });

  it('is not due before 30 days', () => {
    expect(isDueForPersonaReview({ ...base, oldestHabitAgeDays: 29 })).toBe(false);
  });

  it('is not due without habits', () => {
    expect(isDueForPersonaReview({ ...base, habitCount: 0, oldestHabitAgeDays: 90 })).toBe(false);
  });
});

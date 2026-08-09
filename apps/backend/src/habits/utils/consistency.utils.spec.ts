import { calculateConsistencyScore, isImplemented, IMPLEMENTED_MIN_DAYS, IMPLEMENTED_MIN_SCORE } from './consistency.utils';

const dayOffset = (start: string, offset: number): string => {
  const date = new Date(start + 'T00:00:00.000Z');
  date.setUTCDate(date.getUTCDate() + offset);
  return date.toISOString().split('T')[0];
};

describe('calculateConsistencyScore()', () => {
  const START = '2026-01-01';

  it('returns 0 for a brand new habit with no completions yet', () => {
    expect(calculateConsistencyScore([], START, START)).toBe(0);
  });

  it('matches the hand-computed EWMA for the first three days, all completed (alpha = 0.2)', () => {
    const day1 = START;
    const day2 = dayOffset(START, 1);
    const day3 = dayOffset(START, 2);

    expect(calculateConsistencyScore([day1], START, day1)).toBeCloseTo(0.2, 5);
    expect(calculateConsistencyScore([day1, day2], START, day2)).toBeCloseTo(0.36, 5);
    expect(calculateConsistencyScore([day1, day2, day3], START, day3)).toBeCloseTo(0.488, 5);
  });

  it('a single miss after a long streak only barely dents the score', () => {
    const perfectDays = Array.from({ length: 7 }, (_, i) => dayOffset(START, i));
    const scoreBeforeMiss = calculateConsistencyScore(perfectDays, START, perfectDays[6]);

    // day 8 (index 7) is missed, day 9 (index 8) resumes
    const historyWithMiss = [...perfectDays, dayOffset(START, 8)];
    const scoreAfterMiss = calculateConsistencyScore(historyWithMiss, START, dayOffset(START, 8));

    expect(scoreBeforeMiss).toBeGreaterThan(0.75);
    // one miss knocks it down, but nowhere near back to 0
    expect(scoreAfterMiss).toBeGreaterThan(0.5);
    expect(scoreAfterMiss).toBeLessThan(scoreBeforeMiss);
  });

  it('a habit abandoned early decays back down close to 0', () => {
    // completed only the first 2 days, then 25 days pass with no completions at all
    const history = [START, dayOffset(START, 1)];
    const today = dayOffset(START, 25);

    const score = calculateConsistencyScore(history, START, today);

    expect(score).toBeLessThan(0.05);
  });

  it('a perfect long streak converges close to 1', () => {
    const start = START;
    const days = Array.from({ length: 40 }, (_, i) => dayOffset(start, i));

    const score = calculateConsistencyScore(days, start, days[39]);

    expect(score).toBeGreaterThan(0.99);
  });
});

describe('isImplemented()', () => {
  const START = '2026-01-01';

  it('is false before the minimum day count, even with a perfect score', () => {
    const today = dayOffset(START, IMPLEMENTED_MIN_DAYS - 1);
    expect(isImplemented(1, START, today)).toBe(false);
  });

  it('is false after the minimum day count if the score is still too low', () => {
    const today = dayOffset(START, IMPLEMENTED_MIN_DAYS);
    expect(isImplemented(IMPLEMENTED_MIN_SCORE - 0.01, START, today)).toBe(false);
  });

  it('is true once both the day count and score thresholds are met', () => {
    const today = dayOffset(START, IMPLEMENTED_MIN_DAYS);
    expect(isImplemented(IMPLEMENTED_MIN_SCORE, START, today)).toBe(true);
  });
});

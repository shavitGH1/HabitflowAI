import { DayProgress, isPerfectWeek, medalForRank, scoreCompletion } from './scoring.utils';

describe('scoring.utils', () => {
  describe('scoreCompletion()', () => {
    it('awards the base 100 points for the first completion of the day', () => {
      const { pointsDelta, day } = scoreCompletion('2026-08-17', 4, undefined);

      expect(pointsDelta).toBe(100);
      expect(day).toEqual({
        date: '2026-08-17',
        completedCount: 1,
        rosterSize: 4,
        halfBonusAwarded: false,
        allBonusAwarded: false,
      });
    });

    it('awards the +250 half bonus on top of the 100 when crossing half the roster', () => {
      const existing: DayProgress = {
        date: '2026-08-17',
        completedCount: 1,
        rosterSize: 4,
        halfBonusAwarded: false,
        allBonusAwarded: false,
      };

      const { pointsDelta, day } = scoreCompletion('2026-08-17', 4, existing);

      expect(pointsDelta).toBe(350); // 100 base + 250 bonus, completedCount 2/4 = half
      expect(day.halfBonusAwarded).toBe(true);
      expect(day.allBonusAwarded).toBe(false);
    });

    it('awards the +300 all bonus on top of the 100 when completing the whole roster', () => {
      const existing: DayProgress = {
        date: '2026-08-17',
        completedCount: 3,
        rosterSize: 4,
        halfBonusAwarded: true,
        allBonusAwarded: false,
      };

      const { pointsDelta, day } = scoreCompletion('2026-08-17', 4, existing);

      expect(pointsDelta).toBe(400); // 100 base + 300 bonus, completedCount 4/4 = all
      expect(day.allBonusAwarded).toBe(true);
    });

    it('never re-awards the half bonus once already earned that day', () => {
      const existing: DayProgress = {
        date: '2026-08-17',
        completedCount: 2,
        rosterSize: 5,
        halfBonusAwarded: true,
        allBonusAwarded: false,
      };

      const { pointsDelta } = scoreCompletion('2026-08-17', 5, existing);

      expect(pointsDelta).toBe(100); // just the base, half already awarded
    });

    it('rounds "half" up for an odd roster size (3 tasks: half = 2)', () => {
      const existing: DayProgress = {
        date: '2026-08-17',
        completedCount: 1,
        rosterSize: 3,
        halfBonusAwarded: false,
        allBonusAwarded: false,
      };

      const { pointsDelta, day } = scoreCompletion('2026-08-17', 3, existing);

      expect(day.halfBonusAwarded).toBe(true);
      expect(pointsDelta).toBe(350);
    });

    it('awards both half and all bonuses at once for a single-task roster', () => {
      const { pointsDelta, day } = scoreCompletion('2026-08-17', 1, undefined);

      expect(pointsDelta).toBe(650); // 100 + 250 + 300, first and only completion
      expect(day.halfBonusAwarded).toBe(true);
      expect(day.allBonusAwarded).toBe(true);
    });
  });

  describe('isPerfectWeek()', () => {
    const perfectDay = (date: string): DayProgress => ({
      date,
      completedCount: 4,
      rosterSize: 4,
      halfBonusAwarded: true,
      allBonusAwarded: true,
    });

    it('is true only when all 7 days hit the "complete all" tier', () => {
      const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map(perfectDay);
      expect(isPerfectWeek(days)).toBe(true);
    });

    it('is false with fewer than 7 recorded days', () => {
      const days = ['Mon', 'Tue', 'Wed'].map(perfectDay);
      expect(isPerfectWeek(days)).toBe(false);
    });

    it('is false when any day did not reach the "all" tier', () => {
      const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map(perfectDay);
      days[3] = { ...days[3], allBonusAwarded: false };
      expect(isPerfectWeek(days)).toBe(false);
    });
  });

  describe('medalForRank()', () => {
    it.each([
      [1, 'gold'],
      [2, 'silver'],
      [3, 'bronze'],
      [10, 'top10'],
      [50, 'top50'],
      [100, 'top100'],
      [101, undefined],
    ])('rank %i -> %s', (rank, expected) => {
      expect(medalForRank(rank)).toBe(expected);
    });
  });
});

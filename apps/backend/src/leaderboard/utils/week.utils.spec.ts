import { getMonthStart, getPreviousMonthStart, getPreviousWeekStart, getWeekStart } from './week.utils';

describe('week.utils', () => {
  describe('getWeekStart()', () => {
    it('returns the same date when it is already a Monday', () => {
      expect(getWeekStart('2026-08-17')).toBe('2026-08-17');
    });

    it('returns the prior Monday for a mid-week date', () => {
      expect(getWeekStart('2026-08-19')).toBe('2026-08-17');
    });

    it('handles Sunday correctly (rolls back to that week\'s Monday, not the next one)', () => {
      expect(getWeekStart('2026-08-23')).toBe('2026-08-17');
    });

    it('handles a week that crosses a month boundary', () => {
      expect(getWeekStart('2026-09-01')).toBe('2026-08-31');
    });
  });

  describe('getMonthStart()', () => {
    it('returns the 1st of the given date\'s month', () => {
      expect(getMonthStart('2026-08-19')).toBe('2026-08-01');
    });
  });

  describe('getPreviousWeekStart()', () => {
    it('returns exactly 7 days before the current week\'s Monday', () => {
      expect(getPreviousWeekStart('2026-08-19')).toBe('2026-08-10');
    });
  });

  describe('getPreviousMonthStart()', () => {
    it('returns the prior month within the same year', () => {
      expect(getPreviousMonthStart('2026-08-19')).toBe('2026-07-01');
    });

    it('rolls back across a year boundary from January', () => {
      expect(getPreviousMonthStart('2026-01-15')).toBe('2025-12-01');
    });
  });
});

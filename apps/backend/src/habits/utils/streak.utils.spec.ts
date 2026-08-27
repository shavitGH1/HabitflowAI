import { calculateStreak } from './streak.utils';

const TODAY = '2026-07-11';
const YESTERDAY = '2026-07-10';
const TWO_DAYS_AGO = '2026-07-09';
const THREE_DAYS_AGO = '2026-07-08';
const FOUR_DAYS_AGO = '2026-07-07';

describe('calculateStreak()', () => {
  it('returns 0 for empty history', () => {
    expect(calculateStreak([], TODAY)).toBe(0);
  });

  it('returns 1 when only today is completed', () => {
    expect(calculateStreak([TODAY], TODAY)).toBe(1);
  });

  it('returns 2 for two consecutive days ending today', () => {
    expect(calculateStreak([YESTERDAY, TODAY], TODAY)).toBe(2);
  });

  it('returns 5 for five consecutive days ending today', () => {
    const history = [TODAY, YESTERDAY, TWO_DAYS_AGO, THREE_DAYS_AGO, FOUR_DAYS_AGO];
    expect(calculateStreak(history, TODAY)).toBe(5);
  });

  it('returns 1 when today is completed but yesterday was missed', () => {
    expect(calculateStreak([TWO_DAYS_AGO, TODAY], TODAY)).toBe(1);
  });

  it('returns 0 when last completion was before yesterday', () => {
    expect(calculateStreak([TWO_DAYS_AGO, THREE_DAYS_AGO], TODAY)).toBe(0);
  });

  it('deduplicates repeated dates', () => {
    expect(calculateStreak([TODAY, TODAY, TODAY], TODAY)).toBe(1);
  });

  it('keeps a streak alive when today is not completed yet but yesterday was', () => {
    const history = [YESTERDAY, TWO_DAYS_AGO, THREE_DAYS_AGO];
    expect(calculateStreak(history, TODAY)).toBe(3);
  });

  it('resets to 0 when neither today nor yesterday was completed', () => {
    const history = [THREE_DAYS_AGO, FOUR_DAYS_AGO];
    expect(calculateStreak(history, TODAY)).toBe(0);
  });
});

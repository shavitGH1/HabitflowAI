export interface DayProgress {
  date: string;
  completedCount: number;
  rosterSize: number;
  halfBonusAwarded: boolean;
  allBonusAwarded: boolean;
}

export interface ScoringResult {
  pointsDelta: number;
  day: DayProgress;
}

/**
 * One completion just happened. rosterSize is today's full task+habit count
 * (the item just completed is part of it, so rosterSize >= 1 here always).
 * Bonuses fire at most once per day each — existingDay carries forward
 * whether they already did.
 */
export const scoreCompletion = (
  date: string,
  rosterSize: number,
  existingDay: DayProgress | undefined,
): ScoringResult => {
  const completedCount = (existingDay?.completedCount ?? 0) + 1;
  let pointsDelta = 100;
  let halfBonusAwarded = existingDay?.halfBonusAwarded ?? false;
  let allBonusAwarded = existingDay?.allBonusAwarded ?? false;

  if (!halfBonusAwarded && completedCount >= Math.ceil(rosterSize / 2)) {
    pointsDelta += 250;
    halfBonusAwarded = true;
  }
  if (!allBonusAwarded && completedCount >= rosterSize) {
    pointsDelta += 300;
    allBonusAwarded = true;
  }

  return {
    pointsDelta,
    day: { date, completedCount, rosterSize, halfBonusAwarded, allBonusAwarded },
  };
};

/** Every day of a full 7-day week hit the "complete all" tier. */
export const isPerfectWeek = (days: DayProgress[]): boolean =>
  days.length === 7 && days.every(d => d.allBonusAwarded);

export const medalForRank = (rank: number): string | undefined => {
  if (rank === 1) return 'gold';
  if (rank === 2) return 'silver';
  if (rank === 3) return 'bronze';
  if (rank <= 10) return 'top10';
  if (rank <= 50) return 'top50';
  if (rank <= 100) return 'top100';
  return undefined;
};

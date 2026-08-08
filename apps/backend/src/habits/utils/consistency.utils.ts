export const DEFAULT_ALPHA = 0.2;
export const IMPLEMENTED_MIN_DAYS = 28;
export const IMPLEMENTED_MIN_SCORE = 0.8;

export const calculateConsistencyScore = (
  completionHistory: string[],
  createdAt: string,
  today = new Date().toISOString().split('T')[0],
  alpha = DEFAULT_ALPHA,
): number => {
  const completedDates = new Set(completionHistory);
  const cursor = new Date(createdAt + 'T00:00:00.000Z');
  const end = new Date(today + 'T00:00:00.000Z');

  let score = 0;
  while (cursor <= end) {
    const dateStr = cursor.toISOString().split('T')[0];
    const completedThatDay = completedDates.has(dateStr) ? 1 : 0;
    score = alpha * completedThatDay + (1 - alpha) * score;
    cursor.setUTCDate(cursor.getUTCDate() + 1);
  }

  return score;
};

export const isImplemented = (
  consistencyScore: number,
  createdAt: string,
  today = new Date().toISOString().split('T')[0],
): boolean => {
  return daysBetween(createdAt, today) >= IMPLEMENTED_MIN_DAYS && consistencyScore >= IMPLEMENTED_MIN_SCORE;
};

const daysBetween = (from: string, to: string): number => {
  const fromDate = new Date(from + 'T00:00:00.000Z');
  const toDate = new Date(to + 'T00:00:00.000Z');
  return Math.round((toDate.getTime() - fromDate.getTime()) / 86_400_000);
};

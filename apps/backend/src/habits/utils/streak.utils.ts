export const MIN_STREAK_FOR_MANUAL_ACHIEVEMENT = 21;

export const canManuallyAchieve = (streak: number): boolean =>
  streak >= MIN_STREAK_FOR_MANUAL_ACHIEVEMENT;

export const calculateStreak = (
  history: string[],
  today = new Date().toISOString().split('T')[0],
): number => {
  const sorted = [...new Set(history)].sort().reverse();
  if (!sorted.length) return 0;

  const cursor = new Date(today + 'T00:00:00.000Z');
  const yesterday = new Date(cursor);
  yesterday.setUTCDate(yesterday.getUTCDate() - 1);
  const yesterdayStr = yesterday.toISOString().split('T')[0];

  if (sorted[0] === yesterdayStr) {
    cursor.setUTCDate(cursor.getUTCDate() - 1);
  } else if (sorted[0] !== today) {
    return 0;
  }

  let streak = 0;
  for (const dateStr of sorted) {
    const expected = cursor.toISOString().split('T')[0];
    if (dateStr === expected) {
      streak++;
      cursor.setUTCDate(cursor.getUTCDate() - 1);
    } else {
      break;
    }
  }

  return streak;
};

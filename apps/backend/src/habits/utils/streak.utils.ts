export const calculateStreak = (
  history: string[],
  today = new Date().toISOString().split('T')[0],
): number => {
  const sorted = [...new Set(history)].sort().reverse();
  if (!sorted.length) return 0;

  let streak = 0;
  const cursor = new Date(today + 'T00:00:00.000Z');

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

/** Monday of the week containing dateStr (YYYY-MM-DD), as YYYY-MM-DD. */
export const getWeekStart = (dateStr: string): string => {
  const date = new Date(`${dateStr}T00:00:00.000Z`);
  const day = date.getUTCDay(); // 0=Sun .. 6=Sat
  const daysSinceMonday = day === 0 ? 6 : day - 1;
  date.setUTCDate(date.getUTCDate() - daysSinceMonday);
  return date.toISOString().split('T')[0];
};

/** 1st of the month containing dateStr (YYYY-MM-DD), as YYYY-MM-01. */
export const getMonthStart = (dateStr: string): string => `${dateStr.slice(0, 7)}-01`;

/** Monday of the ISO week immediately before the one containing dateStr. */
export const getPreviousWeekStart = (dateStr: string): string => {
  const date = new Date(`${getWeekStart(dateStr)}T00:00:00.000Z`);
  date.setUTCDate(date.getUTCDate() - 7);
  return date.toISOString().split('T')[0];
};

/** 1st of the calendar month immediately before the one containing dateStr. */
export const getPreviousMonthStart = (dateStr: string): string => {
  const [year, month] = dateStr.slice(0, 7).split('-').map(Number);
  const prevMonth = month === 1 ? 12 : month - 1;
  const prevYear = month === 1 ? year - 1 : year;
  return `${prevYear}-${String(prevMonth).padStart(2, '0')}-01`;
};

// Re-dates the scripted demo users' goals/achievements/habits relative to *today*,
// so their streaks/consistency/"recently achieved" always look current no matter
// how long ago the DB volume was first seeded by mongo-init.js.
//
// Deliberately does NOT touch posts, chats, messages, comments, or follows —
// that social content keeps whatever timeline it was originally seeded with.
//
// Safe to re-run any time (updates in place, never inserts/duplicates).
//
// Run against the local dev Mongo:
//   docker cp refresh-demo-dates.js habitflow-mongo:/tmp/refresh-demo-dates.js
//   docker exec habitflow-mongo mongosh "mongodb://<user>:<pass>@localhost:27017/habitflow?authSource=admin" --file /tmp/refresh-demo-dates.js

const today = new Date().toISOString().split('T')[0];

const EWMA_ALPHA = 0.2;
const IMPLEMENTED_MIN_DAYS = 28;
const IMPLEMENTED_MIN_SCORE = 0.8;

function dateStr(d) { return d.toISOString().split('T')[0]; }
function daysAgoDate(n) { const d = new Date(); d.setUTCDate(d.getUTCDate() - n); return d; }
function daysAgoStr(n) { return dateStr(daysAgoDate(n)); }
function daysBetween(fromStr) {
  const from = new Date(fromStr + 'T00:00:00.000Z');
  const to = new Date(today + 'T00:00:00.000Z');
  return Math.round((to - from) / 86400000);
}
function historyLastNDays(n, skipOffsets = []) {
  const days = [];
  for (let i = 0; i < n; i++) {
    if (!skipOffsets.includes(i)) days.push(daysAgoStr(i));
  }
  return days;
}
function calcStreak(history) {
  const sorted = [...new Set(history)].sort().reverse();
  if (!sorted.length) return 0;
  const cursor = new Date(today + 'T00:00:00.000Z');
  const yesterday = new Date(cursor);
  yesterday.setUTCDate(yesterday.getUTCDate() - 1);
  if (sorted[0] === dateStr(yesterday)) {
    cursor.setUTCDate(cursor.getUTCDate() - 1);
  } else if (sorted[0] !== today) {
    return 0;
  }
  let streak = 0;
  for (const d of sorted) {
    if (d === dateStr(cursor)) {
      streak++;
      cursor.setUTCDate(cursor.getUTCDate() - 1);
    } else break;
  }
  return streak;
}
function calcConsistency(history, createdAtStr) {
  const completed = new Set(history);
  const cursor = new Date(createdAtStr + 'T00:00:00.000Z');
  const end = new Date(today + 'T00:00:00.000Z');
  let score = 0;
  while (cursor <= end) {
    score = EWMA_ALPHA * (completed.has(dateStr(cursor)) ? 1 : 0) + (1 - EWMA_ALPHA) * score;
    cursor.setUTCDate(cursor.getUTCDate() + 1);
  }
  return Math.round(score * 100) / 100;
}
function refreshedHabitFields({ createdAtDaysAgo, history, notes }) {
  const createdAtStr = daysAgoStr(createdAtDaysAgo);
  const consistencyScore = calcConsistency(history, createdAtStr);
  const implemented = daysBetween(createdAtStr) >= IMPLEMENTED_MIN_DAYS && consistencyScore >= IMPLEMENTED_MIN_SCORE;
  const fields = {
    streak: calcStreak(history),
    completionHistory: history,
    consistencyScore,
    createdAt: daysAgoDate(createdAtDaysAgo),
    updatedAt: new Date(),
  };
  fields.completionNotes = notes || [];
  fields.$unset = !implemented;
  return fields;
}

const demoUserIds = {};
db.users.find({ email: { $regex: '^demo\\.' } }, { email: 1 }).forEach(u => {
  demoUserIds[u.email] = u._id.toString();
});
const alexId = demoUserIds['demo.alex@habitflow.ai'];
const mayaId = demoUserIds['demo.maya@habitflow.ai'];
const benId = demoUserIds['demo.ben@habitflow.ai'];
const saraId = demoUserIds['demo.sara@habitflow.ai'];
const tomId = demoUserIds['demo.tom@habitflow.ai'];
const lenaId = demoUserIds['demo.lena@habitflow.ai'];
const danaId = demoUserIds['demo.dana@habitflow.ai'];
const ronId = demoUserIds['demo.ron@habitflow.ai'];

if (!alexId || !mayaId || !benId || !saraId || !tomId || !lenaId || !danaId || !ronId) {
  print('One or more demo users not found — run mongo-init.js first. Aborting.');
  quit(1);
}

// --- Goals + achievements ---

const goalDefs = [
  { key: 'alex', userId: alexId, title: 'Run a half marathon', targetDate: daysAgoDate(-60), status: 'active' },
  { key: 'alexAchieved', userId: alexId, title: 'Log workouts for 30 days straight', targetDate: daysAgoDate(3), status: 'achieved', medal: 'goal-achiever', awardedAt: daysAgoDate(3) },
  { key: 'maya', userId: mayaId, title: '30-day meditation streak', targetDate: daysAgoDate(2), status: 'achieved', medal: 'goal-achiever', awardedAt: daysAgoDate(2) },
  { key: 'ben', userId: benId, title: 'Train for a group 10k', targetDate: daysAgoDate(5), status: 'forfeited' },
  { key: 'benAchieved', userId: benId, title: 'Check in on a friend every day for a month', targetDate: daysAgoDate(4), status: 'achieved', medal: 'goal-achiever', awardedAt: daysAgoDate(4) },
  { key: 'sara', userId: saraId, title: 'Build a support squad', targetDate: daysAgoDate(-45), status: 'active' },
  { key: 'saraAchieved', userId: saraId, title: 'Complete a full community challenge', targetDate: daysAgoDate(5), status: 'achieved', medal: 'goal-achiever', awardedAt: daysAgoDate(5) },
  { key: 'tom', userId: tomId, title: 'Finish a personal development book', targetDate: daysAgoDate(6), status: 'achieved', medal: 'goal-achiever', awardedAt: daysAgoDate(6) },
  { key: 'lena', userId: lenaId, title: 'Try 30 new experiences', targetDate: daysAgoDate(7), status: 'achieved', medal: 'goal-achiever', awardedAt: daysAgoDate(7) },
  { key: 'dana', userId: danaId, title: 'Complete a season of volunteering', targetDate: daysAgoDate(8), status: 'achieved', medal: 'goal-achiever', awardedAt: daysAgoDate(8) },
  { key: 'ron', userId: ronId, title: 'Build a bulletproof morning routine', targetDate: daysAgoDate(9), status: 'achieved', medal: 'goal-achiever', awardedAt: daysAgoDate(9) },
];

const goalIds = {};
goalDefs.forEach(def => {
  const goal = db.goals.findOne({ userId: def.userId, title: def.title });
  if (!goal) {
    print('SKIP goal (not found): "' + def.title + '" for ' + def.userId);
    return;
  }
  goalIds[def.key] = goal._id.toString();
  db.goals.updateOne({ _id: goal._id }, { $set: { targetDate: def.targetDate, updatedAt: new Date() } });

  if (def.status === 'achieved') {
    const res = db.users.updateOne(
      { _id: ObjectId(def.userId), 'achievements.goalId': goalIds[def.key] },
      { $set: { 'achievements.$.awardedAt': def.awardedAt } },
    );
    if (res.matchedCount === 0) {
      print('SKIP achievement (not found on user): "' + def.title + '" for ' + def.userId);
    }
  }
  print('refreshed goal "' + def.title + '"');
});

// --- Habits ---

const habitDefs = [
  { userId: alexId, title: '5km morning run', goalId: goalIds.alex, createdAtDaysAgo: 40, history: historyLastNDays(32, [5, 19]), notes: [{ date: today, note: 'New personal best pace today!' }] },
  { userId: alexId, title: 'Log workout stats in app', goalId: goalIds.alexAchieved, createdAtDaysAgo: 35, history: historyLastNDays(30) },
  { userId: alexId, title: '100 push-ups', goalId: goalIds.alex, createdAtDaysAgo: 18, history: historyLastNDays(10, [3]) },
  { userId: alexId, title: 'Cold shower', goalId: undefined, createdAtDaysAgo: 15, history: historyLastNDays(15, [1, 4, 7, 10, 13]) },
  { userId: mayaId, title: 'Daily meditation (15 min)', goalId: goalIds.maya, createdAtDaysAgo: 35, history: historyLastNDays(33, [12]), notes: [{ date: daysAgoStr(2), note: 'Hit the full 30 days — sticking with it.' }] },
  { userId: mayaId, title: 'Evening walk (20 min)', goalId: undefined, createdAtDaysAgo: 9, history: historyLastNDays(9, [2, 6]) },
  { userId: benId, title: 'Check in on a friend', goalId: goalIds.benAchieved, createdAtDaysAgo: 35, history: historyLastNDays(30) },
  { userId: benId, title: 'Check in on a friend', goalId: undefined, createdAtDaysAgo: 22, history: historyLastNDays(22, [4, 9, 15]) },
  { userId: benId, title: 'Group workout session', goalId: goalIds.ben, createdAtDaysAgo: 30, history: historyLastNDays(30, [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]) },
  { userId: saraId, title: 'Join a community challenge', goalId: goalIds.sara, createdAtDaysAgo: 11, history: historyLastNDays(11, [3, 8]) },
  { userId: saraId, title: 'Comment or react on 3 posts', goalId: goalIds.saraAchieved, createdAtDaysAgo: 35, history: historyLastNDays(30) },
  { userId: tomId, title: 'Read 20 pages', goalId: goalIds.tom, createdAtDaysAgo: 35, history: historyLastNDays(30), notes: [{ date: daysAgoStr(6), note: 'Finished the book!' }] },
  { userId: lenaId, title: 'Try a new route or activity', goalId: goalIds.lena, createdAtDaysAgo: 35, history: historyLastNDays(30) },
  { userId: danaId, title: 'Volunteer or help someone today', goalId: goalIds.dana, createdAtDaysAgo: 35, history: historyLastNDays(30) },
  { userId: ronId, title: 'Follow morning routine without skips', goalId: goalIds.ron, createdAtDaysAgo: 35, history: historyLastNDays(30) },
];

habitDefs.forEach(def => {
  const query = { userId: def.userId, title: def.title };
  query.goalId = def.goalId ? def.goalId : { $exists: false };
  const existing = db.habits.findOne(query);
  if (!existing) {
    print('SKIP habit (not found): "' + def.title + '" for ' + def.userId);
    return;
  }
  const { $unset: shouldUnsetImplemented, ...fields } = refreshedHabitFields(def);
  db.habits.updateOne(
    { _id: existing._id },
    shouldUnsetImplemented
      ? { $set: fields, $unset: { implementedAt: '' } }
      : { $set: { ...fields, implementedAt: new Date() } },
  );
  print('refreshed habit "' + def.title + '" for ' + def.userId + ' -> streak=' + fields.streak + ' consistency=' + fields.consistencyScore);
});

print('DONE');

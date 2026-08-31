// Patches demo accounts seeded by an older mongo-init.js (which is insert-only, so
// re-running it against a populated DB does nothing). Safe to re-run.
//
//   docker exec -i habitflow-mongo mongosh "<MONGO_URI>" < migrate-demo-data-v2.js

const today = new Date().toISOString().split('T')[0];

function dateStr(d) {
  return d.toISOString().split('T')[0];
}
function daysAgoDate(n) {
  const d = new Date();
  d.setUTCDate(d.getUTCDate() - n);
  return d;
}
function daysAgoStr(n) {
  return dateStr(daysAgoDate(n));
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
  const EWMA_ALPHA = 0.2;
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

// Only overwrite an array if every item still lacks a `genre` - otherwise it's real
// AI-regenerated content and shouldn't be clobbered with placeholder demo text.
const taskFixes = {
  'demo.alex@habitflow.ai': {
    coreGoals: [
      { id: 'alex-g1', description: "Beat yesterday's personal record", points: 25, completed: false, genre: 'goal' },
      { id: 'alex-g2', description: 'Plan next race training block', points: 20, completed: false, genre: 'goal' },
      { id: 'alex-g3', description: 'Review weekly performance metrics', points: 10, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'alex-v1', description: 'Visualize the finish line before bed', points: 10, completed: false, genre: 'goal' },
    ],
  },
  'demo.maya@habitflow.ai': {
    coreGoals: [
      { id: 'maya-g1', description: "Keep today's streak alive", points: 15, completed: false, genre: 'goal' },
      { id: 'maya-g2', description: 'Complete every item on the checklist', points: 20, completed: false, genre: 'goal' },
      { id: 'maya-g3', description: "Reflect on this week's consistency", points: 15, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'maya-v1', description: 'Write one line of gratitude', points: 10, completed: false, genre: 'goal' },
    ],
  },
  'demo.ben@habitflow.ai': {
    coreGoals: [
      { id: 'ben-g1', description: 'Reach out to someone new this week', points: 15, completed: false, genre: 'goal' },
      { id: 'ben-g2', description: 'Rally the squad for a shared win', points: 20, completed: false, genre: 'goal' },
      { id: 'ben-g3', description: "Celebrate a friend's progress", points: 10, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'ben-v1', description: 'Share an encouraging note', points: 10, completed: false, genre: 'goal' },
    ],
  },
  'demo.sara@habitflow.ai': {
    coreGoals: [
      { id: 'sara-g1', description: 'Show up for the community today', points: 20, completed: false, genre: 'goal' },
      { id: 'sara-g2', description: 'Engage with three people in the app', points: 10, completed: false, genre: 'goal' },
      { id: 'sara-g3', description: 'Invite a new member to the squad', points: 15, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'sara-v1', description: 'Send a voice note to a friend', points: 10, completed: false, genre: 'goal' },
    ],
  },
  'demo.tom@habitflow.ai': {
    coreGoals: [
      { id: 'tom-g1', description: 'Learn one new concept today', points: 15, completed: false, genre: 'goal' },
      { id: 'tom-g2', description: 'Spend focused time building a skill', points: 20, completed: false, genre: 'goal' },
      { id: 'tom-g3', description: 'Watch or read something educational', points: 10, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'tom-v1', description: "Summarize today's key takeaway", points: 10, completed: false, genre: 'goal' },
    ],
  },
  'demo.lena@habitflow.ai': {
    coreGoals: [
      { id: 'lena-g1', description: 'Break your usual routine today', points: 20, completed: false, genre: 'goal' },
      { id: 'lena-g2', description: "Research something you've never studied", points: 15, completed: false, genre: 'goal' },
      { id: 'lena-g3', description: 'Discover a new spot nearby', points: 15, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'lena-v1', description: 'Swap one habit for something different', points: 10, completed: false, genre: 'goal' },
    ],
  },
  'demo.dana@habitflow.ai': {
    coreGoals: [
      { id: 'dana-g1', description: "Make someone's day better", points: 25, completed: false, genre: 'goal' },
      { id: 'dana-g2', description: 'Send a message of encouragement', points: 10, completed: false, genre: 'goal' },
      { id: 'dana-g3', description: 'Give back to a cause you care about', points: 15, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'dana-v1', description: 'Perform one small act of kindness', points: 10, completed: false, genre: 'goal' },
    ],
  },
  'demo.ron@habitflow.ai': {
    coreGoals: [
      { id: 'ron-g1', description: 'Plan tomorrow the night before', points: 15, completed: false, genre: 'goal' },
      { id: 'ron-g2', description: 'Stick to your system today', points: 20, completed: false, genre: 'goal' },
      { id: 'ron-g3', description: "Audit and refine this week's plan", points: 15, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'ron-v1', description: 'Spend 10 minutes on your weekly review', points: 15, completed: false, genre: 'goal' },
    ],
  },
};

function stillUnmigrated(tasks) {
  return Array.isArray(tasks) && tasks.length > 0 && tasks.every((t) => t.genre === undefined);
}

Object.entries(taskFixes).forEach(([email, fix]) => {
  const user = db.users.findOne({ email });
  if (!user) {
    print(`[tasks] skip ${email}: user not found`);
    return;
  }

  const set = {};
  if (stillUnmigrated(user.coreGoals)) set.coreGoals = fix.coreGoals;
  if (stillUnmigrated(user.dailyVariations)) set.dailyVariations = fix.dailyVariations;

  if (Object.keys(set).length === 0) {
    print(`[tasks] skip ${email}: already migrated or already has real AI-generated content`);
    return;
  }

  db.users.updateOne({ _id: user._id }, { $set: set });
  print(`[tasks] updated ${Object.keys(set).join(' + ')} for ${email}`);
});

// Boost Alex's push-ups habit to a 25-day streak, kept under the 28-day
// auto-implement threshold so Task 77's manual "Complete Habit" button has something to demo.
const alexUser = db.users.findOne({ email: 'demo.alex@habitflow.ai' });
if (!alexUser) {
  print('[push-ups] skip: demo.alex@habitflow.ai not found');
} else {
  const alexId = alexUser._id.toString();
  const pushupsHabit = db.habits.findOne({ userId: alexId, title: '100 push-ups' });
  if (!pushupsHabit) {
    print('[push-ups] skip: habit not found for Alex');
  } else {
    const createdAtDaysAgo = 25;
    const history = historyLastNDays(createdAtDaysAgo);
    const createdAtStr = daysAgoStr(createdAtDaysAgo);
    db.habits.updateOne(
      { _id: pushupsHabit._id },
      {
        $set: {
          completionHistory: history,
          streak: calcStreak(history),
          consistencyScore: calcConsistency(history, createdAtStr),
          createdAt: daysAgoDate(createdAtDaysAgo),
          updatedAt: new Date(),
        },
        $unset: { implementedAt: '' },
      },
    );
    print('[push-ups] boosted to a clean 25-day streak for Alex');
  }

  // Backfill Alex's taskHistory - a brand new field, safe to always overwrite.
  const habitIdByTitle = (title) => {
    const habit = db.habits.findOne({ userId: alexId, title });
    return habit ? habit._id.toString() : undefined;
  };
  const runHabitId = habitIdByTitle('5km morning run');
  const pushupsHabitId = habitIdByTitle('100 push-ups');
  const logStatsHabitId = habitIdByTitle('Log workout stats in app');
  const coldShowerHabitId = habitIdByTitle('Cold shower');

  const taskHistoryDefs = [
    { daysAgo: 1, tasks: [
      { id: 'hist-alex-1-1', description: "Beat yesterday's personal record", points: 25, completed: true, genre: 'goal' },
      { id: 'hist-alex-1-2', description: 'Plan next race training block', points: 20, completed: true, genre: 'goal' },
      { id: 'hist-alex-1-3', description: 'Review weekly performance metrics', points: 10, completed: false, genre: 'goal' },
      { id: 'hist-alex-1-4', description: 'Run 5km at an easy recovery pace', points: 25, completed: true, genre: 'habit', habitId: runHabitId },
      { id: 'hist-alex-1-5', description: 'Complete 3 sets of push-ups', points: 20, completed: true, genre: 'habit', habitId: pushupsHabitId },
      { id: 'hist-alex-1-6', description: "Log today's splits in the app", points: 10, completed: true, genre: 'habit', habitId: logStatsHabitId },
    ] },
    { daysAgo: 2, tasks: [
      { id: 'hist-alex-2-1', description: "Beat yesterday's personal record", points: 25, completed: true, genre: 'goal' },
      { id: 'hist-alex-2-2', description: 'Plan next race training block', points: 20, completed: false, genre: 'goal' },
      { id: 'hist-alex-2-3', description: 'Review weekly performance metrics', points: 10, completed: false, genre: 'goal' },
      { id: 'hist-alex-2-4', description: 'Run 5km along the river route', points: 25, completed: true, genre: 'habit', habitId: runHabitId },
      { id: 'hist-alex-2-5', description: 'Complete 3 sets of push-ups', points: 20, completed: true, genre: 'habit', habitId: pushupsHabitId },
      { id: 'hist-alex-2-6', description: 'Take a 2-minute cold shower', points: 10, completed: true, genre: 'habit', habitId: coldShowerHabitId },
    ] },
    { daysAgo: 3, tasks: [
      { id: 'hist-alex-3-1', description: "Beat yesterday's personal record", points: 25, completed: true, genre: 'goal' },
      { id: 'hist-alex-3-2', description: 'Plan next race training block', points: 20, completed: true, genre: 'goal' },
      { id: 'hist-alex-3-3', description: 'Review weekly performance metrics', points: 10, completed: true, genre: 'goal' },
      { id: 'hist-alex-3-4', description: 'Run 5km at race pace', points: 25, completed: true, genre: 'habit', habitId: runHabitId },
      { id: 'hist-alex-3-5', description: 'Complete 3 sets of push-ups', points: 20, completed: true, genre: 'habit', habitId: pushupsHabitId },
      { id: 'hist-alex-3-6', description: "Log today's splits in the app", points: 10, completed: true, genre: 'habit', habitId: logStatsHabitId },
    ] },
    { daysAgo: 4, tasks: [
      { id: 'hist-alex-4-1', description: "Beat yesterday's personal record", points: 25, completed: false, genre: 'goal' },
      { id: 'hist-alex-4-2', description: 'Plan next race training block', points: 20, completed: false, genre: 'goal' },
      { id: 'hist-alex-4-3', description: 'Review weekly performance metrics', points: 10, completed: false, genre: 'goal' },
      { id: 'hist-alex-4-4', description: 'Run 5km at an easy recovery pace', points: 25, completed: true, genre: 'habit', habitId: runHabitId },
      { id: 'hist-alex-4-5', description: 'Complete 3 sets of push-ups', points: 20, completed: true, genre: 'habit', habitId: pushupsHabitId },
      { id: 'hist-alex-4-6', description: 'Take a 2-minute cold shower', points: 10, completed: false, genre: 'habit', habitId: coldShowerHabitId },
    ] },
    { daysAgo: 5, tasks: [
      { id: 'hist-alex-5-1', description: "Beat yesterday's personal record", points: 25, completed: true, genre: 'goal' },
      { id: 'hist-alex-5-2', description: 'Plan next race training block', points: 20, completed: true, genre: 'goal' },
      { id: 'hist-alex-5-3', description: 'Review weekly performance metrics', points: 10, completed: true, genre: 'goal' },
      { id: 'hist-alex-5-4', description: 'Run 5km along the river route', points: 25, completed: false, genre: 'habit', habitId: runHabitId },
      { id: 'hist-alex-5-5', description: 'Complete 3 sets of push-ups', points: 20, completed: true, genre: 'habit', habitId: pushupsHabitId },
      { id: 'hist-alex-5-6', description: "Log today's splits in the app", points: 10, completed: true, genre: 'habit', habitId: logStatsHabitId },
    ] },
  ];

  const taskHistory = taskHistoryDefs.map((def) => ({ date: daysAgoStr(def.daysAgo), tasks: def.tasks }));
  db.users.updateOne({ _id: alexUser._id }, { $set: { taskHistory } });
  print('[taskHistory] backfilled 5 days of Activity History for Alex');
}

print('Migration complete.');

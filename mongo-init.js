const DEMO_PASSWORD = '$2b$10$whSSki94nh9/sTltEVroZu3czQD0/bHwAL3XL3HxDwDaQRahe6y5K';
const today = new Date().toISOString().split('T')[0];

const users = [
  {
    email: 'demo.alex@habitflow.ai',
    firstName: 'Alex',
    lastName: 'Morgan',
    profilePicture: 'preset:1',
    password: DEMO_PASSWORD,
    authProvider: 'local',
    goal: 'Break personal records and dominate every leaderboard',
    personaType: 'Achiever',
    motivationalMessage: 'Champions are made in the moments they want to quit.',
    coreGoals: [
      { id: 'alex-g1', description: "Beat yesterday's personal record", points: 25, completed: false, genre: 'goal' },
      { id: 'alex-g2', description: 'Plan next race training block', points: 20, completed: false, genre: 'goal' },
      { id: 'alex-g3', description: 'Review weekly performance metrics', points: 10, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'alex-v1', description: 'Visualize the finish line before bed', points: 10, completed: false, genre: 'goal' },
    ],
    tasksLastGeneratedDate: today,
    personaBreakdown: { Achievement: 78, Growth: 10, Connection: 4, Exploration: 4, Purpose: 2, Structure: 2 },
    weightedScores: { Achievement: 0.78, Growth: 0.10, Connection: 0.04, Exploration: 0.04, Purpose: 0.02, Structure: 0.02 },
    portfolioSummary: 'You are fueled by competition and measurable wins.',
    tips: ['Track your personal bests daily', 'Set weekly targets you can beat', 'Compete with your past self'],
    failurePatterns: ['Burning out after intense streaks'],
    confidenceScore: 0.92,
    createdAt: new Date(),
    updatedAt: new Date(),
  },
  {
    email: 'demo.maya@habitflow.ai',
    firstName: 'Maya',
    lastName: 'Cohen',
    profilePicture: 'preset:2',
    password: DEMO_PASSWORD,
    authProvider: 'local',
    goal: 'Be the most consistent person in every challenge',
    personaType: 'Achiever',
    motivationalMessage: 'Your streak is your signature.',
    coreGoals: [
      { id: 'maya-g1', description: "Keep today's streak alive", points: 15, completed: false, genre: 'goal' },
      { id: 'maya-g2', description: 'Complete every item on the checklist', points: 20, completed: false, genre: 'goal' },
      { id: 'maya-g3', description: "Reflect on this week's consistency", points: 15, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'maya-v1', description: 'Write one line of gratitude', points: 10, completed: false, genre: 'goal' },
    ],
    tasksLastGeneratedDate: today,
    personaBreakdown: { Achievement: 72, Growth: 14, Connection: 5, Exploration: 3, Purpose: 4, Structure: 2 },
    weightedScores: { Achievement: 0.72, Growth: 0.14, Connection: 0.05, Exploration: 0.03, Purpose: 0.04, Structure: 0.02 },
    portfolioSummary: 'You chase consistency over perfection — and win because of it.',
    tips: ['Never break the chain', 'Review your streaks weekly', 'Visualize your goal daily'],
    failurePatterns: ['All-or-nothing thinking when one day is missed'],
    confidenceScore: 0.88,
    createdAt: new Date(),
    updatedAt: new Date(),
  },
  {
    email: 'demo.ben@habitflow.ai',
    firstName: 'Ben',
    lastName: 'Carter',
    profilePicture: 'preset:3',
    password: DEMO_PASSWORD,
    authProvider: 'local',
    goal: 'Build habits that bring people together',
    personaType: 'Socializer',
    motivationalMessage: 'Your energy lifts everyone around you.',
    coreGoals: [
      { id: 'ben-g1', description: 'Reach out to someone new this week', points: 15, completed: false, genre: 'goal' },
      { id: 'ben-g2', description: 'Rally the squad for a shared win', points: 20, completed: false, genre: 'goal' },
      { id: 'ben-g3', description: "Celebrate a friend's progress", points: 10, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'ben-v1', description: 'Share an encouraging note', points: 10, completed: false, genre: 'goal' },
    ],
    tasksLastGeneratedDate: today,
    personaBreakdown: { Achievement: 8, Growth: 10, Connection: 65, Exploration: 8, Purpose: 5, Structure: 4 },
    weightedScores: { Achievement: 0.08, Growth: 0.10, Connection: 0.65, Exploration: 0.08, Purpose: 0.05, Structure: 0.04 },
    portfolioSummary: 'Habits stick for you when others are involved.',
    tips: ['Join group challenges', 'Share your progress publicly', 'Find an accountability partner'],
    failurePatterns: ['Losing motivation when doing habits solo'],
    confidenceScore: 0.84,
    createdAt: new Date(),
    updatedAt: new Date(),
  },
  {
    email: 'demo.sara@habitflow.ai',
    firstName: 'Sara',
    lastName: 'Levi',
    profilePicture: 'preset:4',
    password: DEMO_PASSWORD,
    authProvider: 'local',
    goal: 'Stay consistent through community and connection',
    personaType: 'Socializer',
    motivationalMessage: 'You grow when you grow with others.',
    coreGoals: [
      { id: 'sara-g1', description: 'Show up for the community today', points: 20, completed: false, genre: 'goal' },
      { id: 'sara-g2', description: 'Engage with three people in the app', points: 10, completed: false, genre: 'goal' },
      { id: 'sara-g3', description: 'Invite a new member to the squad', points: 15, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'sara-v1', description: 'Send a voice note to a friend', points: 10, completed: false, genre: 'goal' },
    ],
    tasksLastGeneratedDate: today,
    personaBreakdown: { Achievement: 6, Growth: 8, Connection: 68, Exploration: 10, Purpose: 4, Structure: 4 },
    weightedScores: { Achievement: 0.06, Growth: 0.08, Connection: 0.68, Exploration: 0.10, Purpose: 0.04, Structure: 0.04 },
    portfolioSummary: 'Social momentum is your superpower — use it.',
    tips: ['Tag friends in your wins', 'React to others to stay engaged', 'Never miss a challenge you signed up for'],
    failurePatterns: ['Ghosting habits when the social buzz dies down'],
    confidenceScore: 0.81,
    createdAt: new Date(),
    updatedAt: new Date(),
  },
  {
    email: 'demo.tom@habitflow.ai',
    firstName: 'Tom',
    lastName: 'Reed',
    profilePicture: 'preset:5',
    password: DEMO_PASSWORD,
    authProvider: 'local',
    goal: 'Become 1% better every single day',
    personaType: 'Grower',
    motivationalMessage: 'Progress is the point.',
    coreGoals: [
      { id: 'tom-g1', description: 'Learn one new concept today', points: 15, completed: false, genre: 'goal' },
      { id: 'tom-g2', description: 'Spend focused time building a skill', points: 20, completed: false, genre: 'goal' },
      { id: 'tom-g3', description: 'Watch or read something educational', points: 10, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'tom-v1', description: "Summarize today's key takeaway", points: 10, completed: false, genre: 'goal' },
    ],
    tasksLastGeneratedDate: today,
    personaBreakdown: { Achievement: 10, Growth: 70, Connection: 5, Exploration: 8, Purpose: 4, Structure: 3 },
    weightedScores: { Achievement: 0.10, Growth: 0.70, Connection: 0.05, Exploration: 0.08, Purpose: 0.04, Structure: 0.03 },
    portfolioSummary: 'You are motivated by mastery and the feeling of getting better.',
    tips: ['Keep a learning log', 'Reflect on weekly improvements', 'Embrace discomfort as growth'],
    failurePatterns: ['Switching interests before mastering the current one'],
    confidenceScore: 0.86,
    createdAt: new Date(),
    updatedAt: new Date(),
  },
  {
    email: 'demo.lena@habitflow.ai',
    firstName: 'Lena',
    lastName: 'Novak',
    profilePicture: 'preset:6',
    password: DEMO_PASSWORD,
    authProvider: 'local',
    goal: 'Try something new every week',
    personaType: 'Explorer',
    motivationalMessage: 'Variety is where you thrive.',
    coreGoals: [
      { id: 'lena-g1', description: 'Break your usual routine today', points: 20, completed: false, genre: 'goal' },
      { id: 'lena-g2', description: "Research something you've never studied", points: 15, completed: false, genre: 'goal' },
      { id: 'lena-g3', description: 'Discover a new spot nearby', points: 15, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'lena-v1', description: 'Swap one habit for something different', points: 10, completed: false, genre: 'goal' },
    ],
    tasksLastGeneratedDate: today,
    personaBreakdown: { Achievement: 8, Growth: 12, Connection: 6, Exploration: 66, Purpose: 4, Structure: 4 },
    weightedScores: { Achievement: 0.08, Growth: 0.12, Connection: 0.06, Exploration: 0.66, Purpose: 0.04, Structure: 0.04 },
    portfolioSummary: 'Novelty fuels you — a rigid routine will kill your momentum.',
    tips: ['Rotate habits every few weeks', 'Set wildcard daily variations', 'Turn boredom into a trigger to explore'],
    failurePatterns: ['Abandoning habits the moment they feel repetitive'],
    confidenceScore: 0.79,
    createdAt: new Date(),
    updatedAt: new Date(),
  },
  {
    email: 'demo.dana@habitflow.ai',
    firstName: 'Dana',
    lastName: 'Shapiro',
    profilePicture: 'preset:7',
    password: DEMO_PASSWORD,
    authProvider: 'local',
    goal: 'Use my habits to make a positive impact on others',
    personaType: 'Altruist',
    motivationalMessage: 'Your habits ripple outward.',
    coreGoals: [
      { id: 'dana-g1', description: "Make someone's day better", points: 25, completed: false, genre: 'goal' },
      { id: 'dana-g2', description: 'Send a message of encouragement', points: 10, completed: false, genre: 'goal' },
      { id: 'dana-g3', description: 'Give back to a cause you care about', points: 15, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'dana-v1', description: 'Perform one small act of kindness', points: 10, completed: false, genre: 'goal' },
    ],
    tasksLastGeneratedDate: today,
    personaBreakdown: { Achievement: 5, Growth: 8, Connection: 12, Exploration: 5, Purpose: 66, Structure: 4 },
    weightedScores: { Achievement: 0.05, Growth: 0.08, Connection: 0.12, Exploration: 0.05, Purpose: 0.66, Structure: 0.04 },
    portfolioSummary: 'Your why is bigger than you — and that makes your habits sustainable.',
    tips: ['Tie each habit to someone you care about', 'Share your mission publicly', 'Track impact not just actions'],
    failurePatterns: ['Neglecting self-care while focusing on others'],
    confidenceScore: 0.83,
    createdAt: new Date(),
    updatedAt: new Date(),
  },
  {
    email: 'demo.ron@habitflow.ai',
    firstName: 'Ron',
    lastName: 'Katz',
    profilePicture: 'preset:8',
    password: DEMO_PASSWORD,
    authProvider: 'local',
    goal: 'Design and execute the perfect daily system',
    personaType: 'Architect',
    motivationalMessage: 'A good system beats motivation every time.',
    coreGoals: [
      { id: 'ron-g1', description: 'Plan tomorrow the night before', points: 15, completed: false, genre: 'goal' },
      { id: 'ron-g2', description: 'Stick to your system today', points: 20, completed: false, genre: 'goal' },
      { id: 'ron-g3', description: "Audit and refine this week's plan", points: 15, completed: false, genre: 'goal' },
    ],
    dailyVariations: [
      { id: 'ron-v1', description: 'Spend 10 minutes on your weekly review', points: 15, completed: false, genre: 'goal' },
    ],
    tasksLastGeneratedDate: today,
    personaBreakdown: { Achievement: 8, Growth: 10, Connection: 4, Exploration: 4, Purpose: 6, Structure: 68 },
    weightedScores: { Achievement: 0.08, Growth: 0.10, Connection: 0.04, Exploration: 0.04, Purpose: 0.06, Structure: 0.68 },
    portfolioSummary: 'You thrive on order. A broken routine costs you more than most.',
    tips: ['Time-block your habit slots', 'Review and adjust your system weekly', 'Treat deviations as data not failure'],
    failurePatterns: ['Paralysis when the schedule breaks unexpectedly'],
    confidenceScore: 0.90,
    createdAt: new Date(),
    updatedAt: new Date(),
  },
];

const existingEmails = db.users.distinct('email', { email: { $in: users.map(u => u.email) } });
const toInsert = users.filter(u => !existingEmails.includes(u.email));
if (toInsert.length > 0) {
  db.users.insertMany(toInsert);
}

const COACH_ID = ObjectId('000000000000000000000c0a');
if (!db.users.findOne({ _id: COACH_ID })) {
  db.users.insertOne({
    _id: COACH_ID,
    email: 'coach@habitflow.ai',
    firstName: 'HabitFlow',
    lastName: 'Coach',
    password: DEMO_PASSWORD,
    authProvider: 'local',
    goal: 'Help every user keep their habits',
    personaType: 'Architect',
    motivationalMessage: 'Small steps, every day.',
    coreGoals: [],
    dailyVariations: [],
    tasksLastGeneratedDate: today,
    createdAt: new Date(),
    updatedAt: new Date(),
  });
}

const demoUserIds = {};
db.users.find({ email: { $in: users.map(u => u.email) } }, { email: 1 }).forEach(u => {
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

if (alexId && mayaId && !db.chats.findOne({ isGroup: false, participantIds: { $all: [alexId, mayaId], $size: 2 } })) {
  const directChatId = db.chats.insertOne({
    participantIds: [alexId, mayaId],
    isGroup: false,
    admins: [],
    unreadCount: {},
    createdAt: new Date(),
    updatedAt: new Date(),
  }).insertedId;

  db.messages.insertOne({
    chatId: directChatId.toString(),
    senderId: alexId,
    text: 'Hey! Saw your streak — nice work this week.',
    likes: [],
    createdAt: new Date(),
    updatedAt: new Date(),
  });

  const directLastMsgId = db.messages.insertOne({
    chatId: directChatId.toString(),
    senderId: mayaId,
    text: 'Thanks! Trying to keep it going through the weekend.',
    likes: [alexId],
    createdAt: new Date(),
    updatedAt: new Date(),
  }).insertedId;

  db.chats.updateOne({ _id: directChatId }, { $set: { lastMessage: directLastMsgId.toString() } });
}

if (alexId && mayaId && benId && saraId && !db.chats.findOne({ isGroup: true, name: 'Habit Squad' })) {
  const groupChatId = db.chats.insertOne({
    participantIds: [alexId, mayaId, benId, saraId],
    isGroup: true,
    name: 'Habit Squad',
    admins: [alexId],
    owner: alexId,
    description: 'Keeping each other accountable.',
    unreadCount: {},
    createdAt: new Date(),
    updatedAt: new Date(),
  }).insertedId;

  db.messages.insertOne({
    chatId: groupChatId.toString(),
    senderId: alexId,
    text: "Let's crush this week!",
    likes: [],
    createdAt: new Date(),
    updatedAt: new Date(),
  });

  const groupLastMsgId = db.messages.insertOne({
    chatId: groupChatId.toString(),
    senderId: saraId,
    text: "I'm in — checking off my morning routine now.",
    likes: [alexId, mayaId, benId],
    createdAt: new Date(),
    updatedAt: new Date(),
  }).insertedId;

  db.chats.updateOne({ _id: groupChatId }, { $set: { lastMessage: groupLastMsgId.toString() } });
}

if (alexId && mayaId && benId && saraId && db.posts.countDocuments() === 0) {
  const postDefs = [
    { authorId: ObjectId(alexId), habitName: '5km morning run', completionNote: 'New personal best today!', likes: [mayaId, benId], imageUrl: '/uploads/demo/alex-morning-run.jpg' },
    { authorId: ObjectId(mayaId), habitName: 'Daily meditation (15 min)', completionNote: 'Stayed calm through a stressful day.', likes: [alexId, benId], imageUrl: '/uploads/demo/maya-meditation.jpg' },
    { authorId: ObjectId(benId), habitName: 'Group workout session', completionNote: 'Got the whole crew moving today.', likes: [alexId, mayaId, saraId], imageUrl: '/uploads/demo/ben-group-workout.jpg' },
    { authorId: ObjectId(saraId), habitName: 'Join a community challenge', completionNote: 'Signed up for the 30-day challenge!', likes: [mayaId] },
  ];

  // authorId/postId/userId are ObjectId refs on these schemas (Post.authorId,
  // Comment.postId, Comment.userId) — the $lookup aggregations that resolve
  // author/commenter names and fetch a post's comments do a type-sensitive
  // match, so these can't be plain strings like the userId fields elsewhere
  // in this file (Habit/Goal/LocationRecord/Follow all declare userId as
  // plain String, so those stay as-is).
  const postIds = postDefs.map(def => db.posts.insertOne({
    ...def,
    createdAt: new Date(),
    updatedAt: new Date(),
  }).insertedId);

  const commentDefs = [
    { postId: postIds[0], userId: ObjectId(mayaId), text: 'Incredible pace!' },
    { postId: postIds[0], userId: ObjectId(saraId), text: 'Beat your own record next!' },
    { postId: postIds[2], userId: ObjectId(saraId), text: 'Count me in next week.' },
    { postId: postIds[3], userId: ObjectId(mayaId), text: 'So proud of you for stepping up!' },
    { postId: postIds[3], userId: ObjectId(alexId), text: "Which challenge? I'm interested!" },
  ];
  commentDefs.forEach(def => db.comments.insertOne({ ...def, createdAt: new Date(), updatedAt: new Date() }));
}

// Backdated posts — separate, own-guarded block (the block above only fires once,
// when `posts` is completely empty, so a shared dev DB that's already been seeded
// needs its own trigger). Dates are chosen to exercise every tier of the relative-time
// formatter (apps/android/.../util/RelativeTime.kt): >1yr falls back to a full date,
// ~6mo hits the comment-only "Xmo" tier, ~3wk hits "Xd" for comments (posts cap "Xd" at
// under a week, so both older posts show a full date instead — expected, not a bug).
if (tomId && lenaId && danaId && ronId && db.posts.countDocuments({ authorId: ObjectId(tomId) }) === 0) {
  const backdatedPostDefs = [
    {
      authorId: ObjectId(tomId),
      habitName: 'Read 20 pages',
      completionNote: 'Finished another chapter before bed — building the habit slowly.',
      likes: [lenaId, danaId],
      createdAt: new Date('2024-12-28T09:15:00Z'),
    },
    {
      authorId: ObjectId(lenaId),
      habitName: 'Try a new route or activity',
      completionNote: 'Explored a trail on the other side of the city I had never been to.',
      likes: [tomId, ronId],
      createdAt: new Date('2026-02-13T18:40:00Z'),
    },
    {
      authorId: ObjectId(danaId),
      habitName: 'Volunteer or help someone today',
      completionNote: 'Spent the afternoon helping out at the community shelter.',
      likes: [lenaId, mayaId],
      createdAt: new Date('2026-07-27T12:05:00Z'),
    },
  ];

  const backdatedPostIds = backdatedPostDefs.map(def => db.posts.insertOne({
    ...def,
    updatedAt: def.createdAt,
  }).insertedId);

  const hoursAfter = (date, hours) => new Date(date.getTime() + hours * 60 * 60 * 1000);

  const backdatedCommentDefs = [
    { postId: backdatedPostIds[0], userId: ObjectId(lenaId), text: 'Love this — what are you reading?', createdAt: hoursAfter(backdatedPostDefs[0].createdAt, 3) },
    { postId: backdatedPostIds[0], userId: ObjectId(ronId), text: 'Consistency is everything, keep it up.', createdAt: hoursAfter(backdatedPostDefs[0].createdAt, 26) },
    { postId: backdatedPostIds[1], userId: ObjectId(tomId), text: 'That sounds amazing, where was it?', createdAt: hoursAfter(backdatedPostDefs[1].createdAt, 2) },
    { postId: backdatedPostIds[1], userId: ObjectId(danaId), text: 'Adding this to my list!', createdAt: hoursAfter(backdatedPostDefs[1].createdAt, 20) },
    { postId: backdatedPostIds[2], userId: ObjectId(lenaId), text: 'This is so inspiring.', createdAt: hoursAfter(backdatedPostDefs[2].createdAt, 4) },
    { postId: backdatedPostIds[2], userId: ObjectId(ronId), text: 'Proud of you for doing this.', createdAt: hoursAfter(backdatedPostDefs[2].createdAt, 22) },
    { postId: backdatedPostIds[2], userId: ObjectId(mayaId), text: 'Count me in next time!', createdAt: hoursAfter(backdatedPostDefs[2].createdAt, 30) },
  ];
  backdatedCommentDefs.forEach(def => db.comments.insertOne({ ...def, updatedAt: def.createdAt }));
}

if (alexId && mayaId && benId && saraId && db.follows.countDocuments() === 0) {
  const followDefs = [
    { followerId: mayaId, followingId: alexId },
    { followerId: benId, followingId: alexId },
    { followerId: saraId, followingId: mayaId },
    { followerId: alexId, followingId: saraId },
  ];
  followDefs.forEach(def => db.follows.insertOne({ ...def, createdAt: new Date(), updatedAt: new Date() }));
}

const EWMA_ALPHA = 0.2;
const IMPLEMENTED_MIN_DAYS = 28;
const IMPLEMENTED_MIN_SCORE = 0.8;

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
function buildHabit({ userId, persona, title, description, frequency, createdAtDaysAgo, history, goalId, notes }) {
  const createdAtStr = daysAgoStr(createdAtDaysAgo);
  const consistencyScore = calcConsistency(history, createdAtStr);
  const implemented = daysBetween(createdAtStr) >= IMPLEMENTED_MIN_DAYS && consistencyScore >= IMPLEMENTED_MIN_SCORE;
  return {
    userId,
    title,
    description: description || '',
    frequency,
    targetCount: 1,
    streak: calcStreak(history),
    completionHistory: history,
    persona,
    isArchived: false,
    ...(goalId ? { goalId } : {}),
    consistencyScore,
    ...(implemented ? { implementedAt: new Date() } : {}),
    completionNotes: notes || [],
    createdAt: daysAgoDate(createdAtDaysAgo),
    updatedAt: new Date(),
  };
}

let goalIds = {};
if (alexId && mayaId && benId && saraId && tomId && lenaId && danaId && ronId && db.goals.countDocuments() === 0) {
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
  goalDefs.forEach(def => {
    const goalId = db.goals.insertOne({
      userId: def.userId,
      title: def.title,
      targetDate: def.targetDate,
      status: def.status,
      createdAt: daysAgoDate(40),
      updatedAt: new Date(),
    }).insertedId;
    goalIds[def.key] = goalId.toString();

    if (def.status === 'achieved') {
      db.users.updateOne(
        { _id: ObjectId(def.userId) },
        { $push: { achievements: { goalId: goalIds[def.key], medal: def.medal, awardedAt: def.awardedAt } } },
      );
    }
  });
}

if (alexId && mayaId && benId && saraId && tomId && lenaId && danaId && ronId && db.habits.countDocuments() === 0) {
  const habitDefs = [
    buildHabit({
      userId: alexId, persona: 'Achiever', title: '5km morning run', frequency: 'daily',
      createdAtDaysAgo: 40, history: historyLastNDays(32, [5, 19]), goalId: goalIds.alex,
      notes: [{ date: today, note: 'New personal best pace today!' }],
    }),
    buildHabit({
      userId: alexId, persona: 'Achiever', title: 'Log workout stats in app', frequency: 'daily',
      createdAtDaysAgo: 35, history: historyLastNDays(30), goalId: goalIds.alexAchieved,
    }),
    // Under 28 days old so implementedAt stays unset - shows Task 77's manual
    // "Complete Habit" button (21-day streak) instead of auto-achieving.
    buildHabit({
      userId: alexId, persona: 'Achiever', title: '100 push-ups', frequency: 'daily',
      createdAtDaysAgo: 25, history: historyLastNDays(25), goalId: goalIds.alex,
    }),
    buildHabit({
      userId: alexId, persona: 'Achiever', title: 'Cold shower', frequency: 'daily',
      createdAtDaysAgo: 15, history: historyLastNDays(15, [1, 4, 7, 10, 13]),
    }),
    buildHabit({
      userId: mayaId, persona: 'Achiever', title: 'Daily meditation (15 min)', frequency: 'daily',
      createdAtDaysAgo: 35, history: historyLastNDays(33, [12]), goalId: goalIds.maya,
      notes: [{ date: daysAgoStr(2), note: 'Hit the full 30 days — sticking with it.' }],
    }),
    buildHabit({
      userId: mayaId, persona: 'Achiever', title: 'Evening walk (20 min)', frequency: 'daily',
      createdAtDaysAgo: 9, history: historyLastNDays(9, [2, 6]),
    }),
    buildHabit({
      userId: benId, persona: 'Socializer', title: 'Check in on a friend', frequency: 'daily',
      createdAtDaysAgo: 35, history: historyLastNDays(30), goalId: goalIds.benAchieved,
    }),
    // No completions in the last 10 days — this gap is why Ben's goal ended up forfeited.
    buildHabit({
      userId: benId, persona: 'Socializer', title: 'Group workout session', frequency: 'weekly',
      createdAtDaysAgo: 30, history: historyLastNDays(30, [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]), goalId: goalIds.ben,
    }),
    buildHabit({
      userId: saraId, persona: 'Socializer', title: 'Join a community challenge', frequency: 'weekly',
      createdAtDaysAgo: 11, history: historyLastNDays(11, [3, 8]), goalId: goalIds.sara,
    }),
    buildHabit({
      userId: saraId, persona: 'Socializer', title: 'Comment or react on 3 posts', frequency: 'daily',
      createdAtDaysAgo: 35, history: historyLastNDays(30), goalId: goalIds.saraAchieved,
    }),
    buildHabit({
      userId: tomId, persona: 'Grower', title: 'Read 20 pages', frequency: 'daily',
      createdAtDaysAgo: 35, history: historyLastNDays(30), goalId: goalIds.tom,
      notes: [{ date: daysAgoStr(6), note: 'Finished the book!' }],
    }),
    buildHabit({
      userId: lenaId, persona: 'Explorer', title: 'Try a new route or activity', frequency: 'weekly',
      createdAtDaysAgo: 35, history: historyLastNDays(30), goalId: goalIds.lena,
    }),
    buildHabit({
      userId: danaId, persona: 'Altruist', title: 'Volunteer or help someone today', frequency: 'daily',
      createdAtDaysAgo: 35, history: historyLastNDays(30), goalId: goalIds.dana,
    }),
    buildHabit({
      userId: ronId, persona: 'Architect', title: 'Follow morning routine without skips', frequency: 'daily',
      createdAtDaysAgo: 35, history: historyLastNDays(30), goalId: goalIds.ron,
    }),
  ];
  db.habits.insertMany(habitDefs);
}

// Gives Alex a few days of "Activity History" right away instead of waiting for a
// real day-transition to archive one (see UsersService.getHomePageData).
if (alexId && db.habits.countDocuments({ userId: alexId }) > 0 && !db.users.findOne({ _id: ObjectId(alexId), 'taskHistory.0': { $exists: true } })) {
  const habitIdByTitle = (title) => {
    const habit = db.habits.findOne({ userId: alexId, title });
    return habit ? habit._id.toString() : undefined;
  };
  const runHabitId = habitIdByTitle('5km morning run');
  const pushupsHabitId = habitIdByTitle('100 push-ups');
  const logStatsHabitId = habitIdByTitle('Log workout stats in app');
  const coldShowerHabitId = habitIdByTitle('Cold shower');

  // completed flags below match each habit's real completionHistory above, so the
  // streak view and this history view agree: run skips day 5 ago, cold shower skips
  // days 1 and 4 ago, push-ups/log-stats have no gaps in this window.
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

  const taskHistory = taskHistoryDefs.map(def => ({ date: daysAgoStr(def.daysAgo), tasks: def.tasks }));
  db.users.updateOne({ _id: ObjectId(alexId) }, { $set: { taskHistory } });
}

if (alexId && mayaId && benId && saraId && db.locationrecords.countDocuments() === 0) {
  const alexRunHabit = db.habits.findOne({ userId: alexId, title: '5km morning run' });
  const mayaMeditationHabit = db.habits.findOne({ userId: mayaId, title: 'Daily meditation (15 min)' });
  const benWorkoutHabit = db.habits.findOne({ userId: benId, title: 'Group workout session' });
  const saraChallengeHabit = db.habits.findOne({ userId: saraId, title: 'Join a community challenge' });
  const alexRunHabitId = alexRunHabit ? alexRunHabit._id.toString() : undefined;
  const mayaMeditationHabitId = mayaMeditationHabit ? mayaMeditationHabit._id.toString() : undefined;
  const benWorkoutHabitId = benWorkoutHabit ? benWorkoutHabit._id.toString() : undefined;
  const saraChallengeHabitId = saraChallengeHabit ? saraChallengeHabit._id.toString() : undefined;

  const locationDefs = [
    { userId: alexId, habitId: alexRunHabitId, taskDescription: '5km morning run', latitude: 32.0880, longitude: 34.7801, daysAgo: 0, personaType: 'Achiever' },
    { userId: alexId, habitId: alexRunHabitId, taskDescription: '5km morning run', latitude: 32.0902, longitude: 34.7838, daysAgo: 1, personaType: 'Achiever' },
    { userId: mayaId, habitId: mayaMeditationHabitId, taskDescription: 'Daily meditation (15 min)', latitude: 32.0838, longitude: 34.7679, daysAgo: 1, personaType: 'Achiever' },
    // Matches the completion gap above, not a fresh visit.
    { userId: benId, habitId: benWorkoutHabitId, taskDescription: 'Group workout session', latitude: 32.0925, longitude: 34.7845, daysAgo: 10, personaType: 'Socializer' },
    { userId: saraId, habitId: saraChallengeHabitId, taskDescription: 'Join a community challenge', latitude: 32.0797, longitude: 34.7746, daysAgo: 0, personaType: 'Socializer' },
  ];

  locationDefs.forEach(def => {
    const timestamp = daysAgoDate(def.daysAgo).getTime();
    db.locationrecords.insertOne({
      userId: def.userId,
      habitId: def.habitId,
      taskDescription: def.taskDescription,
      latitude: def.latitude,
      longitude: def.longitude,
      timestamp,
      personaType: def.personaType,
      isPublic: true,
      createdAt: new Date(timestamp),
      updatedAt: new Date(timestamp),
    });
  });
}

if (benId && db.driftflags.countDocuments() === 0) {
  db.driftflags.insertOne({
    userId: benId,
    detectedAt: daysAgoDate(1),
    driftScore: 0.42,
    suggestedPersona: 'Grower',
    dismissed: false,
    createdAt: daysAgoDate(1),
    updatedAt: daysAgoDate(1),
  });
}

// GET /leaderboard reads only LeaderboardMonth (running totals), never
// LeaderboardWeek — a real completion later just $inc's on top of these,
// consistent with the "running total" design, so seeding month docs alone
// is enough for the table to show real standings without fabricating
// day-by-day week data nothing currently reads.
if (alexId && db.leaderboardmonths.countDocuments() === 0) {
  const monthStart = `${today.slice(0, 7)}-01`;
  const monthPoints = {
    [alexId]: 5450,
    [mayaId]: 4800,
    [ronId]: 4100,
    [danaId]: 3600,
    [tomId]: 3150,
    [saraId]: 2700,
    [benId]: 2200,
    [lenaId]: 1650,
  };
  Object.entries(monthPoints).forEach(([userId, points]) => {
    if (!userId || userId === 'undefined') return;
    db.leaderboardmonths.insertOne({
      userId,
      monthStart,
      monthPoints: points,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });
}

const DEMO_PASSWORD = '$2b$10$whSSki94nh9/sTltEVroZu3czQD0/bHwAL3XL3HxDwDaQRahe6y5K';
const today = new Date().toISOString().split('T')[0];

const users = [
  {
    email: 'demo.alex@habitflow.ai',
    password: DEMO_PASSWORD,
    goal: 'Break personal records and dominate every leaderboard',
    personaType: 'Achiever',
    motivationalMessage: 'Champions are made in the moments they want to quit.',
    coreGoals: [
      { id: 'alex-g1', description: '5km morning run', points: 25, completed: false },
      { id: 'alex-g2', description: '100 push-ups', points: 20, completed: false },
      { id: 'alex-g3', description: 'Log workout stats in app', points: 10, completed: false },
    ],
    dailyVariations: [
      { id: 'alex-v1', description: 'Cold shower', points: 10, completed: false },
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
    password: DEMO_PASSWORD,
    goal: 'Be the most consistent person in every challenge',
    personaType: 'Achiever',
    motivationalMessage: 'Your streak is your signature.',
    coreGoals: [
      { id: 'maya-g1', description: 'Daily meditation (15 min)', points: 15, completed: false },
      { id: 'maya-g2', description: 'Finish daily task list', points: 20, completed: false },
      { id: 'maya-g3', description: 'Evening walk (20 min)', points: 15, completed: false },
    ],
    dailyVariations: [
      { id: 'maya-v1', description: 'Gratitude journal (5 min)', points: 10, completed: false },
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
    password: DEMO_PASSWORD,
    goal: 'Build habits that bring people together',
    personaType: 'Socializer',
    motivationalMessage: 'Your energy lifts everyone around you.',
    coreGoals: [
      { id: 'ben-g1', description: 'Check in on a friend', points: 15, completed: false },
      { id: 'ben-g2', description: 'Group workout session', points: 20, completed: false },
      { id: 'ben-g3', description: 'Post a habit update to the feed', points: 10, completed: false },
    ],
    dailyVariations: [
      { id: 'ben-v1', description: 'Share a motivational post', points: 10, completed: false },
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
    password: DEMO_PASSWORD,
    goal: 'Stay consistent through community and connection',
    personaType: 'Socializer',
    motivationalMessage: 'You grow when you grow with others.',
    coreGoals: [
      { id: 'sara-g1', description: 'Join a community challenge', points: 20, completed: false },
      { id: 'sara-g2', description: 'Comment or react on 3 posts', points: 10, completed: false },
      { id: 'sara-g3', description: 'Invite someone to a habit', points: 15, completed: false },
    ],
    dailyVariations: [
      { id: 'sara-v1', description: 'Voice note to a friend', points: 10, completed: false },
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
    password: DEMO_PASSWORD,
    goal: 'Become 1% better every single day',
    personaType: 'Grower',
    motivationalMessage: 'Progress is the point.',
    coreGoals: [
      { id: 'tom-g1', description: 'Read 20 pages', points: 15, completed: false },
      { id: 'tom-g2', description: 'Practice a new skill (30 min)', points: 20, completed: false },
      { id: 'tom-g3', description: 'Watch an educational video', points: 10, completed: false },
    ],
    dailyVariations: [
      { id: 'tom-v1', description: 'Review what you learned today', points: 10, completed: false },
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
    password: DEMO_PASSWORD,
    goal: 'Try something new every week',
    personaType: 'Explorer',
    motivationalMessage: 'Variety is where you thrive.',
    coreGoals: [
      { id: 'lena-g1', description: 'Try a new route or activity', points: 20, completed: false },
      { id: 'lena-g2', description: 'Explore a topic you have never studied', points: 15, completed: false },
      { id: 'lena-g3', description: 'Visit a new place', points: 15, completed: false },
    ],
    dailyVariations: [
      { id: 'lena-v1', description: 'Switch up one element of your routine', points: 10, completed: false },
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
    password: DEMO_PASSWORD,
    goal: 'Use my habits to make a positive impact on others',
    personaType: 'Altruist',
    motivationalMessage: 'Your habits ripple outward.',
    coreGoals: [
      { id: 'dana-g1', description: 'Volunteer or help someone today', points: 25, completed: false },
      { id: 'dana-g2', description: 'Write a kind message to someone', points: 10, completed: false },
      { id: 'dana-g3', description: 'Donate or contribute to a cause', points: 15, completed: false },
    ],
    dailyVariations: [
      { id: 'dana-v1', description: 'Do one small act of service', points: 10, completed: false },
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
    password: DEMO_PASSWORD,
    goal: 'Design and execute the perfect daily system',
    personaType: 'Architect',
    motivationalMessage: 'A good system beats motivation every time.',
    coreGoals: [
      { id: 'ron-g1', description: 'Plan tomorrow the night before', points: 15, completed: false },
      { id: 'ron-g2', description: 'Follow morning routine without skips', points: 20, completed: false },
      { id: 'ron-g3', description: 'Review and update weekly plan', points: 15, completed: false },
    ],
    dailyVariations: [
      { id: 'ron-v1', description: 'Weekly review (10 min)', points: 15, completed: false },
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

// Fixed id — must stay in sync with COACH_USER_ID in apps/backend/src/coach/coach.templates.ts
const COACH_ID = ObjectId('000000000000000000000c0a');
if (!db.users.findOne({ _id: COACH_ID })) {
  db.users.insertOne({
    _id: COACH_ID,
    email: 'coach@habitflow.ai',
    password: DEMO_PASSWORD,
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
    { authorId: alexId, habitName: '5km morning run', completionNote: 'New personal best today!', likes: [mayaId, benId] },
    { authorId: mayaId, habitName: 'Daily meditation (15 min)', completionNote: 'Stayed calm through a stressful day.', likes: [alexId, benId] },
    { authorId: benId, habitName: 'Group workout session', completionNote: 'Got the whole crew moving today.', likes: [alexId, mayaId, saraId] },
    { authorId: saraId, habitName: 'Join a community challenge', completionNote: 'Signed up for the 30-day challenge!', likes: [mayaId] },
  ];

  const postIds = postDefs.map(def => db.posts.insertOne({
    ...def,
    createdAt: new Date(),
    updatedAt: new Date(),
  }).insertedId);

  // Mix of interaction types across the 8 seeded likes: 5 liked-only (Ben/post0,
  // Alex+Ben/post1, Alex+Maya/post2), 3 liked-and-commented (Maya/post0, Sara/post2,
  // Maya/post3), 2 commented-without-liking (Sara/post0, Alex/post3) - not everyone
  // who liked a post commented on it too.
  const commentDefs = [
    { postId: postIds[0].toString(), userId: mayaId, text: 'Incredible pace!' },
    { postId: postIds[0].toString(), userId: saraId, text: 'Beat your own record next!' },
    { postId: postIds[2].toString(), userId: saraId, text: 'Count me in next week.' },
    { postId: postIds[3].toString(), userId: mayaId, text: 'So proud of you for stepping up!' },
    { postId: postIds[3].toString(), userId: alexId, text: "Which challenge? I'm interested!" },
  ];
  commentDefs.forEach(def => db.comments.insertOne({ ...def, createdAt: new Date(), updatedAt: new Date() }));
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


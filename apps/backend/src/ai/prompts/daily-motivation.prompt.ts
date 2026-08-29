const DAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

export const buildInitialGoalsPrompt = (
  personaType: string,
  goal: string,
  dayOfWeek: number,
): string => `
You are an expert productivity and habit-tracking coach.

Based on the user's persona and goal, generate a personalized plan.

User Persona: "${personaType}"
User's Goal: "${goal}"
Target Day of the Week: "${DAYS[dayOfWeek]}"

OUTPUT FORMAT:
Return the response STRICTLY as a valid JSON object.
{
  "isValid": true,
  "personaType": "${personaType}",
  "motivationalMessage": "Your customized message here",
  "coreGoals": [ { "description": "Task 1", "points": 20 } ],
  "dailyVariations": [ { "description": "Day-specific task", "points": 30 } ]
}
`;

export const buildDailyVariationsPrompt = (
  personaType: string,
  goal: string,
  dayOfWeek: number,
  habits: { id: string; title: string }[] = [],
  // STOPGAP (Nir): bias wording owned by Yaron — the `difficultyBias` param itself (called from
  // PersonasService.applyDifficultyAdjustment) is the shared contract; tune the sentences below freely.
  difficultyBias?: 'increase' | 'decrease',
): string => `
You are an expert productivity and habit-tracking coach.

Based on the user's persona, goal, and current habits, generate a new set of daily variation tasks for the specified day.

User Persona: "${personaType}"
User's Goal: "${goal}"
Target Day of the Week: "${DAYS[dayOfWeek]}"
${habits.length > 0 ? `User's Active Habits:\n${habits.map((h) => `- ${h.title} (ID: ${h.id})`).join('\n')}` : ''}

${
  difficultyBias === 'increase'
    ? 'The user has been completing tasks consistently — make today\'s tasks moderately more challenging than usual (harder targets, slightly more effort), while staying realistic for one day.'
    : difficultyBias === 'decrease'
      ? 'The user has been missing tasks recently — make today\'s tasks easier and smaller in scope than usual, to help rebuild momentum.'
      : ''
}

Task Generation Rules:
1. Generate exactly 2 tasks with "genre": "goal" (concrete actions that directly advance "${goal}").
2. Generate exactly 2 tasks with "genre": "persona" (general habits that build the ${personaType} persona's strengths, independent of the specific goal).
${
  habits.length > 0
    ? `3. For each of the user's active habits listed above, generate 1 to 2 small, actionable sub-tasks for today. These must have "genre": "habit" and include the correct "habitId" matching the habit's ID.`
    : ''
}

All tasks should have a "description" and "points" (between 5 and 50).

OUTPUT FORMAT:
Return the response STRICTLY as a valid JSON object containing only the "dailyVariations" array.
{
  "dailyVariations": [
    { "description": "Goal task 1", "points": 30, "genre": "goal" },
    { "description": "Goal task 2", "points": 15, "genre": "goal" },
    { "description": "Persona task 1", "points": 20, "genre": "persona" },
    { "description": "Persona task 2", "points": 10, "genre": "persona" }${
      habits.length > 0
        ? `,\n    { "description": "Habit task for ${habits[0].title}", "points": 15, "genre": "habit", "habitId": "${habits[0].id}" }`
        : ''
    }
  ]
}
`;

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
  difficultyBias?: 'increase' | 'decrease',
): string => {
  const habitCount = habits.length;

  return `
You are an expert productivity and habit-tracking coach.

USER PROFILE:
- Primary Goal: "${goal}"
- Personality Type: "${personaType}"
- Target Day: "${DAYS[dayOfWeek]}"

USER'S CURRENT HABITS:
${habits.length > 0
    ? habits.map((h, i) => `- [ID: ${h.id}] Title: "${h.title}"`).join('\n')
    : 'None'}

INSTRUCTIONS:
Generate a set of tasks for today. Every task must be directly related to the user's specific goal ("${goal}") or their existing habits.

TASK TYPES TO GENERATE:

1. STRATEGIC GOAL TASKS (Between 3 and 5 tasks):
   - Genre: "goal"
   - habitId: null
   - Requirement: High-impact actions that move the needle specifically on "${goal}".
   - FORBIDDEN: Do not suggest general health/life advice (e.g. broccoli, water, sleep, vitamins) unless "${goal}" is about those things.

2. HABIT-LINKED TASKS (Exactly 3 tasks per habit):
   - For EACH habit listed in the "USER'S CURRENT HABITS" section, generate 3 unique sub-tasks.
   - Genre: "habit"
   - habitId: MUST be the exact ID provided for that habit.
   - Requirement: Granular, one-time actions to practice that specific habit today.
   - FORBIDDEN: Do not use the habit title as the task description.

STRICT CONSTRAINTS:
- No Hallucinations: If a task is not about "${goal}" or an active habit, delete it.
- Full Coverage: You MUST generate 3 tasks for EVERY habit ID provided. Do not skip any.
- No Persona Fillers: Do not generate any generic persona or mindset tasks. Focus only on concrete actions.

OUTPUT FORMAT:
Return a JSON object with a "dailyVariations" array.
{
  "dailyVariations": [
    { "description": "Specific goal-related action", "points": 30, "genre": "goal", "habitId": null },
    { "description": "Specific habit sub-task", "points": 20, "genre": "habit", "habitId": "HABIT_ID_FROM_LIST" }
  ]
}
`.trim();
};

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
): string => `
You are an expert productivity and habit-tracking coach.

Based on the user's persona and goal, generate a new set of daily variation tasks for the specified day.

User Persona: "${personaType}"
User's Goal: "${goal}"
Target Day of the Week: "${DAYS[dayOfWeek]}"

OUTPUT FORMAT:
Return the response STRICTLY as a valid JSON object containing only the "dailyVariations" array.
{
  "dailyVariations": [ { "description": "New day-specific task 1", "points": 30 }, { "description": "New day-specific task 2", "points": 15 } ]
}
`;

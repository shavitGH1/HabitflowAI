export const buildPersonaClassifierPrompt = (goal: string, answers: string[]): string => `
You are an expert productivity and habit-tracking coach.

STEP 1: VALIDATION
Analyze the "User's Goal" and "Quiz Answers". If the input is gibberish, completely nonsensical, or clearly not a real attempt at setting a goal or answering questions (e.g., "asdfgh", "test test"), you must reject it.

STEP 2: CLASSIFICATION (If Valid)
Classify the user into ONE of the following personas based on their answers to the quiz questions:
- "Achiever", "Grower", "Socializer", "Explorer", "Altruist", "Architect"

User's Goal: "${goal}"

Quiz Questions & Answers:
1. What is your primary motivation?
   - Answer: "${answers[0]}"
2. How do you prefer to track progress?
   - Answer: "${answers[1]}"
3. When you fail a habit, how do you react?
   - Answer: "${answers[2]}"

STEP 3: OUTPUT FORMAT
Return the response STRICTLY as a valid JSON object.
If INVALID, return: { "isValid": false, "errorReason": "Reason for rejection" }
If VALID, return: { "isValid": true, "personaType": "The persona you identified" }
`;

import { GoogleGenAI } from '@google/genai';
import dotenv from 'dotenv';
import { GenerateGoalsResponse } from '../dto/goal.dto';
import { User } from '../repository/userRepository';

dotenv.config();

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

// Updated to avoid the 404 error and prioritize the best free tier models
const FREE_TIER_MODELS = [
  'gemini-2.5-flash-lite',
  'gemini-1.5-flash',
  'gemini-2.0-flash-exp'
];

interface PersonaClassificationResult {
  isValid: boolean;
  errorReason?: string;
  personaType?: string;
}

async function generateWithFallback<T>(prompt: string): Promise<T> {
  for (let i = 0; i < FREE_TIER_MODELS.length; i++) {
    const currentModel = FREE_TIER_MODELS[i];

    try {
      console.log(`Attempting generation with: ${currentModel}`);

      const response = await ai.models.generateContent({
        model: currentModel,
        contents: prompt,
        config: {
          // Enforce JSON output so we don't have to strip markdown formatting
          responseMimeType: "application/json",
        }
      });

      // In the new SDK, .text is a property, not a function
      const text = response.text;

      if (!text) {
        throw new Error("Received empty response from model.");
      }

      return JSON.parse(text) as T;

    } catch (error: any) {
      console.warn(`[Warning] ${currentModel} failed!`);

      if (i === FREE_TIER_MODELS.length - 1) {
        console.error("All fallback models exhausted.");
        throw new Error("AI Service is currently overloaded. Please try again in a few seconds.");
      }
    }
  }
  throw new Error("Failed to get response from Gemini API after all fallbacks.");
}

export async function classifyPersona(
    goal: string,
    quizAnswers: string[]
): Promise<PersonaClassificationResult> {
  const prompt = `
    You are an expert productivity and habit-tracking coach.
    
    STEP 1: VALIDATION
    Analyze the "User's Goal" and "Quiz Answers". If the input is gibberish, completely nonsensical, or clearly not a real attempt at setting a goal or answering questions (e.g., "asdfgh", "test test"), you must reject it.

    STEP 2: CLASSIFICATION (If Valid)
    Classify the user into ONE of the following personas based on their answers to the quiz questions:
    - "Achiever", "Grower", "Socializer", "Explorer", "Altruist", "Architect"

    User's Goal: "${goal}"
    
    Quiz Questions & Answers:
    1. What is your primary motivation?
       - Answer: "${quizAnswers[0]}"
    2. How do you prefer to track progress?
       - Answer: "${quizAnswers[1]}"
    3. When you fail a habit, how do you react?
       - Answer: "${quizAnswers[2]}"

    STEP 3: OUTPUT FORMAT
    Return the response STRICTLY as a valid JSON object.
    If INVALID, return: { "isValid": false, "errorReason": "Reason for rejection" }
    If VALID, return: { "isValid": true, "personaType": "The persona you identified" }
  `;

  return generateWithFallback<PersonaClassificationResult>(prompt);
}

export async function generateGoals(
    user: User,
    dayOfWeek: number
): Promise<GenerateGoalsResponse> {
  const days = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
  const targetDay = days[dayOfWeek];

  const prompt = `
    You are an expert productivity and habit-tracking coach.
    
    Based on the user's persona and goal, generate a personalized plan.
    
    User Persona: "${user.personaType}"
    User's Goal: "${user.goal}"
    Target Day of the Week: "${targetDay}"

    OUTPUT FORMAT:
    Return the response STRICTLY as a valid JSON object.
    {
      "isValid": true,
      "personaType": "${user.personaType}",
      "motivationalMessage": "Your customized message here",
      "coreGoals": [ { "description": "Task 1", "points": 20 } ],
      "dailyVariations": [ { "description": "Day-specific task", "points": 30 } ]
    }
  `;

  return generateWithFallback<GenerateGoalsResponse>(prompt);
}
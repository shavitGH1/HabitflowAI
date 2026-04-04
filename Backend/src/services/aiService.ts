import { GoogleGenerativeAI } from '@google/generative-ai';
import dotenv from 'dotenv';

dotenv.config();

const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY as string);

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export async function classifyUserPersona(goal: string, answers: string[]): Promise<{ personaType: 'Architect' | 'Achiever'; motivationalMessage: string; }> {
  const model = genAI.getGenerativeModel({ model: "gemini-flash-latest" });

  const prompt = `
    Analyze the user's habit goal and personality quiz answers to classify them as either an 'Architect' or an 'Achiever'.
    - 'Architect' personas are planners, strategists, and enjoy building systems for success.
    - 'Achiever' personas are goal-oriented, competitive, and motivated by tangible results.
    The user responded to the following questions:
    - What is your goal?
    - What motivates you most?
    - How do you plan your week?
    - What keeps you consistent?
    
    User's Goal: "${goal}"
    Quiz Answers (in order): ${JSON.stringify(answers)}

    Based on this information, return a JSON object with the following structure:
    {
      "personaType": "Architect" | "Achiever",
      "motivationalMessage": "A personalized motivational message for the user based on their persona."
    }
  `;

  console.log('Sending prompt to Gemini API...');

  let retries = 3;
  while (retries > 0) {
    try {
      const result = await model.generateContent(prompt);
      const response = await result.response;
      const text = await response.text();
      
      console.log('Received response from Gemini API:');
      console.log(text);

      // The output from gemini-pro is a markdown string with ```json ... ```, so we need to parse it.
      const jsonString = text.replace(/```json\n|\n```/g, '');
      const parsedResult = JSON.parse(jsonString);
      return parsedResult;
    } catch (error: any) {
      console.error('Error calling Gemini API:', error);
      // Check for rate limit or service unavailable errors and retry if there are retries left
      if ((error.status === 429 || error.status === 503) && retries > 0) {
        console.log(`Service unavailable or rate limit exceeded, retrying in 2 seconds...`);
        await sleep(2000); // Wait for 2 seconds before retrying
        retries--;
      } else {
        // For other errors, or if retries are exhausted, throw the error
        throw error;
      }
    }
  }
  throw new Error("Failed to classify user persona after multiple retries.");
}

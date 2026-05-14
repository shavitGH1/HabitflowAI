import { GeminiClient } from '../gemini.client';
import { buildPersonaClassifierPrompt } from '../prompts/persona-classifier.prompt';

const VALID_PERSONAS = ['Achiever', 'Grower', 'Socializer', 'Explorer', 'Altruist', 'Architect'];

export interface PersonaResult {
  isValid: boolean;
  errorReason?: string;
  personaType?: string;
}

export const classifyPersona = async (
  client: GeminiClient,
  goal: string,
  answers: string[],
): Promise<PersonaResult> => {
  const result = await client.generateJson<PersonaResult>(
    buildPersonaClassifierPrompt(goal, answers),
  );
  if (result.isValid && result.personaType && !VALID_PERSONAS.includes(result.personaType)) {
    return { isValid: false, errorReason: `Unknown persona type: ${result.personaType}` };
  }
  return result;
};

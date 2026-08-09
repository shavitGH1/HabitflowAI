// Owner: Member 3 (AI). Prompt, schema and logic are free to change — callers only depend on AiService.coachChat().
import { Injectable } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { logger } from '../../logger';
import { buildCoachingAgentPrompt, CoachingAgentPromptInput } from '../prompts/coaching-agent.prompt';
import { ResolvedCoachingAgentOutput, coachingAgentOutputSchema } from '../schemas/coaching-agent.schema';

export type CoachingAgentInput = CoachingAgentPromptInput;

const FALLBACK_OUTPUT: ResolvedCoachingAgentOutput = {
  reply: "I'm having trouble thinking that through right now — mind trying again in a moment?",
  proposedChange: null,
};

@Injectable()
export class CoachingAgentFeature {
  constructor(private readonly gemini: GeminiClient) {}

  async converse(input: CoachingAgentInput): Promise<ResolvedCoachingAgentOutput> {
    try {
      const prompt = buildCoachingAgentPrompt(input);
      const output = await this.gemini.generateJson(prompt, coachingAgentOutputSchema);
      return { reply: output.reply, proposedChange: output.proposedChange ?? null };
    } catch (error) {
      logger.warn({ err: error }, 'coaching agent call failed, falling back to a plain reply');
      return FALLBACK_OUTPUT;
    }
  }
}

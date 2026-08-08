import { Injectable } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { logger } from '../../logger';
import { buildTaskVerificationPrompt } from '../prompts/task-verification.prompt';
import { TaskVerificationOutput, taskVerificationOutputSchema } from '../schemas/task-verification.schema';

export interface TaskVerificationInput {
  habitTitle: string;
  note: string;
}

const FALLBACK_OUTPUT: TaskVerificationOutput = { isPlausible: true, reason: '' };

@Injectable()
export class TaskVerificationFeature {
  constructor(private readonly gemini: GeminiClient) {}

  async check(input: TaskVerificationInput): Promise<TaskVerificationOutput> {
    try {
      const prompt = buildTaskVerificationPrompt(input);
      return await this.gemini.generateJson(prompt, taskVerificationOutputSchema);
    } catch (error) {
      logger.warn({ err: error }, 'task verification check failed, defaulting to plausible');
      return FALLBACK_OUTPUT;
    }
  }
}

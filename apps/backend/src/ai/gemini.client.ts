import { Injectable, InternalServerErrorException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { GoogleGenAI } from '@google/genai';
import { ZodSchema } from 'zod';
import { logger } from '../logger';

const EMBEDDING_MODEL = 'gemini-embedding-001';

@Injectable()
export class GeminiClient {
  private readonly ai: GoogleGenAI;
  private readonly models: string[];

  constructor(private readonly config: ConfigService) {
    this.ai = new GoogleGenAI({ apiKey: this.config.get<string>('GEMINI_API_KEY') });
    const configured = this.config.get<string>('GEMINI_MODEL');
    this.models = configured
      ? [configured]
      : ['gemini-1.5-flash', 'gemini-1.5-flash-latest', 'gemini-1.5-pro'];
  }

  async embedContent(text: string): Promise<number[]> {
    try {
      const response = await this.ai.models.embedContent({ model: EMBEDDING_MODEL, contents: text });
      const values = response.embeddings?.[0]?.values;
      if (!values) throw new Error('Empty embedding response');
      return values;
    } catch (error) {
      const msg = error instanceof Error ? error.message : String(error);
      logger.error({ err: msg }, 'Gemini embedding call failed');
      throw new InternalServerErrorException(
        'AI Service is currently overloaded. Please try again in a few seconds.',
      );
    }
  }

  async generateJson<T>(prompt: string, schema?: ZodSchema<T>): Promise<T> {
    const raw = await this.callWithFallback(prompt);
    const parsed = this.parseJson(raw);

    if (schema) {
      const result = schema.safeParse(parsed);
      if (!result.success) {
        logger.error({ err: result.error.message }, 'Gemini output failed schema validation');
        throw new InternalServerErrorException('AI returned invalid output. Please try again.');
      }
      return result.data;
    }

    return parsed as T;
  }

  private async callWithFallback(prompt: string): Promise<string> {
    for (let i = 0; i < this.models.length; i++) {
      const model = this.models[i];
      try {
        const response = await this.ai.models.generateContent({
          model,
          contents: prompt,
          config: { responseMimeType: 'application/json' },
        });
        const text = response.text;
        if (!text) throw new Error('Empty response');
        return text;
      } catch (error) {
        const msg = error instanceof Error ? error.message : String(error);
        if (i === this.models.length - 1) {
          logger.error({ model, err: msg }, 'all Gemini models exhausted');
          throw new InternalServerErrorException(
            'AI Service is currently overloaded. Please try again in a few seconds.',
          );
        }
        logger.warn({ model, err: msg }, 'Gemini model failed, trying next');
      }
    }
    throw new InternalServerErrorException('AI Service unavailable.');
  }

  private parseJson(raw: string): unknown {
    try {
      return JSON.parse(raw);
    } catch {
      throw new InternalServerErrorException('AI returned malformed JSON.');
    }
  }
}

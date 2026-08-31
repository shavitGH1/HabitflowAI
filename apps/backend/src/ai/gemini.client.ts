import { Injectable, InternalServerErrorException, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Content, FunctionCallingConfigMode, FunctionDeclaration, GoogleGenAI } from '@google/genai';
import { ZodSchema } from 'zod';

export interface ToolCallRequest {
  name: string;
  args: Record<string, unknown>;
  thoughtSignature?: string;
}

export interface ToolTurnInput {
  systemInstruction: string;
  contents: Content[];
  functionDeclarations: FunctionDeclaration[];
  forceToolCall?: boolean;
}

export interface ToolTurn {
  text: string;
  toolCalls: ToolCallRequest[];
}

const EMBEDDING_MODEL = 'gemini-embedding-001';
// Without this, a single slow/hanging model attempt blocks the whole fallback chain
// indefinitely - observed live at 70+ seconds for one call with no timeout set.
const REQUEST_TIMEOUT_MS = 15_000;

@Injectable()
export class GeminiClient {
  private readonly ai: GoogleGenAI;
  private readonly models: string[];
  private readonly logger = new Logger(GeminiClient.name);

  constructor(private readonly config: ConfigService) {
    const rawKey = this.config.get<string>('GEMINI_API_KEY') || '';
    const apiKey = rawKey.trim();

    this.ai = new GoogleGenAI({ apiKey });

    const configured = this.config.get<string>('GEMINI_MODEL');
    const defaultModels = [
      'gemini-3.6-flash',
      'gemini-2.5-flash',
      'gemini-flash-lite-latest',
      'gemini-3.5-flash-lite'
    ];

    this.models = configured && !defaultModels.includes(configured)
      ? [configured, ...defaultModels]
      : defaultModels;

    this.logger.log(`GeminiClient initialized with models: ${this.models.join(', ')}`);
  }

  async embedContent(text: string): Promise<number[]> {
    try {
      const response = await this.ai.models.embedContent({
        model: EMBEDDING_MODEL,
        contents: text,
        config: { httpOptions: { timeout: REQUEST_TIMEOUT_MS } },
      });
      const values = response.embeddings?.[0]?.values;
      if (!values || values.length === 0) throw new Error('Empty embedding response');
      return values;
    } catch (error) {
      const msg = error instanceof Error ? error.message : String(error);
      this.logger.error(`Embedding failed: ${msg}`);
      throw new InternalServerErrorException('AI Service overloaded.');
    }
  }

  async generateJson<T>(prompt: string, schema?: ZodSchema<T>): Promise<T> {
    const raw = await this.callWithFallback(prompt);
    const parsed = this.parseJson(raw);

    if (schema) {
      const result = schema.safeParse(parsed);
      if (!result.success) {
        this.logger.error(`Schema validation failed: ${result.error.message}`);
        throw new InternalServerErrorException('AI returned invalid output.');
      }
      return result.data;
    }

    return parsed as T;
  }

  async generateWithTools({
    systemInstruction,
    contents,
    functionDeclarations,
    forceToolCall,
  }: ToolTurnInput): Promise<ToolTurn> {
    return this.withModelFallback(async (model) => {
      const response = await this.ai.models.generateContent({
        model,
        contents,
        config: {
          systemInstruction,
          tools: [{ functionDeclarations }],
          httpOptions: { timeout: REQUEST_TIMEOUT_MS },
          ...(forceToolCall && {
            toolConfig: { functionCallingConfig: { mode: FunctionCallingConfigMode.ANY } },
          }),
        },
      });

      const parts = response.candidates?.[0]?.content?.parts ?? [];
      const toolCalls = parts
        .filter((part): part is typeof part & { functionCall: { name: string; args?: Record<string, unknown> } } =>
          Boolean(part.functionCall?.name),
        )
        .map((part) => ({
          name: part.functionCall.name,
          args: part.functionCall.args ?? {},
          thoughtSignature: part.thoughtSignature,
        }));
      const text = response.text ?? '';

      if (!text && !toolCalls.length) throw new Error('Empty response');
      return { text, toolCalls };
    });
  }

  private callWithFallback(prompt: string): Promise<string> {
    return this.withModelFallback(async (model) => {
      const response = await this.ai.models.generateContent({
        model,
        contents: prompt,
        config: { responseMimeType: 'application/json', httpOptions: { timeout: REQUEST_TIMEOUT_MS } },
      });
      const text = response.text;
      if (!text) throw new Error('Empty response');
      return text;
    });
  }

  private async withModelFallback<T>(run: (model: string) => Promise<T>): Promise<T> {
    for (let i = 0; i < this.models.length; i++) {
      const model = this.models[i];
      try {
        return await run(model);
      } catch (error) {
        const msg = error instanceof Error ? error.message : String(error);
        this.logger.warn(`Model ${model} failed: ${msg}`);
        if (i === this.models.length - 1) {
          throw new InternalServerErrorException(
            'AI Service is currently overloaded. Please try again in a few seconds.',
          );
        }
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

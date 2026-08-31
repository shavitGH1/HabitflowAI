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
const REQUEST_TIMEOUT_MS = 30_000;
// How long the primary model runs alone before a backup model also joins the race.
const HEDGE_DELAY_MS = 8_000;

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
    const attempt = (model: string): Promise<T> =>
      run(model).catch((error) => {
        const msg = error instanceof Error ? error.message : String(error);
        this.logger.warn(`Model ${model} failed: ${msg}`);
        throw error;
      });

    const [primary, backup, ...rest] = this.models;

    if (primary) {
      try {
        return backup ? await this.hedge(() => attempt(primary), () => attempt(backup)) : await attempt(primary);
      } catch {
        // both hedge candidates failed (or there was only one model) - fall through to the tail
      }
    }

    for (const model of rest) {
      try {
        return await attempt(model);
      } catch {
        // already logged inside attempt(); try the next one
      }
    }

    throw new InternalServerErrorException(
      'AI Service is currently overloaded. Please try again in a few seconds.',
    );
  }

  // Races `primary` against `backup` - `backup` only actually starts once, either after
  // HEDGE_DELAY_MS with `primary` still pending, or immediately if `primary` rejects first,
  // whichever happens sooner. First success wins.
  private hedge<T>(primary: () => Promise<T>, backup: () => Promise<T>): Promise<T> {
    const primaryPromise = primary();

    let backupPromise: Promise<T> | null = null;
    const startBackup = (): Promise<T> => {
      if (!backupPromise) backupPromise = backup();
      return backupPromise;
    };

    const backupTrigger = new Promise<T>((resolve, reject) => {
      const timer = setTimeout(() => startBackup().then(resolve, reject), HEDGE_DELAY_MS);
      // A successful primary just needs the timer cancelled - Promise.any already has its
      // winner. A failed primary needs the backup started right away instead of waiting
      // out the rest of the delay.
      primaryPromise.then(
        () => clearTimeout(timer),
        () => {
          clearTimeout(timer);
          startBackup().then(resolve, reject);
        },
      );
    });

    return Promise.any([primaryPromise, backupTrigger]).catch((aggregate: AggregateError) => {
      throw aggregate.errors[aggregate.errors.length - 1];
    });
  }

  private parseJson(raw: string): unknown {
    try {
      return JSON.parse(raw);
    } catch {
      throw new InternalServerErrorException('AI returned malformed JSON.');
    }
  }
}

import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { GoogleGenAI } from '@google/genai';

@Injectable()
export class GeminiClient {
  constructor(private readonly config: ConfigService) {}

  private getModels(): string[] {
    const configured = this.config.get<string>('GEMINI_MODEL');
    return configured
      ? [configured]
      : ['gemini-2.5-flash-lite', 'gemini-flash-lite-latest', 'gemini-2.5-flash'];
  }

  async generateJson<T>(prompt: string): Promise<T> {
    const ai = new GoogleGenAI({ apiKey: this.config.get<string>('GEMINI_API_KEY') });
    const models = this.getModels();

    for (let i = 0; i < models.length; i++) {
      const model = models[i];
      try {
        const response = await ai.models.generateContent({
          model,
          contents: prompt,
          config: { responseMimeType: 'application/json' },
        });
        const text = response.text;
        if (!text) throw new Error('Empty response from model.');
        return JSON.parse(text) as T;
      } catch (error: any) {
        if (i === models.length - 1) {
          throw new Error('AI service is currently unavailable. Please try again.');
        }
      }
    }
    throw new Error('AI service failed.');
  }
}

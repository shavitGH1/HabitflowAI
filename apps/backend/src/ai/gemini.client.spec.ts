const mockGenerateContent = jest.fn();

jest.mock('@google/genai', () => ({
  GoogleGenAI: jest.fn(() => ({ models: { generateContent: mockGenerateContent } })),
}));

import { InternalServerErrorException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { GeminiClient } from './gemini.client';

const makeConfig = (overrides: Record<string, string> = {}): ConfigService =>
  ({
    get: (key: string) =>
      ({ GEMINI_API_KEY: 'test-key', ...overrides } as Record<string, string>)[key],
  } as unknown as ConfigService);

describe('GeminiClient fallback chain', () => {
  beforeEach(() => {
    mockGenerateContent.mockReset();
  });

  it('falls back to the next model when the first one is dead', async () => {
    mockGenerateContent
      .mockRejectedValueOnce(new Error('404 model not found'))
      .mockResolvedValueOnce({ text: JSON.stringify({ ok: true }) });

    const client = new GeminiClient(makeConfig());

    await expect(client.generateJson<{ ok: boolean }>('prompt')).resolves.toEqual({ ok: true });
    expect(mockGenerateContent).toHaveBeenCalledTimes(2);
    expect(mockGenerateContent.mock.calls[0][0].model).toBe('gemini-1.5-flash');
    expect(mockGenerateContent.mock.calls[1][0].model).toBe('gemini-1.5-flash-latest');
  });

  it('walks the whole chain and throws a friendly error when every model fails', async () => {
    mockGenerateContent.mockRejectedValue(new Error('overloaded'));

    const client = new GeminiClient(makeConfig());

    await expect(client.generateJson('prompt')).rejects.toBeInstanceOf(
      InternalServerErrorException,
    );
    expect(mockGenerateContent).toHaveBeenCalledTimes(3);
  });

  it('uses only the configured model and fails clearly when that model name is dead', async () => {
    mockGenerateContent.mockRejectedValue(new Error('404 model not found: dead-model'));

    const client = new GeminiClient(makeConfig({ GEMINI_MODEL: 'dead-model' }));

    await expect(client.generateJson('prompt')).rejects.toBeInstanceOf(
      InternalServerErrorException,
    );
    expect(mockGenerateContent).toHaveBeenCalledTimes(1);
    expect(mockGenerateContent.mock.calls[0][0].model).toBe('dead-model');
  });

  it('treats an empty response as a failure and falls back', async () => {
    mockGenerateContent
      .mockResolvedValueOnce({ text: '' })
      .mockResolvedValueOnce({ text: JSON.stringify({ recovered: true }) });

    const client = new GeminiClient(makeConfig());

    await expect(client.generateJson<{ recovered: boolean }>('prompt')).resolves.toEqual({
      recovered: true,
    });
    expect(mockGenerateContent).toHaveBeenCalledTimes(2);
  });
});

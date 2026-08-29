const mockGenerateContent = jest.fn();
const mockEmbedContent = jest.fn();

jest.mock('@google/genai', () => ({
  GoogleGenAI: jest.fn(() => ({
    models: { generateContent: mockGenerateContent, embedContent: mockEmbedContent },
  })),
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
    expect(mockGenerateContent.mock.calls[0][0].model).toBe('gemini-3.6-flash');
    expect(mockGenerateContent.mock.calls[1][0].model).toBe('gemini-2.5-flash');
  });

  it('walks the whole chain and throws a friendly error when every model fails', async () => {
    mockGenerateContent.mockRejectedValue(new Error('overloaded'));

    const client = new GeminiClient(makeConfig());

    await expect(client.generateJson('prompt')).rejects.toBeInstanceOf(
      InternalServerErrorException,
    );
    expect(mockGenerateContent).toHaveBeenCalledTimes(4);
  });

  it('tries the configured model first, then still falls back through the default chain if it is dead too', async () => {
    mockGenerateContent.mockRejectedValue(new Error('404 model not found: dead-model'));

    const client = new GeminiClient(makeConfig({ GEMINI_MODEL: 'dead-model' }));

    await expect(client.generateJson('prompt')).rejects.toBeInstanceOf(
      InternalServerErrorException,
    );
    // configured model + the 4 defaults it still falls through
    expect(mockGenerateContent).toHaveBeenCalledTimes(5);
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

describe('GeminiClient.embedContent', () => {
  beforeEach(() => {
    mockEmbedContent.mockReset();
  });

  it('returns the embedding values on success', async () => {
    mockEmbedContent.mockResolvedValue({ embeddings: [{ values: [0.1, 0.2, 0.3] }] });

    const client = new GeminiClient(makeConfig());

    await expect(client.embedContent('some text')).resolves.toEqual([0.1, 0.2, 0.3]);
    expect(mockEmbedContent.mock.calls[0][0].model).toBe('gemini-embedding-001');
  });

  it('throws a friendly error when the response has no embedding', async () => {
    mockEmbedContent.mockResolvedValue({ embeddings: [] });

    const client = new GeminiClient(makeConfig());

    await expect(client.embedContent('some text')).rejects.toBeInstanceOf(
      InternalServerErrorException,
    );
  });

  it('throws a friendly error when the API call fails', async () => {
    mockEmbedContent.mockRejectedValue(new Error('quota exceeded'));

    const client = new GeminiClient(makeConfig());

    await expect(client.embedContent('some text')).rejects.toBeInstanceOf(
      InternalServerErrorException,
    );
  });
});

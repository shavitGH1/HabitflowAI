import { GeminiClient } from '../gemini.client';
import { ResearchSearchFeature } from './research-search.feature';

describe('ResearchSearchFeature', () => {
  let gemini: { embedContent: jest.Mock };
  let feature: ResearchSearchFeature;

  const chunks = [
    {
      id: 'a',
      sourceTitle: 'Doc',
      section: 'The Fogg Behavior Model',
      content: 'B=MAP...',
      embedding: [1, 0, 0],
    },
    {
      id: 'b',
      sourceTitle: 'Doc',
      section: 'Unrelated Section',
      content: 'Something else...',
      embedding: [0, 1, 0],
    },
    {
      id: 'c',
      sourceTitle: 'Doc',
      section: 'Close Match',
      content: 'Also relevant...',
      embedding: [0.9, 0.1, 0],
    },
  ];

  beforeEach(() => {
    gemini = { embedContent: jest.fn() };
    feature = new ResearchSearchFeature(gemini as unknown as GeminiClient);
  });

  it('ranks chunks by cosine similarity to the query, best match first', async () => {
    gemini.embedContent.mockResolvedValue([1, 0, 0]);

    const result = await feature.search({ query: 'staying motivated', chunks, topK: 3 });

    expect(result.map(r => r.id)).toEqual(['a', 'c', 'b']);
    expect(result[0].score).toBeCloseTo(1, 5);
  });

  it('returns only the top-K results', async () => {
    gemini.embedContent.mockResolvedValue([1, 0, 0]);

    const result = await feature.search({ query: 'staying motivated', chunks, topK: 2 });

    expect(result).toHaveLength(2);
    expect(result.map(r => r.id)).toEqual(['a', 'c']);
  });

  it('defaults to top 3 when topK is not given', async () => {
    gemini.embedContent.mockResolvedValue([1, 0, 0]);

    const result = await feature.search({ query: 'staying motivated', chunks });

    expect(result).toHaveLength(3);
  });

  it('returns an empty array without calling Gemini when there are no chunks', async () => {
    const result = await feature.search({ query: 'anything', chunks: [] });

    expect(result).toEqual([]);
    expect(gemini.embedContent).not.toHaveBeenCalled();
  });

  it('propagates the error when the embedding call fails', async () => {
    gemini.embedContent.mockRejectedValue(new Error('Gemini overloaded'));

    await expect(feature.search({ query: 'staying motivated', chunks })).rejects.toThrow(
      'Gemini overloaded',
    );
  });
});

import { GeminiClient } from '../gemini.client';
import { RagSearchFeature } from './rag-search.feature';

describe('RagSearchFeature', () => {
  let gemini: { embedContent: jest.Mock };
  let feature: RagSearchFeature;

  const articles = [
    { id: 'a', title: 'Habit Stacking', url: 'https://example.com/a', embedding: [1, 0, 0] },
    { id: 'b', title: 'Unrelated Topic', url: 'https://example.com/b', embedding: [0, 1, 0] },
    { id: 'c', title: 'Close Match', url: 'https://example.com/c', embedding: [0.9, 0.1, 0] },
  ];

  beforeEach(() => {
    gemini = { embedContent: jest.fn() };
    feature = new RagSearchFeature(gemini as unknown as GeminiClient);
  });

  it('ranks articles by cosine similarity to the query, best match first', async () => {
    gemini.embedContent.mockResolvedValue([1, 0, 0]);

    const result = await feature.search({ query: 'building habits', articles, topK: 3 });

    expect(result.map(r => r.id)).toEqual(['a', 'c', 'b']);
    expect(result[0].score).toBeCloseTo(1, 5);
  });

  it('returns only the top-K results', async () => {
    gemini.embedContent.mockResolvedValue([1, 0, 0]);

    const result = await feature.search({ query: 'building habits', articles, topK: 2 });

    expect(result).toHaveLength(2);
    expect(result.map(r => r.id)).toEqual(['a', 'c']);
  });

  it('defaults to top 3 when topK is not given', async () => {
    gemini.embedContent.mockResolvedValue([1, 0, 0]);

    const result = await feature.search({ query: 'building habits', articles });

    expect(result).toHaveLength(3);
  });

  it('returns an empty array without calling Gemini when there are no articles', async () => {
    const result = await feature.search({ query: 'anything', articles: [] });

    expect(result).toEqual([]);
    expect(gemini.embedContent).not.toHaveBeenCalled();
  });

  it('fails open to an empty array when the embedding call fails', async () => {
    gemini.embedContent.mockRejectedValue(new Error('Gemini overloaded'));

    const result = await feature.search({ query: 'building habits', articles });

    expect(result).toEqual([]);
  });
});

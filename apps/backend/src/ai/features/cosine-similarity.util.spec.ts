import { cosineSimilarity } from './cosine-similarity.util';

describe('cosineSimilarity()', () => {
  it('returns 1 for identical vectors', () => {
    expect(cosineSimilarity([1, 2, 3], [1, 2, 3])).toBeCloseTo(1, 5);
  });

  it('returns 0 for orthogonal vectors', () => {
    expect(cosineSimilarity([1, 0], [0, 1])).toBeCloseTo(0, 5);
  });

  it('returns -1 for opposite vectors', () => {
    expect(cosineSimilarity([1, 2, 3], [-1, -2, -3])).toBeCloseTo(-1, 5);
  });

  it('matches the hand-computed value for a non-trivial pair', () => {
    // dot = 32, |a| = sqrt(14), |b| = sqrt(77) -> 32 / sqrt(1078)
    expect(cosineSimilarity([1, 2, 3], [4, 5, 6])).toBeCloseTo(0.9746, 4);
  });

  it('returns 0 instead of NaN when either vector is all zeros', () => {
    expect(cosineSimilarity([0, 0, 0], [1, 2, 3])).toBe(0);
    expect(cosineSimilarity([1, 2, 3], [0, 0, 0])).toBe(0);
  });

  it('returns 0 instead of a corrupted score when vectors have mismatched dimensions', () => {
    expect(cosineSimilarity([1, 2, 3], [1, 2, 3, 4])).toBe(0);
    expect(cosineSimilarity([1, 2, 3, 4], [1, 2, 3])).toBe(0);
  });
});

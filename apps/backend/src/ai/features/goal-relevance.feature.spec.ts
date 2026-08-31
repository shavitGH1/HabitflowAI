import { GeminiClient } from '../gemini.client';
import { GoalRelevanceFeature } from './goal-relevance.feature';

describe('GoalRelevanceFeature', () => {
  let gemini: { generateJson: jest.Mock };
  let feature: GoalRelevanceFeature;

  const input = {
    oldGoalTitle: 'Run 10km under 40 minutes',
    newGoalTitle: 'Run 20km',
  };

  beforeEach(() => {
    gemini = { generateJson: jest.fn() };
    feature = new GoalRelevanceFeature(gemini as unknown as GeminiClient);
  });

  it('returns the validated verdict on a successful call', async () => {
    gemini.generateJson.mockResolvedValue({ isRelated: true, reason: 'Same pursuit, longer distance.' });

    const result = await feature.check(input);

    expect(result).toEqual({ succeeded: true, isRelated: true, reason: 'Same pursuit, longer distance.' });
  });

  it('can flag a new goal as unrelated', async () => {
    gemini.generateJson.mockResolvedValue({ isRelated: false, reason: 'No connection to the prior goal.' });

    const result = await feature.check({ oldGoalTitle: 'Run a marathon', newGoalTitle: 'Learn to paint' });

    expect(result.succeeded).toBe(true);
    expect(result.isRelated).toBe(false);
  });

  it('reports succeeded: false when the AI call fails — never throws, but the caller can tell', async () => {
    gemini.generateJson.mockRejectedValue(new Error('Gemini overloaded'));

    const result = await feature.check(input);

    expect(result).toEqual({ succeeded: false, isRelated: true, reason: '' });
  });
});

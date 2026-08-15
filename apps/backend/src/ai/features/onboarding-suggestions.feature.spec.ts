import { GeminiClient } from '../gemini.client';
import { ONBOARDING_QUESTIONS } from '../pillars';
import { OnboardingSuggestionsFeature } from './onboarding-suggestions.feature';

describe('OnboardingSuggestionsFeature', () => {
  let gemini: { generateJson: jest.Mock };
  let feature: OnboardingSuggestionsFeature;

  const validOutput = {
    suggestions: ONBOARDING_QUESTIONS.map((q) => ({
      questionId: q.id,
      options: ['Option A', 'Option B', 'Option C'],
    })),
  };

  beforeEach(() => {
    gemini = { generateJson: jest.fn() };
    feature = new OnboardingSuggestionsFeature(gemini as unknown as GeminiClient);
  });

  it('returns the validated suggestions on a successful call', async () => {
    gemini.generateJson.mockResolvedValue(validOutput);

    const result = await feature.generate({ goal: 'Run a marathon' });

    expect(result).toEqual(validOutput);
    expect(result.suggestions).toHaveLength(ONBOARDING_QUESTIONS.length);
    expect(gemini.generateJson).toHaveBeenCalledTimes(1);
  });

  it('propagates errors from the Gemini client (caller decides how to degrade)', async () => {
    gemini.generateJson.mockRejectedValue(new Error('AI Service unavailable.'));

    await expect(feature.generate({ goal: 'Run a marathon' })).rejects.toThrow('AI Service unavailable.');
  });
});

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

  it('only requests suggestions for the remaining, not-yet-answered questions', async () => {
    const remaining = ONBOARDING_QUESTIONS.slice(3);
    gemini.generateJson.mockResolvedValue({
      suggestions: remaining.map((q) => ({ questionId: q.id, options: ['A', 'B', 'C'] })),
    });

    const result = await feature.generate({
      goal: 'Run a marathon',
      answeredSoFar: ['answer 1', 'answer 2', 'answer 3'],
    });

    expect(result.suggestions).toHaveLength(remaining.length);
    expect(gemini.generateJson).toHaveBeenCalledTimes(1);
  });

  it('skips the AI call entirely when every question already has an answer', async () => {
    const result = await feature.generate({
      goal: 'Run a marathon',
      answeredSoFar: ONBOARDING_QUESTIONS.map(() => 'already answered'),
    });

    expect(result).toEqual({ suggestions: [] });
    expect(gemini.generateJson).not.toHaveBeenCalled();
  });
});

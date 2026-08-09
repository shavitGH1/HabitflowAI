import { GeminiClient } from '../gemini.client';
import { HabitGoalRelevanceFeature } from './habit-goal-relevance.feature';

describe('HabitGoalRelevanceFeature', () => {
  let gemini: { generateJson: jest.Mock };
  let feature: HabitGoalRelevanceFeature;

  const input = {
    goalTitle: 'Run a marathon',
    habitTitle: 'Stretch 10 minutes',
  };

  beforeEach(() => {
    gemini = { generateJson: jest.fn() };
    feature = new HabitGoalRelevanceFeature(gemini as unknown as GeminiClient);
  });

  it('returns the validated verdict on a successful call', async () => {
    gemini.generateJson.mockResolvedValue({ isRelated: true, reason: 'Supports recovery and mobility.' });

    const result = await feature.check(input);

    expect(result).toEqual({ isRelated: true, reason: 'Supports recovery and mobility.' });
  });

  it('can flag a habit as unrelated', async () => {
    gemini.generateJson.mockResolvedValue({ isRelated: false, reason: 'No connection to the goal.' });

    const result = await feature.check({ goalTitle: 'Run a marathon', habitTitle: 'Watch Netflix' });

    expect(result.isRelated).toBe(false);
  });

  it('falls back to isRelated: true when the AI call fails — never blocks the caller', async () => {
    gemini.generateJson.mockRejectedValue(new Error('Gemini overloaded'));

    const result = await feature.check(input);

    expect(result).toEqual({ isRelated: true, reason: '' });
  });

  it('falls back to isRelated: true on a schema-invalid response', async () => {
    gemini.generateJson.mockRejectedValue(new Error('AI returned invalid output. Please try again.'));

    const result = await feature.check(input);

    expect(result.isRelated).toBe(true);
  });
});

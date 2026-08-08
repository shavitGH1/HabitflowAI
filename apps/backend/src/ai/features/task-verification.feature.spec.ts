import { GeminiClient } from '../gemini.client';
import { TaskVerificationFeature } from './task-verification.feature';

describe('TaskVerificationFeature', () => {
  let gemini: { generateJson: jest.Mock };
  let feature: TaskVerificationFeature;

  const input = {
    habitTitle: 'Morning Run',
    note: 'Ran 5km along the river this morning',
  };

  beforeEach(() => {
    gemini = { generateJson: jest.fn() };
    feature = new TaskVerificationFeature(gemini as unknown as GeminiClient);
  });

  it('returns the validated verdict on a successful call', async () => {
    gemini.generateJson.mockResolvedValue({ isPlausible: true, reason: 'Matches the habit.' });

    const result = await feature.check(input);

    expect(result).toEqual({ isPlausible: true, reason: 'Matches the habit.' });
  });

  it('can flag a note as implausible', async () => {
    gemini.generateJson.mockResolvedValue({ isPlausible: false, reason: 'Watching TV is not a run.' });

    const result = await feature.check({ habitTitle: 'Morning Run', note: 'Watched TV all day' });

    expect(result.isPlausible).toBe(false);
  });

  it('falls back to isPlausible: true when the AI call fails — never blocks completion', async () => {
    gemini.generateJson.mockRejectedValue(new Error('Gemini overloaded'));

    const result = await feature.check(input);

    expect(result).toEqual({ isPlausible: true, reason: '' });
  });

  it('falls back to isPlausible: true on a schema-invalid response', async () => {
    gemini.generateJson.mockRejectedValue(new Error('AI returned invalid output. Please try again.'));

    const result = await feature.check(input);

    expect(result.isPlausible).toBe(true);
  });
});

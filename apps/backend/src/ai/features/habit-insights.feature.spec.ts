import { GeminiClient } from '../gemini.client';
import { HabitInsightsFeature } from './habit-insights.feature';

describe('HabitInsightsFeature', () => {
  const output = {
    summary: 'You had a strong week.',
    wins: ['Completed your morning run 5 times'],
    improvements: ['Try not to skip weekends'],
  };
  let gemini: { generateJson: jest.Mock };
  let feature: HabitInsightsFeature;

  const input = {
    userId: 'u1',
    personaType: 'Achiever' as const,
    weekCompletionRate: 0.7,
    currentStreak: 4,
    completedHabits: ['Morning run'],
    missedHabits: ['Meditate'],
  };

  beforeEach(() => {
    gemini = { generateJson: jest.fn().mockResolvedValue(output) };
    feature = new HabitInsightsFeature(gemini as unknown as GeminiClient);
  });

  it('returns a validated weekly summary', async () => {
    const result = await feature.generate(input);
    expect(result).toEqual(output);
    expect(gemini.generateJson).toHaveBeenCalledTimes(1);
  });

  it('caches per user per week', async () => {
    await feature.generate(input);
    await feature.generate(input);
    expect(gemini.generateJson).toHaveBeenCalledTimes(1);
  });
});

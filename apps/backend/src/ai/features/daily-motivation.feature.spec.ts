import { GeminiClient } from '../gemini.client';
import { DailyMotivationFeature } from './daily-motivation.feature';

describe('DailyMotivationFeature', () => {
  const output = { shortMessage: 'Keep your streak alive', coachingNote: 'You are doing great.' };
  let gemini: { generateJson: jest.Mock };
  let feature: DailyMotivationFeature;

  beforeEach(() => {
    gemini = { generateJson: jest.fn().mockResolvedValue(output) };
    feature = new DailyMotivationFeature(gemini as unknown as GeminiClient);
  });

  it('returns a validated motivation message', async () => {
    const result = await feature.generate({
      userId: 'u1',
      personaType: 'Achiever',
      currentStreak: 5,
      todayCompletionRate: 0.8,
    });
    expect(result).toEqual(output);
    expect(gemini.generateJson).toHaveBeenCalledTimes(1);
  });

  it('caches per user/date/persona to avoid redundant Gemini calls', async () => {
    const input = {
      userId: 'u1',
      personaType: 'Achiever' as const,
      currentStreak: 5,
      todayCompletionRate: 0.8,
    };
    await feature.generate(input);
    await feature.generate(input);
    expect(gemini.generateJson).toHaveBeenCalledTimes(1);
  });

  it('does not share cache across different personas', async () => {
    await feature.generate({ userId: 'u1', personaType: 'Achiever', currentStreak: 1, todayCompletionRate: 0.5 });
    await feature.generate({ userId: 'u1', personaType: 'Grower', currentStreak: 1, todayCompletionRate: 0.5 });
    expect(gemini.generateJson).toHaveBeenCalledTimes(2);
  });
});

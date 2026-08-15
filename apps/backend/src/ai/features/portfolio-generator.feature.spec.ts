import { GeminiClient } from '../gemini.client';
import { HabitGoalRelevanceFeature } from './habit-goal-relevance.feature';
import { PortfolioGeneratorFeature } from './portfolio-generator.feature';

describe('PortfolioGeneratorFeature', () => {
  let gemini: { generateJson: jest.Mock };
  let habitGoalRelevance: { check: jest.Mock };
  let feature: PortfolioGeneratorFeature;

  const input = {
    goal: 'Run a marathon',
    openAnswers: ['I stuck with drinking water daily for months', 'a', 'b', 'c', 'd', 'e'],
    personaType: 'Achiever' as const,
    weightedBreakdown: { Achievement: 80, Growth: 10, Connection: 0, Exploration: 0, Purpose: 5, Structure: 5 },
  };

  const onTopicOutput = {
    summary: 'You are driven by measurable progress.',
    tips: ['tip1', 'tip2', 'tip3'],
    failurePatterns: ['pattern1'],
    coreGoals: [{ description: 'Run 3 times a week', points: 20, genre: 'goal' as const }],
    dailyVariations: [
      { description: 'Run 2 miles today', points: 20, genre: 'goal' as const },
      { description: 'Stretch after your run', points: 10, genre: 'goal' as const },
      { description: 'Journal for 5 minutes', points: 5, genre: 'persona' as const },
      { description: 'Plan tomorrow', points: 5, genre: 'persona' as const },
    ],
  };

  const offTopicOutput = {
    ...onTopicOutput,
    coreGoals: [{ description: 'Drink 8 glasses of water', points: 20, genre: 'goal' as const }],
  };

  beforeEach(() => {
    gemini = { generateJson: jest.fn() };
    habitGoalRelevance = { check: jest.fn() };
    feature = new PortfolioGeneratorFeature(
      gemini as unknown as GeminiClient,
      habitGoalRelevance as unknown as HabitGoalRelevanceFeature,
    );
  });

  it('returns the output on the first attempt when every goal-tagged task is related', async () => {
    gemini.generateJson.mockResolvedValue(onTopicOutput);
    habitGoalRelevance.check.mockResolvedValue({ isRelated: true, reason: '' });

    const result = await feature.generate(input);

    expect(result).toEqual(onTopicOutput);
    expect(gemini.generateJson).toHaveBeenCalledTimes(1);
  });

  it('regenerates when a goal-tagged task is unrelated to the stated goal, and returns the good attempt', async () => {
    gemini.generateJson.mockResolvedValueOnce(offTopicOutput).mockResolvedValueOnce(onTopicOutput);
    habitGoalRelevance.check.mockImplementation(({ habitTitle }: { habitTitle: string }) =>
      Promise.resolve({ isRelated: !habitTitle.toLowerCase().includes('water'), reason: '' }),
    );

    const result = await feature.generate(input);

    expect(result).toEqual(onTopicOutput);
    expect(gemini.generateJson).toHaveBeenCalledTimes(2);
  });

  it('gives up after the max attempts and returns the last generation without throwing', async () => {
    gemini.generateJson.mockResolvedValue(offTopicOutput);
    habitGoalRelevance.check.mockResolvedValue({ isRelated: false, reason: 'No connection to the goal.' });

    const result = await feature.generate(input);

    expect(result).toEqual(offTopicOutput);
    expect(gemini.generateJson).toHaveBeenCalledTimes(3);
  });
});

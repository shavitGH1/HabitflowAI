import { GeminiClient } from '../gemini.client';
import { PortfolioGeneratorFeature } from './portfolio-generator.feature';

describe('PortfolioGeneratorFeature', () => {
  let gemini: { generateJson: jest.Mock };
  let feature: PortfolioGeneratorFeature;

  const input = {
    goal: 'Run a marathon',
    openAnswers: ['I stuck with drinking water daily for months', 'a', 'b', 'c', 'd', 'e'],
    personaType: 'Achiever' as const,
    weightedBreakdown: { Achievement: 80, Growth: 10, Connection: 0, Exploration: 0, Purpose: 5, Structure: 5 },
  };

  const output = {
    summary: 'You are driven by measurable progress.',
    tips: ['tip1', 'tip2', 'tip3'],
    failurePatterns: ['pattern1'],
    coreGoals: [{ description: 'Run 3 times a week', points: 20, genre: 'goal' as const }],
    dailyVariations: [
      { description: 'Run 2 miles today', points: 20, genre: 'goal' as const },
      { description: 'Stretch after your run', points: 10, genre: 'goal' as const },
      { description: 'Plan tomorrow', points: 5, genre: 'goal' as const },
    ],
  };

  beforeEach(() => {
    gemini = { generateJson: jest.fn() };
    feature = new PortfolioGeneratorFeature(gemini as unknown as GeminiClient);
  });

  it('generates the portfolio in a single call and returns it as-is', async () => {
    gemini.generateJson.mockResolvedValue(output);

    const result = await feature.generate(input);

    expect(result).toEqual(output);
    expect(gemini.generateJson).toHaveBeenCalledTimes(1);
  });
});

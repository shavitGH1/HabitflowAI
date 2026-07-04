import { Pillar } from '../pillars';
import { GeminiClient } from '../gemini.client';
import { PersonaDriftDetectorFeature } from './persona-drift-detector.feature';

const breakdown = (scores: Partial<Record<Pillar, number>>): Record<Pillar, number> => ({
  Achievement: 0,
  Growth: 0,
  Connection: 0,
  Exploration: 0,
  Purpose: 0,
  Structure: 0,
  ...scores,
});

describe('PersonaDriftDetectorFeature', () => {
  let gemini: { generateJson: jest.Mock };
  let feature: PersonaDriftDetectorFeature;

  beforeEach(() => {
    gemini = { generateJson: jest.fn() };
    feature = new PersonaDriftDetectorFeature(gemini as unknown as GeminiClient);
  });

  const snapshot = {
    observationWindowDays: 7,
    recentCompletionRate: 0.6,
    activeStreak: 3,
    completedHabits: ['Morning run'],
    skippedHabits: [],
  };

  it('reports no drift when current behavior matches the baseline', async () => {
    const stable = breakdown({ Achievement: 90, Growth: 10 });
    gemini.generateJson.mockResolvedValue({
      currentBreakdown: stable,
      suggestedPersona: 'Achiever',
      rationale: 'consistent',
    });

    const result = await feature.detect({
      currentPersona: 'Achiever',
      baselineBreakdown: stable,
      behaviorSnapshot: snapshot,
    });

    expect(result.driftDetected).toBe(false);
    expect(result.driftScore).toBe(0);
    expect(result.newSuggestedPersona).toBeNull();
  });

  it('detects drift and suggests a new persona on a large pillar shift', async () => {
    gemini.generateJson.mockResolvedValue({
      currentBreakdown: breakdown({ Connection: 90, Achievement: 10 }),
      suggestedPersona: 'Socializer',
      rationale: 'now socially driven',
    });

    const result = await feature.detect({
      currentPersona: 'Achiever',
      baselineBreakdown: breakdown({ Achievement: 90, Growth: 10 }),
      behaviorSnapshot: snapshot,
    });

    expect(result.driftDetected).toBe(true);
    expect(result.driftScore).toBeGreaterThan(0.3);
    expect(result.newSuggestedPersona).toBe('Socializer');
  });

  it('does not suggest a change when drift stays under the threshold', async () => {
    gemini.generateJson.mockResolvedValue({
      currentBreakdown: breakdown({ Achievement: 80, Growth: 20 }),
      suggestedPersona: 'Achiever',
      rationale: 'slightly broader',
    });

    const result = await feature.detect({
      currentPersona: 'Achiever',
      baselineBreakdown: breakdown({ Achievement: 90, Growth: 10 }),
      behaviorSnapshot: snapshot,
    });

    expect(result.driftDetected).toBe(false);
    expect(result.newSuggestedPersona).toBeNull();
  });
});

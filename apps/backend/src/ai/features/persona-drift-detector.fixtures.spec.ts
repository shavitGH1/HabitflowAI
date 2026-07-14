import { readFileSync } from 'fs';
import { join } from 'path';
import { Pillar, PersonaType } from '../pillars';
import { GeminiClient } from '../gemini.client';
import { DRIFT_THRESHOLD, PersonaDriftDetectorFeature } from './persona-drift-detector.feature';

interface DriftFixture {
  id: string;
  description: string;
  currentPersona: PersonaType;
  baselineBreakdown: Record<Pillar, number>;
  aiCurrentBreakdown: Record<Pillar, number>;
  behaviorSnapshot: {
    observationWindowDays: number;
    recentCompletionRate: number;
    activeStreak: number;
    completedHabits: string[];
    skippedHabits: string[];
  };
  expected: { driftDetected: boolean; newSuggestedPersona: PersonaType | null };
}

const FIXTURES_PATH = join(
  __dirname,
  '../../../test/ai-fixtures/persona-drift-detector/fixtures.json',
);

const fixtures: DriftFixture[] = JSON.parse(readFileSync(FIXTURES_PATH, 'utf-8'));

describe('persona-drift-detector fixtures', () => {
  it('contains at least 10 behavioral snapshots', () => {
    expect(fixtures.length).toBeGreaterThanOrEqual(10);
  });

  it('covers both drift and no-drift cases', () => {
    expect(fixtures.some((f) => f.expected.driftDetected)).toBe(true);
    expect(fixtures.some((f) => !f.expected.driftDetected)).toBe(true);
  });

  it('has unique ids', () => {
    const ids = fixtures.map((f) => f.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  describe.each(fixtures.map((f) => [f.id, f] as const))('%s', (_id, fixture) => {
    const buildFeature = () => {
      const gemini = {
        generateJson: jest.fn().mockResolvedValue({
          currentBreakdown: fixture.aiCurrentBreakdown,
          suggestedPersona: fixture.currentPersona,
          rationale: 'fixture-driven inference',
        }),
      };
      return new PersonaDriftDetectorFeature(gemini as unknown as GeminiClient);
    };

    it('produces the expected drift verdict', async () => {
      const result = await buildFeature().detect({
        currentPersona: fixture.currentPersona,
        baselineBreakdown: fixture.baselineBreakdown,
        behaviorSnapshot: fixture.behaviorSnapshot,
      });

      expect(result.driftDetected).toBe(fixture.expected.driftDetected);
      expect(result.newSuggestedPersona).toBe(fixture.expected.newSuggestedPersona);
    });

    it('keeps the drift score consistent with the threshold verdict', async () => {
      const result = await buildFeature().detect({
        currentPersona: fixture.currentPersona,
        baselineBreakdown: fixture.baselineBreakdown,
        behaviorSnapshot: fixture.behaviorSnapshot,
      });

      if (fixture.expected.driftDetected) {
        expect(result.driftScore).toBeGreaterThan(DRIFT_THRESHOLD);
      } else {
        expect(result.driftScore).toBeLessThanOrEqual(DRIFT_THRESHOLD);
      }
    });
  });
});

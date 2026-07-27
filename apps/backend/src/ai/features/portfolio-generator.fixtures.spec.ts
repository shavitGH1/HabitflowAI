import { readFileSync } from 'fs';
import { join } from 'path';
import { ONBOARDING_QUESTIONS, PERSONA_TYPES, PILLARS, Pillar, PersonaType } from '../pillars';
import { portfolioGeneratorOutputSchema } from '../schemas/portfolio-generator.schema';

interface PortfolioFixture {
  id: string;
  input: {
    goal: string;
    openAnswers: string[];
    personaType: PersonaType;
    weightedBreakdown: Record<Pillar, number>;
  };
  expectedOutput: unknown;
}

const FIXTURES_PATH = join(
  __dirname,
  '../../../test/ai-fixtures/portfolio-generator/fixtures.json',
);

const fixtures: PortfolioFixture[] = JSON.parse(readFileSync(FIXTURES_PATH, 'utf-8'));

describe('portfolio-generator fixtures', () => {
  it('contains at least 10 fixture sets', () => {
    expect(fixtures.length).toBeGreaterThanOrEqual(10);
  });

  it('has unique ids', () => {
    const ids = fixtures.map((f) => f.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  it('covers every persona type', () => {
    for (const persona of PERSONA_TYPES) {
      expect(fixtures.some((f) => f.input.personaType === persona)).toBe(true);
    }
  });

  describe.each(fixtures.map((f) => [f.id, f] as const))('%s', (_id, fixture) => {
    it('has a valid input goal and persona', () => {
      expect(fixture.input.goal.trim().length).toBeGreaterThan(0);
      expect(PERSONA_TYPES).toContain(fixture.input.personaType);
    });

    it('provides one answer per onboarding question', () => {
      expect(fixture.input.openAnswers).toHaveLength(ONBOARDING_QUESTIONS.length);
    });

    it('has a complete weighted breakdown across all six pillars', () => {
      for (const pillar of PILLARS) {
        expect(fixture.input.weightedBreakdown[pillar]).toBeGreaterThanOrEqual(0);
        expect(fixture.input.weightedBreakdown[pillar]).toBeLessThanOrEqual(100);
      }
    });

    it('has an expected output that satisfies the portfolio schema', () => {
      const result = portfolioGeneratorOutputSchema.safeParse(fixture.expectedOutput);
      expect(result.success).toBe(true);
    });
  });
});

import { readFileSync } from 'fs';
import { join } from 'path';
import {
  ONBOARDING_QUESTIONS,
  PERSONA_TYPES,
  PILLARS,
  PILLAR_TO_PERSONA,
  Pillar,
  PersonaType,
} from '../pillars';

interface ClassifierFixture {
  id: string;
  description: string;
  input: { goal: string; openAnswers: string[] };
  expected: { personaType: PersonaType; dominantPillar: Pillar };
}

const FIXTURES_PATH = join(
  __dirname,
  '../../../test/ai-fixtures/persona-classifier/fixtures.json',
);

const fixtures: ClassifierFixture[] = JSON.parse(readFileSync(FIXTURES_PATH, 'utf-8'));

describe('persona-classifier fixtures', () => {
  it('contains between 20 and 30 crafted answer sets', () => {
    expect(fixtures.length).toBeGreaterThanOrEqual(20);
    expect(fixtures.length).toBeLessThanOrEqual(30);
  });

  it('has unique ids', () => {
    const ids = fixtures.map((f) => f.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  it('covers every persona type at least twice', () => {
    for (const persona of PERSONA_TYPES) {
      const count = fixtures.filter((f) => f.expected.personaType === persona).length;
      expect(count).toBeGreaterThanOrEqual(2);
    }
  });

  describe.each(fixtures.map((f) => [f.id, f] as const))('%s', (_id, fixture) => {
    it('provides a non-empty goal', () => {
      expect(fixture.input.goal.trim().length).toBeGreaterThan(0);
    });

    it('provides exactly one answer per onboarding question', () => {
      expect(fixture.input.openAnswers).toHaveLength(ONBOARDING_QUESTIONS.length);
      for (const answer of fixture.input.openAnswers) {
        expect(answer.trim().length).toBeGreaterThan(0);
      }
    });

    it('expects a valid persona and pillar', () => {
      expect(PERSONA_TYPES).toContain(fixture.expected.personaType);
      expect(PILLARS).toContain(fixture.expected.dominantPillar);
    });

    it('keeps the expected persona consistent with its dominant pillar', () => {
      expect(PILLAR_TO_PERSONA[fixture.expected.dominantPillar]).toBe(fixture.expected.personaType);
    });
  });
});

import { InternalServerErrorException } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { CoachPhrasingFeature, CoachPhrasingInput } from './coach-phrasing.feature';
import { coachPhrasingOutputSchema } from '../schemas/coach-phrasing.schema';

const TEMPLATE = 'You kept most of your plan going this week.';

const input = (overrides: Partial<CoachPhrasingInput> = {}): CoachPhrasingInput => ({
  userId: 'u1',
  personaType: 'Achiever',
  baseMessage: TEMPLATE,
  completionRate7d: 0.6,
  streak: 3,
  cacheTag: 'weekly',
  ...overrides,
});

describe('CoachPhrasingFeature', () => {
  let gemini: { generateJson: jest.Mock };
  let feature: CoachPhrasingFeature;

  beforeEach(() => {
    gemini = { generateJson: jest.fn() };
    feature = new CoachPhrasingFeature(gemini as unknown as GeminiClient);
  });

  it('returns the rewritten message from the model', async () => {
    gemini.generateJson.mockResolvedValue({ message: 'Six out of ten. Beat it next week.' });

    await expect(feature.phrase(input())).resolves.toBe('Six out of ten. Beat it next week.');
  });

  it('falls back to the template when the model call fails', async () => {
    gemini.generateJson.mockRejectedValue(new InternalServerErrorException('overloaded'));

    await expect(feature.phrase(input())).resolves.toBe(TEMPLATE);
  });

  it('falls back to the template when the response fails schema validation', async () => {
    gemini.generateJson.mockRejectedValue(
      new InternalServerErrorException('AI returned invalid output. Please try again.'),
    );

    await expect(feature.phrase(input())).resolves.toBe(TEMPLATE);
  });

  it('falls back to the template when the response is malformed JSON', async () => {
    gemini.generateJson.mockRejectedValue(
      new InternalServerErrorException('AI returned malformed JSON.'),
    );

    await expect(feature.phrase(input())).resolves.toBe(TEMPLATE);
  });

  it('calls the model once per user, day and tag', async () => {
    gemini.generateJson.mockResolvedValue({ message: 'cached' });

    await feature.phrase(input());
    await feature.phrase(input());

    expect(gemini.generateJson).toHaveBeenCalledTimes(1);
  });

  it('calls the model again for a different tag', async () => {
    gemini.generateJson.mockResolvedValue({ message: 'ok' });

    await feature.phrase(input({ cacheTag: 'daily' }));
    await feature.phrase(input({ cacheTag: 'weekly' }));

    expect(gemini.generateJson).toHaveBeenCalledTimes(2);
  });
});

describe('coachPhrasingOutputSchema', () => {
  it('rejects an empty message', () => {
    expect(coachPhrasingOutputSchema.safeParse({ message: '' }).success).toBe(false);
  });

  it('rejects a message longer than 300 characters', () => {
    expect(coachPhrasingOutputSchema.safeParse({ message: 'a'.repeat(301) }).success).toBe(false);
  });

  it('accepts a message within the limit', () => {
    expect(coachPhrasingOutputSchema.safeParse({ message: TEMPLATE }).success).toBe(true);
  });
});

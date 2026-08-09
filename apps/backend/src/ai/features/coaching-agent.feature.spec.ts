import { GeminiClient } from '../gemini.client';
import { CoachingAgentFeature } from './coaching-agent.feature';

describe('CoachingAgentFeature', () => {
  let gemini: { generateJson: jest.Mock };
  let feature: CoachingAgentFeature;

  const input = {
    message: 'I keep skipping my morning run, what should I do?',
    personaType: 'Achiever' as const,
    activeGoal: { id: 'goal-1', title: 'Run a marathon', targetDate: '2026-12-31' },
    habits: [{ id: 'habit-1', title: 'Morning Run', goalId: 'goal-1', consistencyScore: 0.2, streak: 0 }],
    driftSuggestedPersona: null,
  };

  beforeEach(() => {
    gemini = { generateJson: jest.fn() };
    feature = new CoachingAgentFeature(gemini as unknown as GeminiClient);
  });

  it('returns the reply and null proposedChange when the AI proposes nothing', async () => {
    gemini.generateJson.mockResolvedValue({ reply: 'Keep at it!', proposedChange: null });

    const result = await feature.converse(input);

    expect(result).toEqual({ reply: 'Keep at it!', proposedChange: null });
  });

  it('normalizes an omitted proposedChange field to null', async () => {
    gemini.generateJson.mockResolvedValue({ reply: 'Keep at it!' });

    const result = await feature.converse(input);

    expect(result.proposedChange).toBeNull();
  });

  it('passes through a valid adjustDifficulty proposal', async () => {
    const proposedChange = { type: 'adjustDifficulty', rationale: 'Repeated misses this week', direction: 'decrease' };
    gemini.generateJson.mockResolvedValue({ reply: 'Let\'s ease off a bit.', proposedChange });

    const result = await feature.converse(input);

    expect(result.proposedChange).toEqual(proposedChange);
  });

  it('falls back to a plain reply when the AI call fails — coach-chat never hard-fails', async () => {
    gemini.generateJson.mockRejectedValue(new Error('Gemini overloaded'));

    const result = await feature.converse(input);

    expect(result.proposedChange).toBeNull();
    expect(result.reply).toEqual(expect.any(String));
  });

  it('falls back to a plain reply on a schema-invalid response', async () => {
    gemini.generateJson.mockRejectedValue(new Error('AI returned invalid output. Please try again.'));

    const result = await feature.converse(input);

    expect(result.proposedChange).toBeNull();
  });
});

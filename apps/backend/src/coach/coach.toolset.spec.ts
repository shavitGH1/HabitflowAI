import { AgentTool, ToolRejectedError } from '../ai/agent/agent-tool';
import { GoalRepository } from '../goals/goal.repository';
import { HabitData, HabitRepository } from '../habits/habit.repository';
import { PersonasService } from '../personas/personas.service';
import { UserRepository } from '../users/user.repository';
import { CoachToolset } from './coach.toolset';

const USER_ID = 'user-1';
const GOAL_ID = 'goal-1';
const TODAY = new Date().toISOString().split('T')[0];

const mockUserRepository = { findUserById: jest.fn() };
const mockHabitRepository = { findByUserId: jest.fn() };
const mockGoalRepository = { findActiveByUserId: jest.fn() };
const mockPersonasService = { driftCheck: jest.fn() };

const makeHabit = (overrides: Partial<HabitData> = {}): HabitData =>
  ({
    id: 'habit-1',
    title: 'Morning Run',
    frequency: 'daily',
    streak: 0,
    consistencyScore: 0.1,
    completionHistory: [],
    completionNotes: [],
    createdAt: new Date().toISOString(),
    ...overrides,
  }) as HabitData;

const makeToolset = () =>
  new CoachToolset(
    mockUserRepository as unknown as UserRepository,
    mockHabitRepository as unknown as HabitRepository,
    mockGoalRepository as unknown as GoalRepository,
    mockPersonasService as unknown as PersonasService,
  );

const toolNamed = (tools: AgentTool[], name: string): AgentTool =>
  tools.find((tool) => tool.name === name)!;

describe('CoachToolset', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    mockUserRepository.findUserById.mockResolvedValue({
      personaType: 'Achiever',
      goal: 'Run a 10k',
      coreGoals: [],
      dailyVariations: [],
    });
    mockHabitRepository.findByUserId.mockResolvedValue([]);
    mockGoalRepository.findActiveByUserId.mockResolvedValue(null);
  });

  it('exposes seven tools with disjoint responsibilities', () => {
    const { tools } = makeToolset().open(USER_ID);

    expect(tools.map((tool) => tool.name)).toEqual([
      'get_progress_summary',
      'get_habit_list',
      'get_daily_tasks',
      'get_persona_profile',
      'get_active_goal',
      'check_persona_drift',
      'propose_change',
    ]);
  });

  it('get_daily_tasks surfaces the onboarding-generated goals and today\'s variations', async () => {
    mockUserRepository.findUserById.mockResolvedValue({
      personaType: 'Achiever',
      goal: 'Run a 10k',
      coreGoals: [
        { id: 'core-1', description: 'Run three times a week', points: 30, completed: true, genre: 'goal' },
      ],
      dailyVariations: [
        { id: 'daily-1', description: 'Stretch for 10 minutes', points: 10, completed: false, genre: 'persona' },
      ],
    });

    const { tools } = makeToolset().open(USER_ID);

    expect(await toolNamed(tools, 'get_daily_tasks').run({})).toEqual({
      coreGoals: [{ id: 'core-1', description: 'Run three times a week', points: 30, completed: true }],
      dailyTasks: [{ id: 'daily-1', description: 'Stretch for 10 minutes', points: 10, completed: false }],
    });
  });

  it('get_progress_summary hands the model a band and a verdict it did not compute', async () => {
    mockHabitRepository.findByUserId.mockResolvedValue([
      makeHabit({ streak: 3, consistencyScore: 0.9, completionHistory: Array(7).fill(TODAY) }),
    ]);

    const { tools } = makeToolset().open(USER_ID);
    const output = (await toolNamed(tools, 'get_progress_summary').run({})) as Record<string, unknown>;

    expect(output).toMatchObject({
      completionRate7dPercent: 100,
      longestActiveStreak: 3,
      habitCount: 1,
      band: 'EXCELLENT',
      completedToday: ['Morning Run'],
    });
    expect(output.verdict).toBe(
      'You hit almost everything you planned this week. Treat next week as a number to beat. You are steady enough to add one more habit.',
    );
  });

  it('get_habit_list stays limited to per-habit facts', async () => {
    mockHabitRepository.findByUserId.mockResolvedValue([
      makeHabit({ streak: 3, consistencyScore: 0.42, goalId: GOAL_ID }),
    ]);

    const { tools } = makeToolset().open(USER_ID);

    expect(await toolNamed(tools, 'get_habit_list').run({})).toEqual({
      habits: [
        {
          id: 'habit-1',
          title: 'Morning Run',
          frequency: 'daily',
          streak: 3,
          consistencyPercent: 42,
          linkedGoalId: GOAL_ID,
        },
      ],
    });
  });

  it('caches check_persona_drift instead of paying for a second analysis', async () => {
    mockPersonasService.driftCheck.mockResolvedValue({
      driftDetected: true,
      driftScore: 0.44,
      newSuggestedPersona: 'Grower',
    });

    const { tools } = makeToolset().open(USER_ID);
    const first = await toolNamed(tools, 'check_persona_drift').run({});
    const second = await toolNamed(tools, 'check_persona_drift').run({});

    expect(mockPersonasService.driftCheck).toHaveBeenCalledTimes(1);
    expect(first).toEqual({ driftDetected: true, suggestedPersona: 'Grower', driftScore: 0.44 });
    expect(second).toEqual({ driftDetected: true, suggestedPersona: 'Grower', cached: true });
  });

  it('propose_change stages a change the thresholds allow', async () => {
    mockHabitRepository.findByUserId.mockResolvedValue([makeHabit()]);
    const session = makeToolset().open(USER_ID);

    const output = await toolNamed(session.tools, 'propose_change').run({
      type: 'adjustDifficulty',
      rationale: 'nothing completed in a week',
      direction: 'decrease',
    });

    expect(output).toMatchObject({ staged: true, awaitingUserConfirmation: true });
    expect(session.proposedChange()).toEqual({
      type: 'adjustDifficulty',
      rationale: 'nothing completed in a week',
      direction: 'decrease',
    });
  });

  it('rejects a change the thresholds do not support, with a reason the model can use', async () => {
    mockHabitRepository.findByUserId.mockResolvedValue([makeHabit()]);
    const session = makeToolset().open(USER_ID);

    await expect(
      toolNamed(session.tools, 'propose_change').run({
        type: 'adjustDifficulty',
        rationale: 'they seem to be doing great',
        direction: 'increase',
      }),
    ).rejects.toBeInstanceOf(ToolRejectedError);
    expect(session.proposedChange()).toBeNull();
  });

  it('refuses a persona switch that was never backed by a drift check', async () => {
    const session = makeToolset().open(USER_ID);

    await expect(
      toolNamed(session.tools, 'propose_change').run({
        type: 'personaSwitch',
        rationale: 'feels right',
        suggestedPersona: 'Grower',
      }),
    ).rejects.toThrow(/check_persona_drift/);
  });

  it('refuses to forfeit a goal whose habits are still holding', async () => {
    mockGoalRepository.findActiveByUserId.mockResolvedValue({ id: GOAL_ID });
    mockHabitRepository.findByUserId.mockResolvedValue([
      makeHabit({ goalId: GOAL_ID, streak: 5, consistencyScore: 0.8 }),
    ]);
    const session = makeToolset().open(USER_ID);

    await expect(
      toolNamed(session.tools, 'propose_change').run({
        type: 'forfeitGoal',
        rationale: 'they sound discouraged',
        goalId: GOAL_ID,
      }),
    ).rejects.toThrow(/sustained failure pattern/);
  });

  it('rejects an unknown persona before the policy is even consulted', async () => {
    const session = makeToolset().open(USER_ID);

    await expect(
      toolNamed(session.tools, 'propose_change').run({
        type: 'personaSwitch',
        rationale: 'made up',
        suggestedPersona: 'Wizard',
      }),
    ).rejects.toThrow();
    expect(mockHabitRepository.findByUserId).not.toHaveBeenCalled();
  });

  it('keeps state isolated between sessions', async () => {
    mockHabitRepository.findByUserId.mockResolvedValue([makeHabit()]);
    const toolset = makeToolset();
    const first = toolset.open(USER_ID);
    const second = toolset.open('user-2');

    await toolNamed(first.tools, 'propose_change').run({
      type: 'adjustDifficulty',
      rationale: 'struggling',
      direction: 'decrease',
    });

    expect(second.proposedChange()).toBeNull();
  });

  it('falls back to the deterministic weekly summary when the model is unavailable', async () => {
    mockHabitRepository.findByUserId.mockResolvedValue([makeHabit()]);
    const session = makeToolset().open(USER_ID);

    expect(await session.fallbackReply()).toBe(
      'I could not think that through properly just now, but here is where you stand. ' +
        'Almost nothing was marked as done this week. Treat next week as a number to beat. ' +
        'Restart small: pick one habit for tomorrow and ignore the rest.',
    );
  });

  it('falls back to a plain apology when even the database is unreachable', async () => {
    mockHabitRepository.findByUserId.mockRejectedValue(new Error('db down'));
    const session = makeToolset().open(USER_ID);

    expect(await session.fallbackReply()).toBe(
      'I could not think that through properly just now — try me again in a moment.',
    );
  });
});

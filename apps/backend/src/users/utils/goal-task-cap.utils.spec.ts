import { GoalTask } from '../../dto/goal.dto';
import { capGoalTasks } from './goal-task-cap.utils';

const makeTask = (overrides: Partial<GoalTask> = {}): GoalTask => ({
  id: `task-${Math.random()}`,
  description: 'A task',
  points: 10,
  completed: false,
  genre: 'goal',
  ...overrides,
});

describe('capGoalTasks()', () => {
  it('keeps everything when the combined goal-task count is at or under the cap', () => {
    const coreGoals = [makeTask({ id: '1' }), makeTask({ id: '2' })];
    const dailyVariations = [makeTask({ id: '3' })];

    const result = capGoalTasks(coreGoals, dailyVariations);

    expect(result.coreGoals).toHaveLength(2);
    expect(result.dailyVariations).toHaveLength(1);
  });

  it('caps the combined "goal" genre total at 5, coreGoals first then dailyVariations in order', () => {
    const coreGoals = [makeTask({ id: 'c1' }), makeTask({ id: 'c2' }), makeTask({ id: 'c3' })];
    const dailyVariations = [
      makeTask({ id: 'd1' }),
      makeTask({ id: 'd2' }),
      makeTask({ id: 'd3' }),
      makeTask({ id: 'd4' }),
    ];

    const result = capGoalTasks(coreGoals, dailyVariations);

    expect(result.coreGoals.map(t => t.id)).toEqual(['c1', 'c2', 'c3']);
    expect(result.dailyVariations.map(t => t.id)).toEqual(['d1', 'd2']);
  });

  it('never drops an already-completed task, even past the cap', () => {
    const coreGoals = [
      makeTask({ id: 'c1', completed: true }),
      makeTask({ id: 'c2', completed: true }),
      makeTask({ id: 'c3', completed: true }),
      makeTask({ id: 'c4', completed: true }),
      makeTask({ id: 'c5', completed: true }),
      makeTask({ id: 'c6', completed: true }),
    ];
    const dailyVariations = [makeTask({ id: 'd1', completed: false })];

    const result = capGoalTasks(coreGoals, dailyVariations);

    expect(result.coreGoals).toHaveLength(6);
    expect(result.dailyVariations).toHaveLength(0);
  });

  it('never touches non-"goal" genre tasks', () => {
    const coreGoals = Array.from({ length: 6 }, (_, i) => makeTask({ id: `c${i}` }));
    const dailyVariations = [
      makeTask({ id: 'h1', genre: 'habit', habitId: 'habit-1' }),
      makeTask({ id: 'p1', genre: 'persona', habitId: 'habit-1' }),
    ];

    const result = capGoalTasks(coreGoals, dailyVariations);

    expect(result.dailyVariations.map(t => t.id)).toEqual(['h1', 'p1']);
  });
});

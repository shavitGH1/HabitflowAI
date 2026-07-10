import { ForbiddenException, NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { CreateHabitDto } from './dto/create-habit.dto';
import { HabitData, HabitRepository } from './habit.repository';
import { HabitsService } from './habits.service';

const mockHabitRepository = {
  createHabit: jest.fn(),
  findByUserId: jest.fn(),
  findById: jest.fn(),
  updateHabit: jest.fn(),
  deleteHabit: jest.fn(),
  completeHabit: jest.fn(),
};

const USER_ID = 'user-123';
const OTHER_USER_ID = 'user-999';
const HABIT_ID = 'habit-abc';

const makeHabit = (overrides: Partial<HabitData> = {}): HabitData => ({
  id: HABIT_ID,
  userId: USER_ID,
  title: 'Morning Run',
  description: '',
  frequency: 'daily',
  targetCount: 1,
  streak: 0,
  completionHistory: [],
  persona: '',
  isArchived: false,
  createdAt: new Date().toISOString(),
  ...overrides,
});

describe('HabitsService', () => {
  let service: HabitsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        HabitsService,
        { provide: HabitRepository, useValue: mockHabitRepository },
      ],
    }).compile();

    service = module.get<HabitsService>(HabitsService);
    jest.clearAllMocks();
  });

  describe('createHabit()', () => {
    it('injects userId from the caller — never from the DTO', async () => {
      const dto: CreateHabitDto = { title: 'Morning Run', frequency: 'daily' };
      const expected = makeHabit();
      mockHabitRepository.createHabit.mockResolvedValue(expected);

      const result = await service.createHabit(USER_ID, dto);

      expect(mockHabitRepository.createHabit).toHaveBeenCalledWith(
        expect.objectContaining({ userId: USER_ID, title: 'Morning Run', frequency: 'daily' }),
      );
      expect(result).toEqual(expected);
    });
  });

  describe('findByUserId()', () => {
    it('returns non-archived habits for the user', async () => {
      const habits = [
        makeHabit({ id: 'h1', title: 'Run' }),
        makeHabit({ id: 'h2', title: 'Read' }),
      ];
      mockHabitRepository.findByUserId.mockResolvedValue(habits);

      const result = await service.findByUserId(USER_ID);

      expect(mockHabitRepository.findByUserId).toHaveBeenCalledWith(USER_ID);
      expect(result).toHaveLength(2);
      expect(result.every((h) => !h.isArchived)).toBe(true);
    });
  });

  describe('deleteHabit()', () => {
    it('throws NotFoundException when habit does not exist', async () => {
      mockHabitRepository.findById.mockResolvedValue(null);

      await expect(service.deleteHabit(USER_ID, HABIT_ID)).rejects.toThrow(NotFoundException);
      expect(mockHabitRepository.deleteHabit).not.toHaveBeenCalled();
    });

    it('throws ForbiddenException and skips repository when caller does not own the habit', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit({ userId: OTHER_USER_ID }));

      await expect(service.deleteHabit(USER_ID, HABIT_ID)).rejects.toThrow(ForbiddenException);
      expect(mockHabitRepository.deleteHabit).not.toHaveBeenCalled();
    });
  });

  describe('completeHabit()', () => {
    it('throws ForbiddenException and skips repository when caller does not own the habit', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit({ userId: OTHER_USER_ID }));

      await expect(service.completeHabit(USER_ID, HABIT_ID)).rejects.toThrow(ForbiddenException);
      expect(mockHabitRepository.completeHabit).not.toHaveBeenCalled();
    });

    it('returns streak incremented by 1 on a consecutive day completion', async () => {
      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);
      const yesterdayStr = yesterday.toISOString().split('T')[0];
      const todayStr = new Date().toISOString().split('T')[0];

      mockHabitRepository.findById.mockResolvedValue(
        makeHabit({ streak: 1, completionHistory: [yesterdayStr] }),
      );
      mockHabitRepository.completeHabit.mockResolvedValue(
        makeHabit({ streak: 2, completionHistory: [yesterdayStr, todayStr] }),
      );

      const result = await service.completeHabit(USER_ID, HABIT_ID);

      expect(mockHabitRepository.completeHabit).toHaveBeenCalledWith(HABIT_ID);
      expect(result.streak).toBe(2);
    });

    it('resets streak to 1 when a day was missed before today', async () => {
      const twoDaysAgo = new Date();
      twoDaysAgo.setDate(twoDaysAgo.getDate() - 2);
      const twoDaysAgoStr = twoDaysAgo.toISOString().split('T')[0];
      const todayStr = new Date().toISOString().split('T')[0];

      mockHabitRepository.findById.mockResolvedValue(
        makeHabit({ streak: 3, completionHistory: [twoDaysAgoStr] }),
      );
      mockHabitRepository.completeHabit.mockResolvedValue(
        makeHabit({ streak: 1, completionHistory: [twoDaysAgoStr, todayStr] }),
      );

      const result = await service.completeHabit(USER_ID, HABIT_ID);

      expect(result.streak).toBe(1);
    });
  });
});

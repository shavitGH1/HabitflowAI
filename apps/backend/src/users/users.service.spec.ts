import { BadRequestException, NotFoundException, UnauthorizedException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import bcrypt from 'bcrypt';
import { AiService } from '../ai/ai.service';
import { HabitRepository } from '../habits/habit.repository';
import { GoalRepository } from '../goals/goal.repository';
import { STORAGE_ADAPTER } from '../storage/storage.adapter';
import { HabitData } from '../habits/habit.repository';
import { GoalData } from '../goals/goal.repository';
import { UserData, UserRepository } from './user.repository';
import { UsersService } from './users.service';

const mockUserRepository = {
  findUserById: jest.fn(),
  findAllUsers: jest.fn(),
  updateProfilePicture: jest.fn(),
  updateName: jest.fn(),
  updatePassword: jest.fn(),
};

const mockHabitRepository = {
  findByUserId: jest.fn(),
};

const mockGoalRepository = {
  findActiveByUserId: jest.fn(),
  findById: jest.fn(),
};

const mockAiService = {
  generateDailyVariations: jest.fn(),
};

const mockStorage = {
  upload: jest.fn(),
  delete: jest.fn(),
};

const USER_ID = 'user-123';

const makeHabit = (overrides: Partial<HabitData> = {}): HabitData => ({
  id: 'habit-1',
  userId: USER_ID,
  title: 'Morning Run',
  description: '',
  frequency: 'daily',
  targetCount: 1,
  streak: 0,
  completionHistory: [],
  persona: '',
  isArchived: false,
  consistencyScore: 0,
  completionNotes: [],
  createdAt: '2026-08-01T00:00:00.000Z',
  ...overrides,
});

const makeGoal = (overrides: Partial<GoalData> = {}): GoalData => ({
  id: 'goal-1',
  userId: USER_ID,
  title: 'Run a marathon',
  targetDate: '2026-12-01T00:00:00.000Z',
  status: 'active',
  createdAt: '2026-08-01T00:00:00.000Z',
  ...overrides,
});

const makeUser = (overrides: Partial<UserData> = {}): UserData => ({
  id: USER_ID,
  email: 'user@example.com',
  firstName: 'Test',
  lastName: 'User',
  password: 'hashed',
  authProvider: 'local',
  goal: 'Stay consistent',
  personaType: 'Achiever',
  motivationalMessage: 'Keep going',
  coreGoals: [],
  dailyVariations: [],
  tasksLastGeneratedDate: '',
  ...overrides,
});

describe('UsersService', () => {
  let service: UsersService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        UsersService,
        { provide: UserRepository, useValue: mockUserRepository },
        { provide: HabitRepository, useValue: mockHabitRepository },
        { provide: GoalRepository, useValue: mockGoalRepository },
        { provide: AiService, useValue: mockAiService },
        { provide: STORAGE_ADAPTER, useValue: mockStorage },
      ],
    }).compile();

    service = module.get<UsersService>(UsersService);
    jest.clearAllMocks();
  });

  describe('getAllUsers()', () => {
    it('returns id, email, name and profilePicture for every user', async () => {
      mockUserRepository.findAllUsers.mockResolvedValue([
        makeUser({ profilePicture: 'preset:3' }),
        makeUser({ id: 'user-456', email: 'other@example.com', profilePicture: undefined }),
      ]);

      const result = await service.getAllUsers();

      expect(result).toEqual([
        { id: USER_ID, email: 'user@example.com', firstName: 'Test', lastName: 'User', profilePicture: 'preset:3' },
        { id: 'user-456', email: 'other@example.com', firstName: 'Test', lastName: 'User', profilePicture: undefined },
      ]);
    });
  });

  describe('getHomePageData()', () => {
    const today = new Date().toISOString().split('T')[0];

    it('averages consistencyScore only across habits that have been completed at least once', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ tasksLastGeneratedDate: today }));
      mockHabitRepository.findByUserId.mockResolvedValue([
        makeHabit({ id: 'h1', consistencyScore: 0.96, completionHistory: ['2026-08-25'] }),
        makeHabit({ id: 'h2', consistencyScore: 0, completionHistory: [] }),
      ]);
      mockGoalRepository.findActiveByUserId.mockResolvedValue(null);

      const result = await service.getHomePageData(USER_ID);

      expect(result.confidenceScore).toBeCloseTo(0.96, 5);
    });

    it('reports 0 consistency when no habit has ever been completed', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ tasksLastGeneratedDate: today }));
      mockHabitRepository.findByUserId.mockResolvedValue([
        makeHabit({ id: 'h1', consistencyScore: 0, completionHistory: [] }),
      ]);
      mockGoalRepository.findActiveByUserId.mockResolvedValue(null);

      const result = await service.getHomePageData(USER_ID);

      expect(result.confidenceScore).toBe(0);
    });

    it('reports 0 consistency and empty history for a user with no habits at all', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ tasksLastGeneratedDate: today }));
      mockHabitRepository.findByUserId.mockResolvedValue([]);
      mockGoalRepository.findActiveByUserId.mockResolvedValue(null);

      const result = await service.getHomePageData(USER_ID);

      expect(result.confidenceScore).toBe(0);
      expect(result.completionHistory).toEqual([]);
    });

    it('finds the goal calendar history via the real goalId link, not the habit title', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ tasksLastGeneratedDate: today, goal: 'Run a marathon' }));
      mockHabitRepository.findByUserId.mockResolvedValue([
        makeHabit({ id: 'h1', title: 'Evening Jog', goalId: 'goal-1', completionHistory: ['2026-08-20', '2026-08-21'] }),
        makeHabit({ id: 'h2', title: 'Read 10 pages', completionHistory: ['2026-08-22'] }),
      ]);
      mockGoalRepository.findActiveByUserId.mockResolvedValue(makeGoal({ id: 'goal-1' }));

      const result = await service.getHomePageData(USER_ID);

      expect(result.completionHistory).toEqual(['2026-08-20', '2026-08-21']);
    });

    it('unions completion history across every habit linked to the active goal', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ tasksLastGeneratedDate: today, goal: 'Run a marathon' }));
      mockHabitRepository.findByUserId.mockResolvedValue([
        makeHabit({ id: 'h1', title: 'Evening Jog', goalId: 'goal-1', completionHistory: ['2026-08-20', '2026-08-21'] }),
        makeHabit({ id: 'h2', title: 'Push-ups', goalId: 'goal-1', completionHistory: ['2026-08-21', '2026-08-22'] }),
      ]);
      mockGoalRepository.findActiveByUserId.mockResolvedValue(makeGoal({ id: 'goal-1' }));

      const result = await service.getHomePageData(USER_ID);

      expect(result.completionHistory?.sort()).toEqual(['2026-08-20', '2026-08-21', '2026-08-22']);
    });

    it('leaves the goal calendar history empty when no habit is linked to the active goal', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ tasksLastGeneratedDate: today }));
      mockHabitRepository.findByUserId.mockResolvedValue([
        makeHabit({ id: 'h1', title: 'Evening Jog', completionHistory: ['2026-08-20'] }),
      ]);
      mockGoalRepository.findActiveByUserId.mockResolvedValue(makeGoal({ id: 'goal-1' }));

      const result = await service.getHomePageData(USER_ID);

      expect(result.completionHistory).toEqual([]);
    });

    it('resolves each achievement with its goal title', async () => {
      mockUserRepository.findUserById.mockResolvedValue(
        makeUser({
          tasksLastGeneratedDate: today,
          achievements: [
            { goalId: 'goal-1', medal: 'gold', awardedAt: '2026-08-10T00:00:00.000Z' },
            { goalId: 'goal-2', medal: 'silver', awardedAt: '2026-08-15T00:00:00.000Z' },
          ],
        }),
      );
      mockHabitRepository.findByUserId.mockResolvedValue([]);
      mockGoalRepository.findActiveByUserId.mockResolvedValue(null);
      mockGoalRepository.findById.mockImplementation((id: string) =>
        Promise.resolve(id === 'goal-1' ? makeGoal({ id: 'goal-1', title: 'Run a marathon' }) : makeGoal({ id: 'goal-2', title: 'Read 12 books' })),
      );

      const result = await service.getHomePageData(USER_ID);

      expect(result.achievements).toEqual([
        { goalId: 'goal-1', goalTitle: 'Run a marathon', medal: 'gold', awardedAt: '2026-08-10T00:00:00.000Z' },
        { goalId: 'goal-2', goalTitle: 'Read 12 books', medal: 'silver', awardedAt: '2026-08-15T00:00:00.000Z' },
      ]);
    });

    it('falls back to a generic title when the linked goal no longer exists', async () => {
      mockUserRepository.findUserById.mockResolvedValue(
        makeUser({
          tasksLastGeneratedDate: today,
          achievements: [{ goalId: 'deleted-goal', medal: 'gold', awardedAt: '2026-08-10T00:00:00.000Z' }],
        }),
      );
      mockHabitRepository.findByUserId.mockResolvedValue([]);
      mockGoalRepository.findActiveByUserId.mockResolvedValue(null);
      mockGoalRepository.findById.mockResolvedValue(null);

      const result = await service.getHomePageData(USER_ID);

      expect(result.achievements[0].goalTitle).toBe('Goal');
    });
  });

  describe('updateProfilePicture()', () => {
    it('throws NotFoundException when the user does not exist', async () => {
      mockUserRepository.findUserById.mockResolvedValue(null);

      await expect(service.updateProfilePicture(USER_ID, 'preset:1')).rejects.toThrow(NotFoundException);
      expect(mockUserRepository.updateProfilePicture).not.toHaveBeenCalled();
    });

    it('persists the preset key and returns it', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser());
      mockUserRepository.updateProfilePicture.mockResolvedValue(makeUser({ profilePicture: 'preset:2' }));

      const result = await service.updateProfilePicture(USER_ID, 'preset:2');

      expect(mockUserRepository.updateProfilePicture).toHaveBeenCalledWith(USER_ID, 'preset:2');
      expect(result).toEqual({ profilePicture: 'preset:2', success: true });
    });
  });

  describe('updateName()', () => {
    it('throws NotFoundException when the user does not exist', async () => {
      mockUserRepository.findUserById.mockResolvedValue(null);

      await expect(service.updateName(USER_ID, 'Alex', 'Morgan')).rejects.toThrow(NotFoundException);
      expect(mockUserRepository.updateName).not.toHaveBeenCalled();
    });

    it('updates the name when it has never been changed before', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ nameChangedAt: undefined }));
      mockUserRepository.updateName.mockResolvedValue(
        makeUser({ firstName: 'Alex', lastName: 'Morgan', nameChangedAt: '2026-08-19T00:00:00.000Z' }),
      );

      const result = await service.updateName(USER_ID, 'Alex', 'Morgan');

      expect(mockUserRepository.updateName).toHaveBeenCalledWith(USER_ID, 'Alex', 'Morgan');
      expect(result).toEqual({
        firstName: 'Alex',
        lastName: 'Morgan',
        nameChangedAt: '2026-08-19T00:00:00.000Z',
        success: true,
      });
    });

    it('updates the name when the 3-month cooldown has already elapsed', async () => {
      const fourMonthsAgo = new Date();
      fourMonthsAgo.setMonth(fourMonthsAgo.getMonth() - 4);
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ nameChangedAt: fourMonthsAgo.toISOString() }));
      mockUserRepository.updateName.mockResolvedValue(makeUser({ firstName: 'Alex', lastName: 'Morgan' }));

      await service.updateName(USER_ID, 'Alex', 'Morgan');

      expect(mockUserRepository.updateName).toHaveBeenCalledWith(USER_ID, 'Alex', 'Morgan');
    });

    it('rejects the change when still within the 3-month cooldown', async () => {
      const oneMonthAgo = new Date();
      oneMonthAgo.setMonth(oneMonthAgo.getMonth() - 1);
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ nameChangedAt: oneMonthAgo.toISOString() }));

      await expect(service.updateName(USER_ID, 'Alex', 'Morgan')).rejects.toThrow(BadRequestException);
      expect(mockUserRepository.updateName).not.toHaveBeenCalled();
    });
  });

  describe('changePassword()', () => {
    it('throws NotFoundException when the user does not exist', async () => {
      mockUserRepository.findUserById.mockResolvedValue(null);

      await expect(service.changePassword(USER_ID, 'old-pass', 'new-pass')).rejects.toThrow(NotFoundException);
    });

    it('throws BadRequestException for a Google account, without checking the current password', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ authProvider: 'google' }));

      await expect(service.changePassword(USER_ID, 'anything', 'new-pass')).rejects.toThrow(BadRequestException);
      expect(mockUserRepository.updatePassword).not.toHaveBeenCalled();
    });

    it('throws UnauthorizedException when the current password is wrong', async () => {
      mockUserRepository.findUserById.mockResolvedValue(
        makeUser({ password: await bcrypt.hash('correct-pass', 10) }),
      );

      await expect(service.changePassword(USER_ID, 'wrong-pass', 'new-pass')).rejects.toThrow(UnauthorizedException);
      expect(mockUserRepository.updatePassword).not.toHaveBeenCalled();
    });

    it('hashes and persists the new password when the current one is correct', async () => {
      mockUserRepository.findUserById.mockResolvedValue(
        makeUser({ password: await bcrypt.hash('correct-pass', 10) }),
      );
      mockUserRepository.updatePassword.mockResolvedValue(makeUser());

      const result = await service.changePassword(USER_ID, 'correct-pass', 'new-pass');

      expect(mockUserRepository.updatePassword).toHaveBeenCalledWith(USER_ID, expect.any(String));
      const persistedHash = mockUserRepository.updatePassword.mock.calls[0][1];
      expect(await bcrypt.compare('new-pass', persistedHash)).toBe(true);
      expect(result).toEqual({ success: true });
    });
  });

  describe('uploadAvatar()', () => {
    const file = { originalname: 'me.jpg', buffer: Buffer.from('fake-image') } as Express.Multer.File;

    it('uploads the file, persists the returned URL, and does not delete a preset avatar', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ profilePicture: 'preset:1' }));
      mockStorage.upload.mockResolvedValue('/uploads/abc.jpg');
      mockUserRepository.updateProfilePicture.mockResolvedValue(makeUser({ profilePicture: '/uploads/abc.jpg' }));

      const result = await service.uploadAvatar(USER_ID, file);

      expect(mockStorage.upload).toHaveBeenCalledWith(file);
      expect(mockStorage.delete).not.toHaveBeenCalled();
      expect(mockUserRepository.updateProfilePicture).toHaveBeenCalledWith(USER_ID, '/uploads/abc.jpg');
      expect(result).toEqual({ profilePicture: '/uploads/abc.jpg', success: true });
    });

    it('deletes the previous uploaded avatar before persisting the new one', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ profilePicture: '/uploads/old.jpg' }));
      mockStorage.upload.mockResolvedValue('/uploads/new.jpg');
      mockUserRepository.updateProfilePicture.mockResolvedValue(makeUser({ profilePicture: '/uploads/new.jpg' }));

      await service.uploadAvatar(USER_ID, file);

      expect(mockStorage.delete).toHaveBeenCalledWith('/uploads/old.jpg');
    });

    it('throws NotFoundException when the user does not exist', async () => {
      mockUserRepository.findUserById.mockResolvedValue(null);

      await expect(service.uploadAvatar(USER_ID, file)).rejects.toThrow(NotFoundException);
      expect(mockStorage.upload).not.toHaveBeenCalled();
    });
  });
});

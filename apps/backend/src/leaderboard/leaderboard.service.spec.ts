import { Test, TestingModule } from '@nestjs/testing';
import { LeaderboardRepository } from './leaderboard.repository';
import { UserRepository } from '../users/user.repository';
import { HabitRepository } from '../habits/habit.repository';
import { LeaderboardService } from './leaderboard.service';
import { getMonthStart, getWeekStart } from './utils/week.utils';

const mockLeaderboardRepository = {
  findWeek: jest.fn(),
  findWeeksByWeekStart: jest.fn(),
  recordCompletion: jest.fn(),
  awardPerfectWeekBonus: jest.fn(),
  getMonthStandings: jest.fn(),
  archiveMonth: jest.fn(),
  findArchive: jest.fn(),
};

const mockUserRepository = {
  findUserById: jest.fn(),
  findUserEmailsByIds: jest.fn(),
};

const mockHabitRepository = {
  findByUserId: jest.fn(),
};

const USER_ID = 'user-123';

describe('LeaderboardService', () => {
  let service: LeaderboardService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        LeaderboardService,
        { provide: LeaderboardRepository, useValue: mockLeaderboardRepository },
        { provide: UserRepository, useValue: mockUserRepository },
        { provide: HabitRepository, useValue: mockHabitRepository },
      ],
    }).compile();

    service = module.get<LeaderboardService>(LeaderboardService);
    jest.clearAllMocks();
  });

  describe('recordCompletion()', () => {
    it('computes today\'s roster from coreGoals + dailyVariations + active habits', async () => {
      mockUserRepository.findUserById.mockResolvedValue({
        coreGoals: [{ id: '1' }, { id: '2' }],
        dailyVariations: [{ id: '3' }],
      });
      mockHabitRepository.findByUserId.mockResolvedValue([{ id: 'h1' }]);
      mockLeaderboardRepository.findWeek.mockResolvedValue(null);

      await service.recordCompletion(USER_ID);

      const today = new Date().toISOString().split('T')[0];
      expect(mockLeaderboardRepository.recordCompletion).toHaveBeenCalledWith(
        expect.objectContaining({
          userId: USER_ID,
          weekStart: getWeekStart(today),
          monthStart: getMonthStart(today),
          pointsDelta: 100,
          day: expect.objectContaining({ date: today, completedCount: 1, rosterSize: 4 }),
        }),
      );
    });

    it('carries forward an existing day\'s progress instead of starting over', async () => {
      const today = new Date().toISOString().split('T')[0];
      mockUserRepository.findUserById.mockResolvedValue({ coreGoals: [{ id: '1' }], dailyVariations: [] });
      mockHabitRepository.findByUserId.mockResolvedValue([]);
      mockLeaderboardRepository.findWeek.mockResolvedValue({
        userId: USER_ID,
        weekStart: getWeekStart(today),
        monthStart: getMonthStart(today),
        weekPoints: 100,
        perfectWeekBonusAwarded: false,
        days: [{ date: today, completedCount: 1, rosterSize: 1, halfBonusAwarded: true, allBonusAwarded: false }],
      });

      await service.recordCompletion(USER_ID);

      expect(mockLeaderboardRepository.recordCompletion).toHaveBeenCalledWith(
        expect.objectContaining({
          day: expect.objectContaining({ completedCount: 2, halfBonusAwarded: true }),
        }),
      );
    });
  });

  describe('getCurrentStandings()', () => {
    it('attaches rank and display name to each standing', async () => {
      mockLeaderboardRepository.getMonthStandings.mockResolvedValue([
        { userId: 'u1', points: 500 },
        { userId: 'u2', points: 300 },
      ]);
      mockUserRepository.findUserEmailsByIds.mockResolvedValue([
        { id: 'u1', email: 'alex@habitflow.ai', firstName: 'Alex', lastName: 'Morgan' },
        { id: 'u2', email: 'ben@habitflow.ai', firstName: '', lastName: '' },
      ]);

      const result = await service.getCurrentStandings();

      expect(result).toEqual([
        { userId: 'u1', points: 500, rank: 1, username: 'Alex Morgan' },
        { userId: 'u2', points: 300, rank: 2, username: 'ben' },
      ]);
    });

    it('returns an empty array without querying user names when there are no standings', async () => {
      mockLeaderboardRepository.getMonthStandings.mockResolvedValue([]);

      const result = await service.getCurrentStandings();

      expect(result).toEqual([]);
      expect(mockUserRepository.findUserEmailsByIds).not.toHaveBeenCalled();
    });
  });

  describe('closeOutWeek()', () => {
    it('awards the perfect-week bonus only to users who hit the "all" tier every day', async () => {
      const perfectDay = (date: string) => ({ date, completedCount: 1, rosterSize: 1, halfBonusAwarded: true, allBonusAwarded: true });
      mockLeaderboardRepository.findWeeksByWeekStart.mockResolvedValue([
        {
          userId: 'perfect-user',
          weekStart: '2026-08-10',
          monthStart: '2026-08-01',
          perfectWeekBonusAwarded: false,
          days: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map(perfectDay),
        },
        {
          userId: 'incomplete-user',
          weekStart: '2026-08-10',
          monthStart: '2026-08-01',
          perfectWeekBonusAwarded: false,
          days: [perfectDay('Mon'), perfectDay('Tue')],
        },
      ]);

      await service.closeOutWeek('2026-08-10');

      expect(mockLeaderboardRepository.awardPerfectWeekBonus).toHaveBeenCalledTimes(1);
      expect(mockLeaderboardRepository.awardPerfectWeekBonus).toHaveBeenCalledWith('perfect-user', '2026-08-10', '2026-08-01', 1000);
    });

    it('does not re-award an already-awarded perfect week bonus', async () => {
      const perfectDay = (date: string) => ({ date, completedCount: 1, rosterSize: 1, halfBonusAwarded: true, allBonusAwarded: true });
      mockLeaderboardRepository.findWeeksByWeekStart.mockResolvedValue([
        {
          userId: 'perfect-user',
          weekStart: '2026-08-10',
          monthStart: '2026-08-01',
          perfectWeekBonusAwarded: true,
          days: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map(perfectDay),
        },
      ]);

      await service.closeOutWeek('2026-08-10');

      expect(mockLeaderboardRepository.awardPerfectWeekBonus).not.toHaveBeenCalled();
    });
  });

  describe('closeOutMonth()', () => {
    it('ranks by points descending and assigns medals', async () => {
      mockLeaderboardRepository.getMonthStandings.mockResolvedValue([
        { userId: 'u2', points: 300 },
        { userId: 'u1', points: 900 },
        { userId: 'u3', points: 100 },
      ]);

      await service.closeOutMonth('2026-07-01');

      expect(mockLeaderboardRepository.archiveMonth).toHaveBeenCalledWith('2026-07-01', [
        { userId: 'u1', points: 900, rank: 1, medal: 'gold' },
        { userId: 'u2', points: 300, rank: 2, medal: 'silver' },
        { userId: 'u3', points: 100, rank: 3, medal: 'bronze' },
      ]);
    });
  });
});

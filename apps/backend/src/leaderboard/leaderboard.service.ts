import { Injectable } from '@nestjs/common';
import { ArchiveData, LeaderboardRepository, StandingData } from './leaderboard.repository';
import { UserRepository } from '../users/user.repository';
import { HabitRepository } from '../habits/habit.repository';
import { getMonthStart, getWeekStart } from './utils/week.utils';
import { isPerfectWeek, medalForRank, scoreCompletion } from './utils/scoring.utils';

export interface RankedStanding extends StandingData {
  rank: number;
  username: string;
}

@Injectable()
export class LeaderboardService {
  constructor(
    private readonly leaderboardRepository: LeaderboardRepository,
    private readonly userRepository: UserRepository,
    private readonly habitRepository: HabitRepository,
  ) {}

  /** Call once per genuinely new completion (caller de-dupes already-completed items). */
  async recordCompletion(userId: string): Promise<void> {
    const today = new Date().toISOString().split('T')[0];
    const weekStart = getWeekStart(today);
    const monthStart = getMonthStart(today);

    const [rosterSize, week] = await Promise.all([
      this.getTodaysRosterSize(userId),
      this.leaderboardRepository.findWeek(userId, weekStart),
    ]);
    const existingDay = week?.days.find(d => d.date === today);

    const { pointsDelta, day } = scoreCompletion(today, rosterSize, existingDay);

    await this.leaderboardRepository.recordCompletion({ userId, weekStart, monthStart, pointsDelta, day });
  }

  async getCurrentStandings(limit = 50): Promise<RankedStanding[]> {
    const monthStart = getMonthStart(new Date().toISOString().split('T')[0]);
    const standings = await this.leaderboardRepository.getMonthStandings(monthStart, limit);
    return this.attachRankAndNames(standings);
  }

  async getArchive(monthStart?: string): Promise<ArchiveData[]> {
    return this.leaderboardRepository.findArchive(monthStart);
  }

  /** Weekly cron: award the perfect-week bonus for the week that just ended. */
  async closeOutWeek(weekStart: string): Promise<void> {
    const weeks = await this.leaderboardRepository.findWeeksByWeekStart(weekStart);
    for (const week of weeks) {
      if (!week.perfectWeekBonusAwarded && isPerfectWeek(week.days)) {
        await this.leaderboardRepository.awardPerfectWeekBonus(week.userId, week.weekStart, week.monthStart, 1000);
      }
    }
  }

  /** Monthly cron: rank the month that just ended, award medals, archive. */
  async closeOutMonth(monthStart: string): Promise<void> {
    const standings = await this.leaderboardRepository.getMonthStandings(monthStart);
    const sorted = [...standings].sort((a, b) => b.points - a.points);
    const archived = sorted.map((s, index) => ({
      ...s,
      rank: index + 1,
      medal: medalForRank(index + 1),
    }));
    await this.leaderboardRepository.archiveMonth(monthStart, archived);
  }

  private async getTodaysRosterSize(userId: string): Promise<number> {
    const [user, habits] = await Promise.all([
      this.userRepository.findUserById(userId),
      this.habitRepository.findByUserId(userId),
    ]);
    const taskCount = (user?.coreGoals.length ?? 0) + (user?.dailyVariations.length ?? 0);
    return taskCount + habits.length;
  }

  private async attachRankAndNames(standings: StandingData[]): Promise<RankedStanding[]> {
    if (standings.length === 0) return [];
    const users = await this.userRepository.findUserEmailsByIds(standings.map(s => s.userId));
    const nameById = new Map(
      users.map(u => [u.id, [u.firstName, u.lastName].filter(Boolean).join(' ') || u.email.split('@')[0]]),
    );
    return standings.map((s, index) => ({
      ...s,
      rank: index + 1,
      username: nameById.get(s.userId) ?? 'Unknown',
    }));
  }
}

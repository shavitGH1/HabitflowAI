import { Injectable } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { logger } from '../logger';
import { LeaderboardService } from './leaderboard.service';
import { getPreviousMonthStart, getPreviousWeekStart } from './utils/week.utils';

@Injectable()
export class LeaderboardScheduler {
  constructor(private readonly leaderboardService: LeaderboardService) {}

  @Cron('0 0 * * 1', { name: 'weekly-leaderboard-close-out' })
  async handleWeeklyCloseOut(): Promise<void> {
    const today = new Date().toISOString().split('T')[0];
    const weekJustEnded = getPreviousWeekStart(today);
    logger.info({ weekStart: weekJustEnded }, 'weekly leaderboard close-out cron triggered');
    await this.leaderboardService.closeOutWeek(weekJustEnded);
  }

  @Cron('0 0 1 * *', { name: 'monthly-leaderboard-archive' })
  async handleMonthlyArchive(): Promise<void> {
    const today = new Date().toISOString().split('T')[0];
    const monthJustEnded = getPreviousMonthStart(today);
    logger.info({ monthStart: monthJustEnded }, 'monthly leaderboard archive cron triggered');
    await this.leaderboardService.closeOutMonth(monthJustEnded);
  }
}

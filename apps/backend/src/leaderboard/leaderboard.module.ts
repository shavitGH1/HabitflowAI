import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { LeaderboardWeek, LeaderboardWeekSchema } from './schemas/leaderboard-week.schema';
import { LeaderboardMonth, LeaderboardMonthSchema } from './schemas/leaderboard-month.schema';
import { LeaderboardArchive, LeaderboardArchiveSchema } from './schemas/leaderboard-archive.schema';
import { LeaderboardRepository } from './leaderboard.repository';
import { LeaderboardService } from './leaderboard.service';
import { LeaderboardController } from './leaderboard.controller';
import { LeaderboardScheduler } from './leaderboard.scheduler';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: LeaderboardWeek.name, schema: LeaderboardWeekSchema },
      { name: LeaderboardMonth.name, schema: LeaderboardMonthSchema },
      { name: LeaderboardArchive.name, schema: LeaderboardArchiveSchema },
    ]),
    AuthModule,
    DatabaseModule,
  ],
  providers: [LeaderboardRepository, LeaderboardService, LeaderboardScheduler],
  controllers: [LeaderboardController],
  exports: [LeaderboardService],
})
export class LeaderboardModule {}

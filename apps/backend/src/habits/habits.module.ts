import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { GoalsModule } from '../goals/goals.module';
import { AiModule } from '../ai/ai.module';
import { LeaderboardModule } from '../leaderboard/leaderboard.module';
import { HabitsService } from './habits.service';
import { HabitsController } from './habits.controller';

@Module({
  imports: [DatabaseModule, AuthModule, GoalsModule, AiModule, LeaderboardModule],
  providers: [HabitsService],
  controllers: [HabitsController],
  exports: [DatabaseModule],
})
export class HabitsModule {}

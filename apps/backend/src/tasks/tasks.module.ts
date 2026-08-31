import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { GoalsModule } from '../goals/goals.module';
import { LeaderboardModule } from '../leaderboard/leaderboard.module';
import { AiModule } from '../ai/ai.module';
import { TasksController } from './tasks.controller';
import { TasksService } from './tasks.service';

@Module({
  imports: [AuthModule, DatabaseModule, LeaderboardModule, GoalsModule, AiModule],
  providers: [TasksService],
  controllers: [TasksController],
})
export class TasksModule {}

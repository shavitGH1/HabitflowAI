import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { LeaderboardModule } from '../leaderboard/leaderboard.module';
import { TasksController } from './tasks.controller';
import { TasksService } from './tasks.service';

@Module({
  imports: [AuthModule, DatabaseModule, LeaderboardModule],
  providers: [TasksService],
  controllers: [TasksController],
})
export class TasksModule {}

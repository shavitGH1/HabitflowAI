import { Module } from '@nestjs/common';
import { AiModule } from '../ai/ai.module';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { HabitsModule } from '../habits/habits.module';
import { InsightsController } from './insights.controller';
import { InsightsService } from './insights.service';

@Module({
  imports: [AuthModule, DatabaseModule, AiModule, HabitsModule],
  providers: [InsightsService],
  controllers: [InsightsController],
})
export class InsightsModule {}

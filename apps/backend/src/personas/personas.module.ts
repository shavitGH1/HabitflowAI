import { Module } from '@nestjs/common';
import { AiModule } from '../ai/ai.module';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { NotificationsModule } from '../notifications/notifications.module';
import { HabitsModule } from '../habits/habits.module';
import { GoalsModule } from '../goals/goals.module';
import { PersonaDriftScheduler } from './persona-drift.scheduler';
import { PersonasController } from './personas.controller';
import { PersonasService } from './personas.service';

@Module({
  imports: [AuthModule, DatabaseModule, AiModule, NotificationsModule, HabitsModule, GoalsModule],
  providers: [PersonasService, PersonaDriftScheduler],
  controllers: [PersonasController],
  exports: [PersonasService],
})
export class PersonasModule {}

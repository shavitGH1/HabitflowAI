import { Module } from '@nestjs/common';
import { AiModule } from '../ai/ai.module';
import { AuthModule } from '../auth/auth.module';
import { ChatModule } from '../chat/chat.module';
import { DatabaseModule } from '../database/database.module';
import { HabitsModule } from '../habits/habits.module';
import { PersonasModule } from '../personas/personas.module';
import { CoachController } from './coach.controller';
import { CoachService } from './coach.service';

@Module({
  imports: [AuthModule, DatabaseModule, AiModule, ChatModule, HabitsModule, PersonasModule],
  providers: [CoachService],
  controllers: [CoachController],
})
export class CoachModule {}

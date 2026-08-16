import { Module } from '@nestjs/common';
import { AiModule } from '../ai/ai.module';
import { ArticlesModule } from '../articles/articles.module';
import { AuthModule } from '../auth/auth.module';
import { ChatModule } from '../chat/chat.module';
import { DatabaseModule } from '../database/database.module';
import { GoalsModule } from '../goals/goals.module';
import { HabitsModule } from '../habits/habits.module';
import { PersonasModule } from '../personas/personas.module';
import { ResearchChunksModule } from '../research-chunks/research-chunks.module';
import { CoachAgent } from './coach.agent';
import { CoachController } from './coach.controller';
import { CoachService } from './coach.service';
import { CoachToolset } from './coach.toolset';

@Module({
  imports: [
    AuthModule,
    DatabaseModule,
    AiModule,
    ChatModule,
    HabitsModule,
    GoalsModule,
    PersonasModule,
    ArticlesModule,
    ResearchChunksModule,
  ],
  providers: [CoachService, CoachAgent, CoachToolset],
  controllers: [CoachController],
})
export class CoachModule {}

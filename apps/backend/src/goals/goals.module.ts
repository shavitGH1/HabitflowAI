import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { AiModule } from '../ai/ai.module';
import { Goal, GoalSchema } from './schemas/goal.schema';
import { GoalRepository } from './goal.repository';
import { GoalsService } from './goals.service';
import { GoalsController } from './goals.controller';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Goal.name, schema: GoalSchema }]),
    AuthModule,
    DatabaseModule,
    AiModule,
  ],
  providers: [GoalRepository, GoalsService],
  controllers: [GoalsController],
  exports: [GoalRepository, GoalsService],
})
export class GoalsModule {}

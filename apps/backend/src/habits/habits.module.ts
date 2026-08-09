import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from '../auth/auth.module';
import { GoalsModule } from '../goals/goals.module';
import { AiModule } from '../ai/ai.module';
import { Habit, HabitSchema } from './schemas/habit.schema';
import { HabitRepository } from './habit.repository';
import { HabitsService } from './habits.service';
import { HabitsController } from './habits.controller';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Habit.name, schema: HabitSchema }]),
    AuthModule,
    GoalsModule,
    AiModule,
  ],
  providers: [HabitRepository, HabitsService],
  controllers: [HabitsController],
  exports: [HabitRepository],
})
export class HabitsModule {}

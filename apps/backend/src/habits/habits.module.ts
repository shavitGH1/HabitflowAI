import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from '../auth/auth.module';
import { Habit, HabitSchema } from './schemas/habit.schema';
import { HabitRepository } from './habit.repository';
import { HabitsService } from './habits.service';
import { HabitsController } from './habits.controller';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Habit.name, schema: HabitSchema }]),
    AuthModule,
  ],
  providers: [HabitRepository, HabitsService],
  controllers: [HabitsController],
})
export class HabitsModule {}

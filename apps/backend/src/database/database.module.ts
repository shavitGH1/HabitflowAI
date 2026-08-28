import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { User, UserSchema } from '../users/schemas/user.schema';
import { UserRepository } from '../users/user.repository';
import { Habit, HabitSchema } from '../habits/schemas/habit.schema';
import { HabitRepository } from '../habits/habit.repository';
import { Goal, GoalSchema } from '../goals/schemas/goal.schema';
import { GoalRepository } from '../goals/goal.repository';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: User.name, schema: UserSchema },
      { name: Habit.name, schema: HabitSchema },
      { name: Goal.name, schema: GoalSchema },
    ]),
  ],
  providers: [UserRepository, HabitRepository, GoalRepository],
  exports: [UserRepository, HabitRepository, GoalRepository],
})
export class DatabaseModule {}

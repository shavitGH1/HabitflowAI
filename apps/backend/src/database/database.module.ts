import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { User, UserSchema } from '../users/schemas/user.schema';
import { UserRepository } from '../users/user.repository';
import { Habit, HabitSchema } from '../habits/schemas/habit.schema';
import { HabitRepository } from '../habits/habit.repository';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: User.name, schema: UserSchema },
      { name: Habit.name, schema: HabitSchema },
    ]),
  ],
  providers: [UserRepository, HabitRepository],
  exports: [UserRepository, HabitRepository],
})
export class DatabaseModule {}

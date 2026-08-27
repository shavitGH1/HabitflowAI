import { Module } from '@nestjs/common';
import { AiModule } from '../ai/ai.module';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { GoalsModule } from '../goals/goals.module';
import { StorageModule } from '../storage/storage.module';
import { UsersController } from './users.controller';
import { UsersService } from './users.service';

@Module({
  imports: [AuthModule, DatabaseModule, AiModule, StorageModule, GoalsModule],
  providers: [UsersService],
  controllers: [UsersController],
})
export class UsersModule {}

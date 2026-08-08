import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { Follow, FollowSchema } from './schemas/follow.schema';
import { FollowRepository } from './follow.repository';
import { FollowsService } from './follows.service';
import { FollowsController } from './follows.controller';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Follow.name, schema: FollowSchema }]),
    AuthModule,
    DatabaseModule,
  ],
  providers: [FollowRepository, FollowsService],
  controllers: [FollowsController],
  exports: [FollowRepository],
})
export class FollowsModule {}

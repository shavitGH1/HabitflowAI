import { Module } from '@nestjs/common';
import { AiModule } from '../ai/ai.module';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { InsightsController } from './insights.controller';
import { InsightsService } from './insights.service';

@Module({
  imports: [AuthModule, DatabaseModule, AiModule],
  providers: [InsightsService],
  controllers: [InsightsController],
})
export class InsightsModule {}

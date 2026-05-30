import { Module } from '@nestjs/common';
import { PassportModule } from '@nestjs/passport';
import { AiModule } from '../ai/ai.module';
import { DatabaseModule } from '../database/database.module';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { JwtAuthGuard } from './guards/jwt-auth.guard';
import { JwtStrategy } from './strategies/jwt.strategy';

@Module({
  imports: [PassportModule, DatabaseModule, AiModule],
  providers: [AuthService, JwtStrategy, JwtAuthGuard],
  controllers: [AuthController],
  exports: [JwtAuthGuard, PassportModule, JwtStrategy],
})
export class AuthModule {}

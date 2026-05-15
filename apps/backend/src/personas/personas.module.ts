import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { PersonasController } from './personas.controller';
import { PersonasService } from './personas.service';

@Module({
  imports: [AuthModule, DatabaseModule],
  providers: [PersonasService],
  controllers: [PersonasController],
})
export class PersonasModule {}

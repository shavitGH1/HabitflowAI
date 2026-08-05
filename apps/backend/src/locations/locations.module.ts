import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from '../auth/auth.module';
import { LocationRecord, LocationRecordSchema } from './schemas/location.schema';
import { LocationRepository } from './location.repository';
import { LocationsController } from './locations.controller';
import { LocationsService } from './locations.service';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: LocationRecord.name, schema: LocationRecordSchema }]),
    AuthModule,
  ],
  providers: [LocationRepository, LocationsService],
  controllers: [LocationsController],
})
export class LocationsModule {}

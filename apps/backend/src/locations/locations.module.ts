import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { FollowsModule } from '../follows/follows.module';
import { LocationRecord, LocationRecordSchema } from './schemas/location.schema';
import { LocationRepository } from './location.repository';
import { GeocodingService } from './geocoding.service';
import { LocationsController } from './locations.controller';
import { LocationsService } from './locations.service';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: LocationRecord.name, schema: LocationRecordSchema }]),
    AuthModule,
    DatabaseModule,
    FollowsModule,
  ],
  providers: [LocationRepository, GeocodingService, LocationsService],
  controllers: [LocationsController],
})
export class LocationsModule {}

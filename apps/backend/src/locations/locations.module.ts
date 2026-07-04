import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { LocationsController } from './locations.controller';
import { LocationsService } from './locations.service';
import { LocationRecord, LocationRecordSchema } from './schemas/location.schema';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: LocationRecord.name, schema: LocationRecordSchema }]),
  ],
  controllers: [LocationsController],
  providers: [LocationsService],
})
export class LocationsModule {}

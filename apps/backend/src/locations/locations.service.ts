import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { LocationRecord, LocationRecordDocument } from './schemas/location.schema';
import { LocationDto } from './dto/location.dto';

@Injectable()
export class LocationsService {
  constructor(
    @InjectModel(LocationRecord.name)
    private readonly locationModel: Model<LocationRecordDocument>,
  ) {}

  async recordLocation(userId: string, dto: LocationDto) {
    const record = new this.locationModel({
      userId,
      habitId: dto.habitId,
      latitude: dto.latitude,
      longitude: dto.longitude,
      timestamp: dto.timestamp ?? Date.now(),
    });
    await record.save();
    return { success: true };
  }
}

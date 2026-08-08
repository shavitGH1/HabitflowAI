import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { LocationRecord, LocationRecordDocument } from './schemas/location.schema';

export interface LocationData {
  id: string;
  userId: string;
  habitId?: string;
  taskTitle?: string;
  placeName?: string;
  address?: string;
  latitude: number;
  longitude: number;
  timestamp: number;
  personaType: string;
  isPublic: boolean;
  createdAt: string;
}

export interface CreateLocationInput {
  userId: string;
  habitId?: string;
  taskTitle?: string;
  latitude: number;
  longitude: number;
  timestamp?: number;
  personaType?: string;
  isPublic?: boolean;
}

@Injectable()
export class LocationRepository {
  constructor(
    @InjectModel(LocationRecord.name)
    private readonly locationModel: Model<LocationRecordDocument>,
  ) {}

  async create(input: CreateLocationInput): Promise<LocationData> {
    const doc = await new this.locationModel(input).save();
    return this.toLocationData(doc);
  }

  async findByUser(userId: string): Promise<LocationData[]> {
    const docs = await this.locationModel
      .find({ userId })
      .sort({ timestamp: -1 })
      .exec();
    return docs.map(doc => this.toLocationData(doc));
  }

  async updatePlaceName(id: string, placeName?: string, address?: string): Promise<void> {
    await this.locationModel.updateOne({ _id: id }, { $set: { placeName, address } });
  }

  async findByBbox(
    minLat: number,
    maxLat: number,
    minLng: number,
    maxLng: number,
  ): Promise<LocationData[]> {
    const docs = await this.locationModel
      .find({
        isPublic: true,
        latitude: { $gte: minLat, $lte: maxLat },
        longitude: { $gte: minLng, $lte: maxLng },
      })
      .sort({ createdAt: -1 })
      .exec();
    return docs.map(doc => this.toLocationData(doc));
  }

  private toLocationData(doc: LocationRecordDocument): LocationData {
    return {
      id: (doc._id as { toString(): string }).toString(),
      userId: doc.userId,
      habitId: doc.habitId,
      taskTitle: doc.taskTitle,
      placeName: doc.placeName,
      address: doc.address,
      latitude: doc.latitude,
      longitude: doc.longitude,
      timestamp: doc.timestamp,
      personaType: doc.personaType,
      isPublic: doc.isPublic,
      createdAt: (doc as unknown as { createdAt: Date }).createdAt.toISOString(),
    };
  }
}

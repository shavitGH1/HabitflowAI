import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { LocationRecord, LocationRecordDocument } from './schemas/location.schema';

export type LocationScope = 'all' | 'friends' | 'mine';
export type LocationRelationship = 'mine' | 'friend' | 'stranger';

export interface LocationData {
  id: string;
  userId: string;
  habitId?: string;
  latitude: number;
  longitude: number;
  placeName: string;
  taskDescription: string;
  timestamp: number;
  personaType: string;
  isPublic: boolean;
  type: 'habit' | 'task';
  username?: string;
  relationship?: LocationRelationship;
  createdAt: string;
}

export interface BboxScopeOptions {
  sinceTimestamp?: number;
  scope: LocationScope;
  requestingUserId: string;
  followingIds: string[];
}

export interface CreateLocationInput {
  userId: string;
  habitId?: string;
  latitude: number;
  longitude: number;
  placeName?: string;
  taskDescription?: string;
  timestamp?: number;
  personaType?: string;
  isPublic?: boolean;
  type?: 'habit' | 'task';
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

  async findByBbox(
    minLat: number,
    maxLat: number,
    minLng: number,
    maxLng: number,
    options: BboxScopeOptions,
  ): Promise<LocationData[]> {
    const query: Record<string, unknown> = {
      latitude: { $gte: minLat, $lte: maxLat },
      longitude: { $gte: minLng, $lte: maxLng },
    };

    if (options.sinceTimestamp !== undefined) {
      query.timestamp = { $gte: options.sinceTimestamp };
    }

    if (options.scope === 'mine') {
      // Always see your own pins, public or not.
      query.userId = options.requestingUserId;
    } else {
      query.isPublic = true;
      if (options.scope === 'friends') {
        query.userId = { $in: options.followingIds };
      }
    }

    const docs = await this.locationModel.find(query).sort({ createdAt: -1 }).exec();
    return docs.map(doc => this.toLocationData(doc));
  }

  async findByUser(userId: string, limit = 500): Promise<LocationData[]> {
    const docs = await this.locationModel
      .find({ userId })
      .sort({ createdAt: -1 })
      .limit(limit)
      .exec();
    return docs.map(doc => this.toLocationData(doc));
  }

  private toLocationData(doc: LocationRecordDocument): LocationData {
    return {
      id: (doc._id as { toString(): string }).toString(),
      userId: doc.userId,
      habitId: doc.habitId,
      latitude: doc.latitude,
      longitude: doc.longitude,
      placeName: doc.placeName ?? '',
      taskDescription: doc.taskDescription ?? '',
      timestamp: doc.timestamp,
      personaType: doc.personaType,
      isPublic: doc.isPublic,
      type: (doc.type === 'habit' ? 'habit' : 'task'),
      createdAt: (doc as unknown as { createdAt: Date }).createdAt.toISOString(),
    };
  }
}

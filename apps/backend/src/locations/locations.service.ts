import { Injectable } from '@nestjs/common';
import { logger } from '../logger';
import { LocationDto } from './dto/location.dto';
import { LocationData, LocationRepository, LocationScope } from './location.repository';
import { GeocodingService } from './geocoding.service';
import { UserRepository } from '../users/user.repository';
import { HabitRepository } from '../habits/habit.repository';
import { FollowRepository } from '../follows/follow.repository';

@Injectable()
export class LocationsService {
  constructor(
    private readonly locationRepository: LocationRepository,
    private readonly geocoding: GeocodingService,
    private readonly userRepository: UserRepository,
    private readonly habitRepository: HabitRepository,
    private readonly followRepository: FollowRepository,
  ) {}

  async recordLocation(userId: string, dto: LocationDto): Promise<{ success: boolean }> {
    logger.info(
      { userId, habitId: dto.habitId, latitude: dto.latitude, longitude: dto.longitude },
      'recordLocation: request received',
    );

    const placeName = dto.placeName ?? (await this.geocoding.reverseGeocode(dto.latitude, dto.longitude)) ?? '';
    const taskDescription =
      dto.taskDescription ?? (dto.habitId ? await this.resolveTaskDescription(userId, dto.habitId) : '');

    const created = await this.locationRepository.create({
      userId,
      habitId: dto.habitId,
      latitude: dto.latitude,
      longitude: dto.longitude,
      placeName,
      taskDescription,
      timestamp: dto.timestamp ?? Date.now(),
      personaType: dto.personaType,
      isPublic: dto.isPublic,
      type: dto.type ?? 'task',
    });

    logger.info(
      { id: created.id, placeName, taskDescription, success: true },
      'recordLocation: saved',
    );
    return { success: true };
  }

  async getMyLocations(userId: string): Promise<LocationData[]> {
    const records = await this.locationRepository.findByUser(userId);
    logger.info({ userId, count: records.length }, 'getMyLocations: returning records');
    return records;
  }

  async getBbox(
    minLat: number,
    maxLat: number,
    minLng: number,
    maxLng: number,
    requestingUserId: string,
    scope: LocationScope = 'all',
    sinceTimestamp?: number,
  ): Promise<LocationData[]> {
    const followingIds = await this.followRepository.findFollowingIds(requestingUserId);
    if (scope === 'friends' && followingIds.length === 0) return [];

    const records = await this.locationRepository.findByBbox(minLat, maxLat, minLng, maxLng, {
      sinceTimestamp,
      scope,
      requestingUserId,
      followingIds,
    });
    return this.attachUsernamesAndRelationship(records, requestingUserId, followingIds);
  }

  private async attachUsernamesAndRelationship(
    records: LocationData[],
    requestingUserId: string,
    followingIds: string[],
  ): Promise<LocationData[]> {
    if (records.length === 0) return records;
    const ids = [...new Set(records.map(r => r.userId))];
    const users = await this.userRepository.findUserEmailsByIds(ids);
    const usernameById = new Map(
      users.map(u => [u.id, [u.firstName, u.lastName].filter(Boolean).join(' ') || u.email.split('@')[0]]),
    );
    const followingSet = new Set(followingIds);
    return records.map(r => ({
      ...r,
      username: usernameById.get(r.userId),
      relationship: r.userId === requestingUserId ? 'mine' : followingSet.has(r.userId) ? 'friend' : 'stranger',
    }));
  }

  private async resolveTaskDescription(userId: string, taskId: string): Promise<string> {
    try {
      const user = await this.userRepository.findUserById(userId);
      if (!user) return '';
      const task =
        user.coreGoals.find(t => t.id === taskId) ?? user.dailyVariations.find(t => t.id === taskId);
      if (task) return task.description;

      // taskId can also be a real Habit id (Habits tab) rather than an
      // onboarding-generated coreGoals/dailyVariations task — those live in
      // a separate collection, not on the User document.
      const habit = await this.habitRepository.findById(taskId);
      if (habit && habit.userId === userId) return habit.title;

      return '';
    } catch {
      return '';
    }
  }
}

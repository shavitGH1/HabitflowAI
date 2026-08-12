import { Injectable } from '@nestjs/common';
import { LocationDto } from './dto/location.dto';
import { LocationData, LocationRepository } from './location.repository';
import { GoogleGeocodingService } from './geocoding.service';
import { UserRepository } from '../users/user.repository';

@Injectable()
export class LocationsService {
  constructor(
    private readonly locationRepository: LocationRepository,
    private readonly geocodingService: GoogleGeocodingService,
    private readonly userRepository: UserRepository,
  ) {}

  async recordLocation(userId: string, dto: LocationDto): Promise<{ success: boolean }> {
    const created = await this.locationRepository.create({
      userId,
      habitId: dto.habitId,
      taskTitle: dto.taskTitle,
      latitude: dto.latitude,
      longitude: dto.longitude,
      timestamp: dto.timestamp ?? Date.now(),
      personaType: dto.personaType,
      isPublic: dto.isPublic,
    });

    const { placeName, address } = await this.geocodingService.reverseGeocode(
      dto.latitude,
      dto.longitude,
    );
    if (placeName || address) {
      await this.locationRepository.updatePlaceName(created.id, placeName, address);
    }

    return { success: true };
  }

  async getMyLocations(userId: string): Promise<LocationData[]> {
    const [locations, user] = await Promise.all([
      this.locationRepository.findByUser(userId),
      this.userRepository.findUserById(userId),
    ]);

    const taskTitles = new Map<string, string>();
    for (const goal of user?.coreGoals ?? []) taskTitles.set(goal.id, goal.description);
    for (const goal of user?.dailyVariations ?? []) taskTitles.set(goal.id, goal.description);

    return locations.map(loc => ({
      ...loc,
      taskTitle: loc.habitId ? taskTitles.get(loc.habitId) ?? loc.taskTitle : loc.taskTitle,
    }));
  }

  async getBbox(
    minLat: number,
    maxLat: number,
    minLng: number,
    maxLng: number,
  ): Promise<LocationData[]> {
    return this.locationRepository.findByBbox(minLat, maxLat, minLng, maxLng);
  }
}

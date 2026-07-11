import { Injectable } from '@nestjs/common';
import { LocationDto } from './dto/location.dto';
import { LocationData, LocationRepository } from './location.repository';

@Injectable()
export class LocationsService {
  constructor(private readonly locationRepository: LocationRepository) {}

  async recordLocation(userId: string, dto: LocationDto): Promise<{ success: boolean }> {
    await this.locationRepository.create({
      userId,
      habitId: dto.habitId,
      latitude: dto.latitude,
      longitude: dto.longitude,
      timestamp: dto.timestamp ?? Date.now(),
      personaType: dto.personaType,
      isPublic: dto.isPublic,
    });
    return { success: true };
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

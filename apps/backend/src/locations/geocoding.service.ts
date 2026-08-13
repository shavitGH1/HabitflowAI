import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { logger } from '../logger';

@Injectable()
export class GeocodingService {
  constructor(private readonly config: ConfigService) {}

  async reverseGeocode(latitude: number, longitude: number): Promise<string | null> {
    const apiKey = this.config.get<string>('GOOGLE_MAPS_API_KEY');
    if (!apiKey) {
      logger.warn({ latitude, longitude }, 'reverseGeocode: GOOGLE_MAPS_API_KEY not set');
      return null;
    }

    const url =
      'https://maps.googleapis.com/maps/api/geocode/json' +
      `?latlng=${latitude},${longitude}` +
      `&key=${encodeURIComponent(apiKey)}`;

    try {
      const res = await fetch(url);
      if (!res.ok) {
        logger.warn({ latitude, longitude, status: res.status }, 'reverseGeocode: HTTP error');
        return null;
      }
      const data = (await res.json()) as {
        status: string;
        results?: { formatted_address?: string }[];
      };
      if (data.status !== 'OK' || !data.results?.length) {
        logger.warn({ latitude, longitude, status: data.status }, 'reverseGeocode: no result');
        return null;
      }
      const place = data.results[0].formatted_address ?? null;
      logger.info({ latitude, longitude, place }, 'reverseGeocode: ok');
      return place;
    } catch (err) {
      logger.warn({ latitude, longitude, err: String(err) }, 'reverseGeocode: failed');
      return null;
    }
  }
}

import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

export interface GeocodeResult {
  placeName?: string;
  address?: string;
}

@Injectable()
export class GoogleGeocodingService {
  constructor(private readonly configService: ConfigService) {}

  async reverseGeocode(latitude: number, longitude: number): Promise<GeocodeResult> {
    const apiKey = this.configService.get<string>('GOOGLE_MAPS_API_KEY');
    if (!apiKey) return {};

    try {
      const url =
        `https://maps.googleapis.com/maps/api/geocode/json` +
        `?latlng=${latitude},${longitude}&language=en&key=${apiKey}`;
      const res = await fetch(url);
      if (!res.ok) return {};

      const data = (await res.json()) as {
        status: string;
        results: Array<{ formatted_address: string }>;
      };

      if (data.status !== 'OK' || data.results.length === 0) return {};

      const formattedAddress = data.results[0].formatted_address;
      return { placeName: formattedAddress, address: formattedAddress };
    } catch {
      return {};
    }
  }
}

import { GoogleGeocodingService } from './geocoding.service';

describe('GoogleGeocodingService', () => {
  let service: GoogleGeocodingService;

  const config = (apiKey?: string) =>
    ({ get: (key: string) => (key === 'GOOGLE_MAPS_API_KEY' ? apiKey : undefined) }) as never;

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('returns empty result when no API key is configured', async () => {
    service = new GoogleGeocodingService(config(undefined));
    await expect(service.reverseGeocode(32.0853, 34.7818)).resolves.toEqual({});
  });

  it('returns formatted address on a successful response', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () =>
        Promise.resolve({
          status: 'OK',
          results: [{ formatted_address: 'Dizengoff St, Tel Aviv-Yafo, Israel' }],
        }),
    }) as unknown as typeof fetch;

    service = new GoogleGeocodingService(config('test-key'));
    const result = await service.reverseGeocode(32.0853, 34.7818);
    expect(result).toEqual({
      placeName: 'Dizengoff St, Tel Aviv-Yafo, Israel',
      address: 'Dizengoff St, Tel Aviv-Yafo, Israel',
    });
  });

  it('returns empty result on a non-OK geocoding status', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ status: 'ZERO_RESULTS', results: [] }),
    }) as unknown as typeof fetch;

    service = new GoogleGeocodingService(config('test-key'));
    await expect(service.reverseGeocode(32.0853, 34.7818)).resolves.toEqual({});
  });

  it('returns empty result when the HTTP request fails', async () => {
    global.fetch = jest.fn().mockRejectedValue(new Error('network down')) as unknown as typeof fetch;

    service = new GoogleGeocodingService(config('test-key'));
    await expect(service.reverseGeocode(32.0853, 34.7818)).resolves.toEqual({});
  });
});

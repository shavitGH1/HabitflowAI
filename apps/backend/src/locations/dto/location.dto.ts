import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsNotEmpty, IsNumber, IsOptional, IsString } from 'class-validator';

export class LocationDto {
  @IsOptional()
  @IsString()
  @ApiPropertyOptional({ description: 'The related habit/task ID' })
  habitId?: string;

  @IsNumber()
  @IsNotEmpty()
  @ApiProperty({ description: 'Latitude' })
  latitude: number;

  @IsNumber()
  @IsNotEmpty()
  @ApiProperty({ description: 'Longitude' })
  longitude: number;

  @IsOptional()
  @IsNumber()
  @ApiPropertyOptional({ description: 'Unix timestamp in ms' })
  timestamp?: number;
}

import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsBoolean, IsNotEmpty, IsNumber, IsOptional, IsString } from 'class-validator';

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

  @IsOptional()
  @IsString()
  @ApiPropertyOptional({ description: "User's persona type at time of completion" })
  personaType?: string;

  @IsOptional()
  @IsBoolean()
  @ApiPropertyOptional({ description: 'Whether this location is visible on the public map', default: true })
  isPublic?: boolean;
}

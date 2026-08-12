import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsBoolean, IsNumber, IsOptional, IsString, Max, Min } from 'class-validator';

export class LocationDto {
  @IsOptional()
  @IsString()
  @ApiPropertyOptional({ description: 'The related habit/task ID' })
  habitId?: string;

  @IsOptional()
  @IsString()
  @ApiPropertyOptional({ description: 'Display name of the completed task' })
  taskTitle?: string;

  @IsNumber()
  @Min(-90)
  @Max(90)
  @ApiProperty({ description: 'Latitude' })
  latitude: number;

  @IsNumber()
  @Min(-180)
  @Max(180)
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
  @ApiPropertyOptional({ description: 'Whether this location is visible on the public map', default: false })
  isPublic?: boolean;
}

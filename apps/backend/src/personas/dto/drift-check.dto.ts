import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsArray, IsInt, IsNumber, IsOptional, IsString, Max, Min } from 'class-validator';

export class DriftCheckDto {
  @IsOptional()
  @IsNumber()
  @Min(0)
  @Max(1)
  @ApiPropertyOptional({ description: 'Override completion rate (0-1)', example: 0.4 })
  recentCompletionRate?: number;

  @IsOptional()
  @IsInt()
  @Min(0)
  @ApiPropertyOptional({ description: 'Override active streak in days', example: 2 })
  activeStreak?: number;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  @ApiPropertyOptional({ type: [String], example: ['Morning run', 'Read 10 pages'] })
  completedHabits?: string[];

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  @ApiPropertyOptional({ type: [String], example: ['Meditate'] })
  skippedHabits?: string[];
}

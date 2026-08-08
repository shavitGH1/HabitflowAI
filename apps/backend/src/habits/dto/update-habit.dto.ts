import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsEnum, IsInt, IsMongoId, IsOptional, IsString, Min } from 'class-validator';

export class UpdateHabitDto {
  @IsOptional()
  @IsString()
  @ApiPropertyOptional({ example: 'Evening Run' })
  title?: string;

  @IsOptional()
  @IsString()
  @ApiPropertyOptional({ example: 'Run 5km every evening after work' })
  description?: string;

  @IsOptional()
  @IsEnum(['daily', 'weekly'])
  @ApiPropertyOptional({ enum: ['daily', 'weekly'] })
  frequency?: 'daily' | 'weekly';

  @IsOptional()
  @IsInt()
  @Min(1)
  @ApiPropertyOptional({ example: 1, minimum: 1 })
  targetCount?: number;

  @IsOptional()
  @IsMongoId()
  @ApiPropertyOptional({
    example: '64f1a2b3c4d5e6f7a8b9c0d1',
    description:
      'Link to a different active goal (subject to the per-goal cap), or pass null to ' +
      'unlink and make this habit standalone (subject to the standalone cap). Omit this ' +
      'field entirely to leave the current link untouched.',
    nullable: true,
  })
  goalId?: string | null;
}

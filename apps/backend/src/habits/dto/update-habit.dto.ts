import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsEnum, IsInt, IsOptional, IsString, Min } from 'class-validator';

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
}

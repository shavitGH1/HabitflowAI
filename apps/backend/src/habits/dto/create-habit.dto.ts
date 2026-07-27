import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsEnum, IsInt, IsNotEmpty, IsOptional, IsString, Min } from 'class-validator';

export class CreateHabitDto {
  @IsString()
  @IsNotEmpty()
  @ApiProperty({ example: 'Morning Run' })
  title: string;

  @IsOptional()
  @IsString()
  @ApiPropertyOptional({ example: 'Run 5km every morning before work' })
  description?: string;

  @IsEnum(['daily', 'weekly'])
  @ApiProperty({ enum: ['daily', 'weekly'], example: 'daily' })
  frequency: 'daily' | 'weekly';

  @IsOptional()
  @IsInt()
  @Min(1)
  @ApiPropertyOptional({ example: 1, minimum: 1 })
  targetCount?: number;
}

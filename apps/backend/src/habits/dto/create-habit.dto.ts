import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsEnum, IsInt, IsMongoId, IsNotEmpty, IsOptional, IsString, Min } from 'class-validator';

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

  @IsOptional()
  @IsMongoId()
  @ApiPropertyOptional({
    example: '64f1a2b3c4d5e6f7a8b9c0d1',
    description: 'Link this habit to your active goal (max 3 linked habits per goal). Omit for a standalone habit (max 2 per user).',
  })
  goalId?: string;
}

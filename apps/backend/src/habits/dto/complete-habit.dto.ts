import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString, MaxLength } from 'class-validator';

export class CompleteHabitDto {
  @IsOptional()
  @IsString()
  @MaxLength(200)
  @ApiPropertyOptional({
    example: 'Ran 5km along the river this morning',
    description: 'What did you do? Checked for plausibility against the habit, never blocks completion.',
    maxLength: 200,
  })
  note?: string;
}

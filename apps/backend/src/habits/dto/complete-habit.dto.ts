import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString, Matches, MaxLength } from 'class-validator';

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

  @IsOptional()
  @Matches(/^\d{4}-\d{2}-\d{2}$/, { message: 'date must be in YYYY-MM-DD format' })
  @ApiPropertyOptional({
    example: '2026-08-27',
    description: "The client's local calendar date for this completion. Falls back to the server's " +
      'UTC date when omitted, which can misfile a completion under the wrong day for users ahead of UTC.',
  })
  date?: string;
}

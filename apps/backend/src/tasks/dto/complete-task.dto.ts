import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString, Matches, MaxLength } from 'class-validator';

export class CompleteTaskDto {
  @IsOptional()
  @Matches(/^\d{4}-\d{2}-\d{2}$/, { message: 'date must be in YYYY-MM-DD format' })
  @ApiPropertyOptional({
    example: '2026-08-27',
    description: "The client's local calendar date for this completion, used when a goal-genre task " +
      "auto-completes its linked habit(s). Falls back to the server's UTC date when omitted.",
  })
  date?: string;

  @IsOptional()
  @IsString()
  @MaxLength(200)
  @ApiPropertyOptional({
    example: 'Ran 5km along the river this morning',
    description: 'Optional "what did you do?" note - checked for plausibility against the task description.',
  })
  note?: string;
}

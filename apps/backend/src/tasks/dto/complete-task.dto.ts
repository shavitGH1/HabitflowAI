import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, Matches } from 'class-validator';

export class CompleteTaskDto {
  @IsOptional()
  @Matches(/^\d{4}-\d{2}-\d{2}$/, { message: 'date must be in YYYY-MM-DD format' })
  @ApiPropertyOptional({
    example: '2026-08-27',
    description: "The client's local calendar date for this completion, used when a goal-genre task " +
      "auto-completes its linked habit(s). Falls back to the server's UTC date when omitted.",
  })
  date?: string;
}

import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsArray, IsNotEmpty, IsOptional, IsString } from 'class-validator';

export class OnboardingSuggestionsDto {
  @IsString()
  @IsNotEmpty()
  @ApiProperty({ example: 'Run a marathon' })
  goal: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  @ApiPropertyOptional({
    type: [String],
    description:
      "Answers given so far in the same quiz, one per question in order (empty string for " +
      'not-yet-answered). When provided, suggestions are only generated for the remaining ' +
      "questions, informed by what's already been answered. Omit for the initial goal-only fetch.",
  })
  answeredSoFar?: string[];
}

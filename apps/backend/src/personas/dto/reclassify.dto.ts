import { ApiProperty } from '@nestjs/swagger';
import { ArrayMaxSize, ArrayMinSize, IsArray, IsNotEmpty, IsString } from 'class-validator';
import { ONBOARDING_QUESTIONS } from '../../ai/pillars';

export class ReclassifyDto {
  @IsString()
  @IsNotEmpty()
  @ApiProperty({
    example: 'Run a marathon',
    description: "The user's stated goal — authoritative, drives persona classification and task generation.",
  })
  goal: string;

  @IsArray()
  @IsString({ each: true })
  @IsNotEmpty({ each: true })
  @ArrayMinSize(ONBOARDING_QUESTIONS.length)
  @ArrayMaxSize(ONBOARDING_QUESTIONS.length)
  @ApiProperty({
    type: [String],
    minItems: ONBOARDING_QUESTIONS.length,
    maxItems: ONBOARDING_QUESTIONS.length,
    description: `One answer per onboarding background question (${ONBOARDING_QUESTIONS.length} required). Background/persona signal only — not the goal.`,
    example: [
      'I stuck with Duolingo for 3 months because of daily streaks',
      'Yes — I studied with friends and we kept each other accountable',
      'I switch up locations or topics to keep it fresh',
      'I want to make my family proud and set a good example',
      'I prefer a fixed schedule: same time, same place every day',
      'I set a goal to read more last year and tracked it weekly',
    ],
  })
  openAnswers: string[];
}

import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { ArrayMaxSize, ArrayMinSize, IsArray, IsNotEmpty, IsOptional, IsString } from 'class-validator';
import { ONBOARDING_QUESTIONS } from '../../ai/pillars';

export class GoogleRegisterDto {
  @IsString()
  @IsNotEmpty()
  @ApiProperty({ description: 'Short-lived token returned by GET /auth/google/callback for a new (isNewUser) account' })
  signupToken: string;

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
  })
  openAnswers: string[];

  @IsOptional()
  @IsString()
  @ApiPropertyOptional({ description: 'FCM push notification token' })
  fcmToken?: string;
}

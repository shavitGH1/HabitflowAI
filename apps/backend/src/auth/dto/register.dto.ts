import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { ArrayMaxSize, ArrayMinSize, IsArray, IsEmail, IsNotEmpty, IsOptional, IsString, MinLength } from 'class-validator';
import { ONBOARDING_QUESTIONS } from '../../ai/pillars';

export class RegisterDto {
  @IsEmail()
  @ApiProperty({ example: 'user@example.com' })
  email: string;

  @IsOptional()
  @IsString()
  @ApiPropertyOptional({ example: 'Alex' })
  firstName?: string;

  @IsOptional()
  @IsString()
  @ApiPropertyOptional({ example: 'Morgan' })
  lastName?: string;

  @IsString()
  @MinLength(6)
  @ApiProperty({ minLength: 6 })
  password: string;

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

  @IsOptional()
  @IsString()
  @ApiPropertyOptional({ description: 'FCM push notification token' })
  fcmToken?: string;
}

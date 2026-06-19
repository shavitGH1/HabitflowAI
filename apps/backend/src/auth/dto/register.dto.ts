import { ApiProperty } from '@nestjs/swagger';
import { ArrayMaxSize, ArrayMinSize, IsArray, IsEmail, IsNotEmpty, IsString, MinLength } from 'class-validator';
import { ONBOARDING_QUESTIONS } from '../../ai/pillars';

export class RegisterDto {
  @IsEmail()
  @ApiProperty({ example: 'user@example.com' })
  email: string;

  @IsString()
  @MinLength(6)
  @ApiProperty({ minLength: 6 })
  password: string;

  @IsArray()
  @IsString({ each: true })
  @IsNotEmpty({ each: true })
  @ArrayMinSize(ONBOARDING_QUESTIONS.length)
  @ArrayMaxSize(ONBOARDING_QUESTIONS.length)
  @ApiProperty({
    type: [String],
    minItems: ONBOARDING_QUESTIONS.length,
    maxItems: ONBOARDING_QUESTIONS.length,
    description: `One answer per onboarding question (${ONBOARDING_QUESTIONS.length} required). First answer is treated as the user's primary goal.`,
    example: [
      'I want to build a consistent study routine',
      'I stuck with Duolingo for 3 months because of daily streaks',
      'Yes — I studied with friends and we kept each other accountable',
      'I switch up locations or topics to keep it fresh',
      'I want to make my family proud and set a good example',
      'I prefer a fixed schedule: same time, same place every day',
    ],
  })
  quizAnswers: string[];
}

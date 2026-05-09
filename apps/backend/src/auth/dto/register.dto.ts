import { ApiProperty } from '@nestjs/swagger';
import { IsArray, IsEmail, IsString, MinLength } from 'class-validator';

export class RegisterDto {
  @IsEmail()
  @ApiProperty({ example: 'user@example.com' })
  email: string;

  @IsString()
  @MinLength(6)
  @ApiProperty({ minLength: 6 })
  password: string;

  @IsString()
  @ApiProperty({ example: 'Run a 5k every day' })
  goal: string;

  @IsArray()
  @IsString({ each: true })
  @ApiProperty({ type: [String], example: ['A', 'B', 'C'] })
  quizAnswers: string[];
}

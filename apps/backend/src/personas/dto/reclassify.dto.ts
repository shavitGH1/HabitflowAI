import { ApiProperty } from '@nestjs/swagger';
import { IsArray, IsString } from 'class-validator';

export class ReclassifyDto {
  @IsString()
  @ApiProperty({ example: 'Run a 5k every day' })
  goal: string;

  @IsArray()
  @IsString({ each: true })
  @ApiProperty({ type: [String], example: ['A', 'B', 'C'] })
  quizAnswers: string[];
}

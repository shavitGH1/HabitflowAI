import { ApiProperty } from '@nestjs/swagger';
import { IsDateString, IsNotEmpty, IsString, MaxLength } from 'class-validator';

export class CreateGoalDto {
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  @ApiProperty({ example: 'Run a marathon' })
  title: string;

  @IsDateString()
  @ApiProperty({ example: '2026-12-31' })
  targetDate: string;
}

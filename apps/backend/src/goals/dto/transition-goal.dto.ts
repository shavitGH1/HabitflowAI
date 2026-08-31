import { ApiProperty } from '@nestjs/swagger';
import { IsDateString, IsEnum, IsNotEmpty, IsString, MaxLength } from 'class-validator';

export class TransitionGoalDto {
  @IsEnum(['achieve', 'forfeit'])
  @ApiProperty({ enum: ['achieve', 'forfeit'], example: 'achieve' })
  resolution: 'achieve' | 'forfeit';

  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  @ApiProperty({ example: 'Run 20km' })
  newGoalTitle: string;

  @IsDateString()
  @ApiProperty({ example: '2026-12-31' })
  newGoalTargetDate: string;
}

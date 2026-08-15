import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString } from 'class-validator';

export class OnboardingSuggestionsDto {
  @IsString()
  @IsNotEmpty()
  @ApiProperty({ example: 'Run a marathon' })
  goal: string;
}

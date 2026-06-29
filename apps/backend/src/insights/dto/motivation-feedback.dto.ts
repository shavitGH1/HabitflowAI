import { ApiProperty } from '@nestjs/swagger';
import { IsIn } from 'class-validator';

export class MotivationFeedbackDto {
  @IsIn(['up', 'down'])
  @ApiProperty({ enum: ['up', 'down'], example: 'up' })
  vote: 'up' | 'down';
}

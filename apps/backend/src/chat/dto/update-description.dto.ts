import { ApiProperty } from '@nestjs/swagger';
import { IsString, MaxLength } from 'class-validator';

export class UpdateDescriptionDto {
  @IsString()
  @MaxLength(300)
  @ApiProperty({ example: 'A place to plan our weekend runs', required: false })
  description: string;
}

import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString } from 'class-validator';

export class TargetUserDto {
  @IsString()
  @IsNotEmpty()
  @ApiProperty({ description: 'The target user ID', example: '64f1a2b3c4d5e6f7a8b9c0d1' })
  userId: string;
}

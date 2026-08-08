import { ApiProperty } from '@nestjs/swagger';
import { ArrayNotEmpty, IsArray, IsString } from 'class-validator';

export class AddMembersDto {
  @IsArray()
  @ArrayNotEmpty()
  @IsString({ each: true })
  @ApiProperty({ description: 'User IDs to add to the group', example: ['64f1a2b3c4d5e6f7a8b9c0d1'] })
  userIds: string[];
}

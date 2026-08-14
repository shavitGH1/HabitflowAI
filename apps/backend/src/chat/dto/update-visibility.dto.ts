import { ApiProperty } from '@nestjs/swagger';
import { IsBoolean } from 'class-validator';

export class UpdateVisibilityDto {
  @IsBoolean()
  @ApiProperty({ example: true, description: 'True to make the group publicly visible, false for private' })
  isPublic: boolean;
}

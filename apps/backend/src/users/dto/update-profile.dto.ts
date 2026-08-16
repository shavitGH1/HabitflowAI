import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString } from 'class-validator';

export class UpdateProfileDto {
  @IsOptional()
  @IsString()
  @ApiPropertyOptional({
    example: 'preset:1',
    description:
      'Profile picture: a preset avatar key (e.g. "preset:1") or a host-relative /uploads URL',
  })
  profilePicture?: string;
}

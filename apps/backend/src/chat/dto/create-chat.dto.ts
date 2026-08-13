import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { ArrayNotEmpty, IsArray, IsBoolean, IsOptional, IsString } from 'class-validator';

export class CreateChatDto {
  @IsArray()
  @ArrayNotEmpty()
  @IsString({ each: true })
  @ApiProperty({
    description: 'User IDs of the other participant(s) — the requester is added automatically',
    example: ['64f1a2b3c4d5e6f7a8b9c0d1'],
  })
  participantIds: string[];

  @IsOptional()
  @IsBoolean()
  @ApiPropertyOptional({ default: false, description: 'True for a group chat, false/omitted for a direct 1:1 chat' })
  isGroup?: boolean;

  @IsOptional()
  @IsBoolean()
  @ApiPropertyOptional({
    default: false,
    description: 'Group chats only: true for a publicly visible group, false/omitted for private',
  })
  isPublic?: boolean;

  @IsOptional()
  @IsString()
  @ApiPropertyOptional({ example: 'Marathon Crew' })
  name?: string;
}

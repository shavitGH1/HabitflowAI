import { IsMongoId, IsOptional, IsString } from 'class-validator';

export class SendMessageDto {
  @IsMongoId()
  chatId: string;

  @IsOptional()
  @IsString()
  text?: string;

  @IsOptional()
  @IsString()
  imageUrl?: string;
}

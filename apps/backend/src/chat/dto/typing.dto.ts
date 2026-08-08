import { IsBoolean, IsMongoId } from 'class-validator';

export class TypingDto {
  @IsMongoId()
  chatId: string;

  @IsBoolean()
  isTyping: boolean;
}

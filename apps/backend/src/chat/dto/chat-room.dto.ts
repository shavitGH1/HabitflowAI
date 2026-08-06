import { IsMongoId } from 'class-validator';

export class ChatRoomDto {
  @IsMongoId()
  chatId: string;
}

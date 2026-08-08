import { CanActivate, ExecutionContext, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { ChatData, ChatRepository } from '../chat.repository';

export interface ChatMemberRequest {
  params: { chatId: string };
  user: { id: string };
  chat: ChatData;
}

@Injectable()
export class ChatMemberGuard implements CanActivate {
  constructor(private readonly chatRepository: ChatRepository) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<ChatMemberRequest>();
    const { chatId } = request.params;
    const userId = request.user.id;

    const chat = await this.chatRepository.findById(chatId);
    if (!chat) throw new NotFoundException('Chat not found');
    if (!chat.participantIds.includes(userId)) {
      throw new ForbiddenException('You are not a participant in this chat');
    }

    request.chat = chat;
    return true;
  }
}

import { CanActivate, ExecutionContext, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { ChatRepository } from '../chat.repository';

interface AdminGuardRequest {
  params: { chatId: string };
  user: { id: string };
}

@Injectable()
export class ChatAdminGuard implements CanActivate {
  constructor(private readonly chatRepository: ChatRepository) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<AdminGuardRequest>();
    const { chatId } = request.params;
    const userId = request.user.id;

    const chat = await this.chatRepository.findById(chatId);
    if (!chat) throw new NotFoundException('Chat not found');
    if (!chat.isGroup) throw new ForbiddenException('This action is only available for group chats');
    if (!chat.admins.includes(userId)) {
      throw new ForbiddenException('Only group admins can perform this action');
    }

    return true;
  }
}

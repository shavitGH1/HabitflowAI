import { BadRequestException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { ChatData, ChatRepository, MessageData } from './chat.repository';
import { UserRepository } from '../users/user.repository';
import { CreateChatDto } from './dto/create-chat.dto';

@Injectable()
export class ChatService {
  constructor(
    private readonly chatRepository: ChatRepository,
    private readonly userRepository: UserRepository,
  ) {}

  async createChat(requesterId: string, dto: CreateChatDto): Promise<ChatData> {
    const participantIds = Array.from(new Set([requesterId, ...dto.participantIds]));

    for (const id of participantIds) {
      if (id === requesterId) continue;
      const user = await this.userRepository.findUserById(id);
      if (!user) throw new BadRequestException(`No user found for participant id ${id}`);
    }

    const isGroup = dto.isGroup ?? false;
    if (!isGroup) {
      if (participantIds.length !== 2) {
        throw new BadRequestException('A direct chat requires exactly one other participant');
      }
      const [a, b] = participantIds;
      const existing = await this.chatRepository.findDirectChatBetween(a, b);
      if (existing) return existing;
    }

    return this.chatRepository.createChat({ participantIds, isGroup, name: dto.name });
  }

  async getChatsForUser(userId: string): Promise<ChatData[]> {
    return this.chatRepository.findByParticipantId(userId);
  }

  async getMessages(userId: string, chatId: string, page: number, limit: number): Promise<MessageData[]> {
    await this.assertParticipant(userId, chatId);
    return this.chatRepository.findMessagesPaginated(chatId, page, limit);
  }

  async postMessage(userId: string, chatId: string, text?: string, imageUrl?: string): Promise<MessageData> {
    await this.assertParticipant(userId, chatId);
    if (!text && !imageUrl) {
      throw new BadRequestException('Message must have text or an image');
    }
    return this.chatRepository.addMessage({ chatId, senderId: userId, text, imageUrl });
  }

  async assertParticipant(userId: string, chatId: string): Promise<ChatData> {
    const chat = await this.chatRepository.findById(chatId);
    if (!chat) throw new NotFoundException('Chat not found');
    if (!chat.participantIds.includes(userId)) {
      throw new ForbiddenException('You are not a participant in this chat');
    }
    return chat;
  }
}

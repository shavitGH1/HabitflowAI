import { BadRequestException, ForbiddenException, Inject, Injectable, NotFoundException } from '@nestjs/common';
import { ChatData, ChatRepository, MessageData } from './chat.repository';
import { UserRepository } from '../users/user.repository';
import { IStorageAdapter, STORAGE_ADAPTER } from '../storage/storage.adapter';
import { CreateChatDto } from './dto/create-chat.dto';

const MAX_PINNED_CHATS = 3;

@Injectable()
export class ChatService {
  constructor(
    private readonly chatRepository: ChatRepository,
    private readonly userRepository: UserRepository,
    @Inject(STORAGE_ADAPTER) private readonly storage: IStorageAdapter,
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
      return this.chatRepository.createChat({ participantIds, isGroup, name: dto.name });
    }

    return this.chatRepository.createChat({
      participantIds,
      isGroup,
      isPublic: dto.isPublic ?? false,
      name: dto.name,
      owner: requesterId,
      admins: [requesterId],
    });
  }

  async getChatsForUser(userId: string): Promise<ChatData[]> {
    return this.chatRepository.findByParticipantId(userId);
  }

  async getMessages(chatId: string, page: number, limit: number): Promise<MessageData[]> {
    return this.chatRepository.findMessagesPaginated(chatId, page, limit);
  }

  async postMessage(userId: string, chatId: string, text?: string, imageUrl?: string): Promise<MessageData> {
    const chat = await this.assertParticipant(userId, chatId);
    if (!text && !imageUrl) {
      throw new BadRequestException('Message must have text or an image');
    }

    const message = await this.chatRepository.addMessage({ chatId, senderId: userId, text, imageUrl });

    const unreadCount = { ...chat.unreadCount };
    for (const participantId of chat.participantIds) {
      if (participantId === userId) continue;
      unreadCount[participantId] = (unreadCount[participantId] ?? 0) + 1;
    }
    const lastMessage = text && text.length > 0 ? text : imageUrl ? '📷 Photo' : '';
    await this.chatRepository.updateChat(chatId, {
      lastMessage,
      lastMessageAt: new Date(),
      lastMessageSenderId: userId,
      unreadCount,
    });

    return message;
  }

  async markAsRead(userId: string, chat: ChatData): Promise<ChatData> {
    const unreadCount = { ...chat.unreadCount, [userId]: 0 };
    return (await this.chatRepository.updateChat(chat.id, { unreadCount }))!;
  }

  async togglePin(userId: string, chat: ChatData): Promise<ChatData> {
    const isPinned = chat.pinnedBy.includes(userId);
    if (!isPinned) {
      const userChats = await this.chatRepository.findByParticipantId(userId);
      const pinnedCount = userChats.filter(c => c.pinnedBy.includes(userId)).length;
      if (pinnedCount >= MAX_PINNED_CHATS) {
        throw new BadRequestException(`You can only pin up to ${MAX_PINNED_CHATS} chats`);
      }
    }
    const pinnedBy = isPinned ? chat.pinnedBy.filter(id => id !== userId) : [...chat.pinnedBy, userId];
    return (await this.chatRepository.updateChat(chat.id, { pinnedBy }))!;
  }

  async toggleMute(userId: string, chat: ChatData): Promise<ChatData> {
    const isMuted = chat.mutedBy.includes(userId);
    const mutedBy = isMuted ? chat.mutedBy.filter(id => id !== userId) : [...chat.mutedBy, userId];
    return (await this.chatRepository.updateChat(chat.id, { mutedBy }))!;
  }

  async toggleMessageLike(userId: string, chatId: string, messageId: string): Promise<MessageData> {
    const message = await this.chatRepository.findMessageById(chatId, messageId);
    if (!message) throw new NotFoundException('Message not found');

    const likes = message.likes.includes(userId)
      ? message.likes.filter(id => id !== userId)
      : [...message.likes, userId];

    return this.chatRepository.setMessageLikes(messageId, likes);
  }

  async assertParticipant(userId: string, chatId: string): Promise<ChatData> {
    const chat = await this.chatRepository.findById(chatId);
    if (!chat) throw new NotFoundException('Chat not found');
    if (!chat.participantIds.includes(userId)) {
      throw new ForbiddenException('You are not a participant in this chat');
    }
    return chat;
  }

  async addMembers(chatId: string, userIds: string[]): Promise<ChatData> {
    const chat = await this.getGroupChat(chatId);

    for (const id of userIds) {
      const user = await this.userRepository.findUserById(id);
      if (!user) throw new BadRequestException(`No user found for participant id ${id}`);
    }

    const participantIds = Array.from(new Set([...chat.participantIds, ...userIds]));
    return (await this.chatRepository.updateChat(chatId, { participantIds }))!;
  }

  async removeMember(chatId: string, targetUserId: string): Promise<ChatData | null> {
    const chat = await this.getGroupChat(chatId);
    if (chat.owner === targetUserId) {
      throw new ForbiddenException('Cannot remove the group owner');
    }
    return this.removeParticipant(chat, targetUserId);
  }

  async leaveGroup(userId: string, chatId: string): Promise<ChatData | null> {
    const chat = await this.getGroupChat(chatId);
    if (!chat.participantIds.includes(userId)) {
      throw new ForbiddenException('You are not a participant in this chat');
    }
    return this.removeParticipant(chat, userId);
  }

  async renameGroup(chatId: string, name: string): Promise<ChatData> {
    await this.getGroupChat(chatId);
    return (await this.chatRepository.updateChat(chatId, { name }))!;
  }

  async updateDescription(chatId: string, description: string): Promise<ChatData> {
    await this.getGroupChat(chatId);
    return (await this.chatRepository.updateChat(chatId, { description }))!;
  }

  async updateVisibility(chatId: string, isPublic: boolean): Promise<ChatData> {
    await this.getGroupChat(chatId);
    return (await this.chatRepository.updateChat(chatId, { isPublic }))!;
  }

  async addAdmin(chatId: string, targetUserId: string): Promise<ChatData> {
    const chat = await this.getGroupChat(chatId);
    if (!chat.participantIds.includes(targetUserId)) {
      throw new BadRequestException('The target user is not a member of this group');
    }
    if (chat.admins.includes(targetUserId)) return chat;

    const admins = [...chat.admins, targetUserId];
    return (await this.chatRepository.updateChat(chatId, { admins }))!;
  }

  async removeAdmin(chatId: string, targetUserId: string): Promise<ChatData> {
    const chat = await this.getGroupChat(chatId);
    if (!chat.admins.includes(targetUserId)) {
      throw new BadRequestException('User is not an admin of this group');
    }
    if (chat.admins.length <= 1) {
      throw new BadRequestException('Cannot remove the last admin');
    }

    const admins = chat.admins.filter(id => id !== targetUserId);
    return (await this.chatRepository.updateChat(chatId, { admins }))!;
  }

  async uploadGroupImage(chatId: string, file: Express.Multer.File): Promise<ChatData> {
    await this.getGroupChat(chatId);
    const imageUrl = await this.storage.upload(file);
    return (await this.chatRepository.updateChat(chatId, { imageUrl }))!;
  }

  // Membership is already verified by ChatMemberGuard before this runs. Doesn't persist
  // anything — the returned URL is sent back via the sendMessage socket event, which
  // creates the actual Message document with this imageUrl attached.
  async uploadMessageImage(file: Express.Multer.File): Promise<string> {
    return this.storage.upload(file);
  }

  async deleteChat(chatId: string): Promise<void> {
    await this.chatRepository.deleteChat(chatId);
  }

  private async getGroupChat(chatId: string): Promise<ChatData> {
    const chat = await this.chatRepository.findById(chatId);
    if (!chat) throw new NotFoundException('Chat not found');
    if (!chat.isGroup) throw new BadRequestException('This action is only available for group chats');
    return chat;
  }

  private async removeParticipant(chat: ChatData, userId: string): Promise<ChatData | null> {
    const participantIds = chat.participantIds.filter(id => id !== userId);

    if (participantIds.length === 0) {
      await this.chatRepository.deleteChat(chat.id);
      return null;
    }

    let admins = chat.admins.filter(id => id !== userId);
    if (admins.length === 0) {
      admins = [participantIds[0]];
    }

    const owner = chat.owner === userId ? (admins[0] ?? participantIds[0]) : chat.owner;

    return this.chatRepository.updateChat(chat.id, { participantIds, admins, owner });
  }
}

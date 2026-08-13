import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, isValidObjectId } from 'mongoose';
import { Chat, ChatDocument } from './schemas/chat.schema';
import { Message, MessageDocument } from './schemas/message.schema';
import { logger } from '../logger';

export interface ChatData {
  id: string;
  participantIds: string[];
  isGroup: boolean;
  name?: string;
  admins: string[];
  owner?: string;
  description?: string;
  imageUrl?: string;
  lastMessage?: string;
  unreadCount: Record<string, number>;
  createdAt: string;
}

export interface MessageData {
  id: string;
  chatId: string;
  senderId: string;
  text?: string;
  imageUrl?: string;
  likes: string[];
  sentAt: string;
}

export interface CreateChatInput {
  participantIds: string[];
  isGroup: boolean;
  name?: string;
  admins?: string[];
  owner?: string;
}

export interface UpdateChatInput {
  name?: string;
  description?: string;
  imageUrl?: string;
  participantIds?: string[];
  admins?: string[];
  owner?: string;
  lastMessage?: string;
  unreadCount?: Record<string, number>;
}

export interface CreateMessageInput {
  chatId: string;
  senderId: string;
  text?: string;
  imageUrl?: string;
}

@Injectable()
export class ChatRepository {
  constructor(
    @InjectModel(Chat.name) private readonly chatModel: Model<ChatDocument>,
    @InjectModel(Message.name) private readonly messageModel: Model<MessageDocument>,
  ) {}

  async createChat(input: CreateChatInput): Promise<ChatData> {
    try {
      const doc = await this.chatModel.create(input);
      return this.toChatData(doc);
    } catch (error) {
      logger.error({ err: error }, 'DB error in createChat');
      throw error;
    }
  }

  async findDirectChatBetween(userIdA: string, userIdB: string): Promise<ChatData | null> {
    const doc = await this.chatModel.findOne({
      isGroup: false,
      participantIds: { $all: [userIdA, userIdB], $size: 2 },
    });
    return doc ? this.toChatData(doc) : null;
  }

  async findById(id: string): Promise<ChatData | null> {
    if (!isValidObjectId(id)) return null;
    const doc = await this.chatModel.findById(id);
    return doc ? this.toChatData(doc) : null;
  }

  async findByParticipantId(userId: string): Promise<ChatData[]> {
    const docs = await this.chatModel.find({ participantIds: userId }).sort({ updatedAt: -1 });
    return docs.map(d => this.toChatData(d));
  }

  async updateChat(chatId: string, updates: UpdateChatInput): Promise<ChatData | null> {
    if (!isValidObjectId(chatId)) return null;
    const doc = await this.chatModel.findByIdAndUpdate(chatId, updates, { returnDocument: 'after' });
    return doc ? this.toChatData(doc) : null;
  }

  async deleteChat(chatId: string): Promise<void> {
    if (!isValidObjectId(chatId)) return;
    await this.messageModel.deleteMany({ chatId });
    await this.chatModel.findByIdAndDelete(chatId);
  }

  async addMessage(input: CreateMessageInput): Promise<MessageData> {
    if (!isValidObjectId(input.chatId)) {
      throw new Error('Invalid chat id');
    }
    const doc = await this.messageModel.create(input);
    return this.toMessageData(doc);
  }

  async findMessagesPaginated(chatId: string, page: number, limit: number): Promise<MessageData[]> {
    if (!isValidObjectId(chatId)) return [];
    const docs = await this.messageModel
      .find({ chatId })
      .sort({ createdAt: -1 })
      .skip((page - 1) * limit)
      .limit(limit)
      .exec();
    return docs.map(d => this.toMessageData(d));
  }

  async findMessageById(chatId: string, messageId: string): Promise<MessageData | null> {
    if (!isValidObjectId(chatId) || !isValidObjectId(messageId)) return null;
    const doc = await this.messageModel.findOne({ _id: messageId, chatId });
    return doc ? this.toMessageData(doc) : null;
  }

  async setMessageLikes(messageId: string, likes: string[]): Promise<MessageData> {
    if (!isValidObjectId(messageId)) {
      throw new Error('Invalid message id');
    }
    const doc = await this.messageModel.findByIdAndUpdate(messageId, { likes }, { returnDocument: 'after' });
    return this.toMessageData(doc!);
  }

  private toChatData(doc: ChatDocument): ChatData {
    return {
      id: doc._id.toString(),
      participantIds: doc.participantIds,
      isGroup: doc.isGroup,
      name: doc.name,
      admins: doc.admins,
      owner: doc.owner,
      description: doc.description,
      imageUrl: doc.imageUrl,
      lastMessage: doc.lastMessage,
      unreadCount: Object.fromEntries(doc.unreadCount ?? new Map()),
      createdAt: (doc as unknown as { createdAt: Date }).createdAt?.toISOString() ?? '',
    };
  }

  private toMessageData(doc: MessageDocument): MessageData {
    return {
      id: doc._id.toString(),
      chatId: doc.chatId,
      senderId: doc.senderId,
      text: doc.text,
      imageUrl: doc.imageUrl,
      likes: doc.likes,
      sentAt: (doc as unknown as { createdAt: Date }).createdAt?.toISOString() ?? '',
    };
  }
}

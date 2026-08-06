import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
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
  createdAt: string;
}

export interface MessageData {
  id: string;
  chatId: string;
  senderId: string;
  text?: string;
  imageUrl?: string;
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
    const doc = await this.chatModel.findById(id);
    return doc ? this.toChatData(doc) : null;
  }

  async findByParticipantId(userId: string): Promise<ChatData[]> {
    const docs = await this.chatModel.find({ participantIds: userId }).sort({ createdAt: -1 });
    return docs.map(d => this.toChatData(d));
  }

  async updateChat(chatId: string, updates: UpdateChatInput): Promise<ChatData | null> {
    const doc = await this.chatModel.findByIdAndUpdate(chatId, updates, { returnDocument: 'after' });
    return doc ? this.toChatData(doc) : null;
  }

  async deleteChat(chatId: string): Promise<void> {
    await this.messageModel.deleteMany({ chatId });
    await this.chatModel.findByIdAndDelete(chatId);
  }

  async addMessage(input: CreateMessageInput): Promise<MessageData> {
    const doc = await this.messageModel.create(input);
    return this.toMessageData(doc);
  }

  async findMessagesPaginated(chatId: string, page: number, limit: number): Promise<MessageData[]> {
    const docs = await this.messageModel
      .find({ chatId })
      .sort({ createdAt: -1 })
      .skip((page - 1) * limit)
      .limit(limit)
      .exec();
    return docs.map(d => this.toMessageData(d));
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
      sentAt: (doc as unknown as { createdAt: Date }).createdAt?.toISOString() ?? '',
    };
  }
}

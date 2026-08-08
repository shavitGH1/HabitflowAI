import { UseFilters, UsePipes, ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import {
  ConnectedSocket,
  MessageBody,
  OnGatewayConnection,
  OnGatewayDisconnect,
  SubscribeMessage,
  WebSocketGateway,
  WebSocketServer,
} from '@nestjs/websockets';
import jwt from 'jsonwebtoken';
import { Server, Socket } from 'socket.io';
import { logger } from '../logger';
import { ChatService } from './chat.service';
import { ChatRoomDto } from './dto/chat-room.dto';
import { SendMessageDto } from './dto/send-message.dto';
import { TypingDto } from './dto/typing.dto';
import { ChatEvent } from './enums/chat-event.enum';
import { WsExceptionFilter } from './filters/ws-exception.filter';

const userRoom = (userId: string): string => `user:${userId}`;

@WebSocketGateway({ cors: { origin: '*' } })
@UsePipes(new ValidationPipe({ whitelist: true, transform: true }))
@UseFilters(new WsExceptionFilter())
export class ChatGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  constructor(
    private readonly chatService: ChatService,
    private readonly config: ConfigService,
  ) {}

  handleConnection(client: Socket): void {
    const userId = this.authenticate(client);
    if (!userId) {
      client.emit('error', { message: 'Unauthorized' });
      client.disconnect(true);
      return;
    }
    client.data.userId = userId;
    client.join(userRoom(userId));
    logger.info({ userId, socketId: client.id }, 'chat socket connected');
  }

  handleDisconnect(client: Socket): void {
    logger.info({ socketId: client.id }, 'chat socket disconnected');
  }

  @SubscribeMessage(ChatEvent.JOIN_CHAT)
  async joinChat(@ConnectedSocket() client: Socket, @MessageBody() dto: ChatRoomDto): Promise<void> {
    const userId = client.data.userId as string;
    await this.chatService.assertParticipant(userId, dto.chatId);
    await client.join(dto.chatId);
    client.to(dto.chatId).emit(ChatEvent.USER_JOINED, { chatId: dto.chatId, userId });
  }

  @SubscribeMessage(ChatEvent.LEAVE_CHAT)
  async leaveChat(@ConnectedSocket() client: Socket, @MessageBody() dto: ChatRoomDto): Promise<void> {
    const userId = client.data.userId as string;
    await client.leave(dto.chatId);
    client.to(dto.chatId).emit(ChatEvent.USER_LEFT, { chatId: dto.chatId, userId });
  }

  @SubscribeMessage(ChatEvent.SEND_MESSAGE)
  async sendMessage(@ConnectedSocket() client: Socket, @MessageBody() dto: SendMessageDto): Promise<void> {
    const userId = client.data.userId as string;
    const message = await this.chatService.postMessage(userId, dto.chatId, dto.text, dto.imageUrl);
    this.server.to(dto.chatId).emit(ChatEvent.NEW_MESSAGE, message);
  }

  @SubscribeMessage(ChatEvent.TYPING)
  handleTyping(@ConnectedSocket() client: Socket, @MessageBody() dto: TypingDto): void {
    const userId = client.data.userId as string;
    client.to(dto.chatId).emit(ChatEvent.TYPING, { userId, isTyping: dto.isTyping });
  }

  emitToRoom(chatId: string, event: string, payload: Record<string, unknown>): void {
    this.server.to(chatId).emit(event, payload);
  }

  emitToUser(userId: string, event: string, payload: Record<string, unknown>): void {
    this.server.to(userRoom(userId)).emit(event, payload);
  }

  private authenticate(client: Socket): string | null {
    const token = this.extractToken(client);
    if (!token) return null;

    try {
      const secret = this.config.get<string>('JWT_SECRET')!;
      const decoded = jwt.verify(token, secret) as { id: string };
      return decoded.id;
    } catch {
      return null;
    }
  }

  private extractToken(client: Socket): string | null {
    const authToken = client.handshake.auth?.['token'] as string | undefined;
    if (authToken) return authToken.replace(/^Bearer\s+/i, '');

    const header = client.handshake.headers?.authorization;
    if (header) return header.replace(/^Bearer\s+/i, '');

    return null;
  }
}

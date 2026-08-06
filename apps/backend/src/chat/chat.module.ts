import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { Chat, ChatSchema } from './schemas/chat.schema';
import { Message, MessageSchema } from './schemas/message.schema';
import { ChatRepository } from './chat.repository';
import { ChatService } from './chat.service';
import { ChatController } from './chat.controller';
import { ChatGateway } from './chat.gateway';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Chat.name, schema: ChatSchema },
      { name: Message.name, schema: MessageSchema },
    ]),
    AuthModule,
    DatabaseModule,
  ],
  providers: [ChatRepository, ChatService, ChatGateway],
  controllers: [ChatController],
})
export class ChatModule {}

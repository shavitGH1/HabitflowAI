import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { StorageModule } from '../storage/storage.module';
import { Chat, ChatSchema } from './schemas/chat.schema';
import { Message, MessageSchema } from './schemas/message.schema';
import { ChatRepository } from './chat.repository';
import { ChatService } from './chat.service';
import { ChatController } from './chat.controller';
import { ChatGateway } from './chat.gateway';
import { ChatAdminGuard } from './guards/chat-admin.guard';
import { ChatMemberGuard } from './guards/chat-member.guard';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Chat.name, schema: ChatSchema },
      { name: Message.name, schema: MessageSchema },
    ]),
    AuthModule,
    DatabaseModule,
    StorageModule,
  ],
  providers: [ChatRepository, ChatService, ChatGateway, ChatAdminGuard, ChatMemberGuard],
  controllers: [ChatController],
  exports: [ChatService, ChatGateway],
})
export class ChatModule {}

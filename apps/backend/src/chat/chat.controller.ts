import { Body, Controller, Get, Param, Post, Query, Req, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiQuery, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { ChatService } from './chat.service';
import { CreateChatDto } from './dto/create-chat.dto';

@ApiTags('chat')
@Controller('chats')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class ChatController {
  constructor(private readonly chatService: ChatService) {}

  @Post()
  @ApiOperation({
    summary: 'Create a direct or group chat',
    description: 'For a direct chat, returns the existing chat if one already exists between the two participants',
  })
  @ApiResponse({ status: 201, description: 'Chat created or found' })
  @ApiResponse({ status: 400, description: 'Invalid participant list' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  create(@Req() req: { user: { id: string } }, @Body() dto: CreateChatDto) {
    return this.chatService.createChat(req.user.id, dto);
  }

  @Get()
  @ApiOperation({ summary: "List the current user's chats" })
  @ApiResponse({ status: 200, description: 'Array of chats' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  findAll(@Req() req: { user: { id: string } }) {
    return this.chatService.getChatsForUser(req.user.id);
  }

  @Get(':chatId/messages')
  @ApiOperation({
    summary: 'Paginated message history for a chat, most recent first',
    description: 'Used for initial screen load and offline catch-up; live messages arrive over the WebSocket gateway',
  })
  @ApiQuery({ name: 'page', required: false, type: Number, example: 1 })
  @ApiQuery({ name: 'limit', required: false, type: Number, example: 30 })
  @ApiResponse({ status: 200, description: 'Array of messages' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'Not a participant in this chat' })
  @ApiResponse({ status: 404, description: 'Chat not found' })
  getMessages(
    @Req() req: { user: { id: string } },
    @Param('chatId') chatId: string,
    @Query('page') page = '1',
    @Query('limit') limit = '30',
  ) {
    return this.chatService.getMessages(req.user.id, chatId, Number(page), Number(limit));
  }
}

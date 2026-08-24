import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Query,
  Req,
  UploadedFile,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import {
  ApiBearerAuth,
  ApiBody,
  ApiConsumes,
  ApiOperation,
  ApiQuery,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { ChatGateway } from './chat.gateway';
import { ChatService } from './chat.service';
import { AddMembersDto } from './dto/add-members.dto';
import { CreateChatDto } from './dto/create-chat.dto';
import { RenameGroupDto } from './dto/rename-group.dto';
import { TargetUserDto } from './dto/target-user.dto';
import { UpdateDescriptionDto } from './dto/update-description.dto';
import { UpdateVisibilityDto } from './dto/update-visibility.dto';
import { ChatAdminGuard } from './guards/chat-admin.guard';
import { ChatMemberGuard, ChatMemberRequest } from './guards/chat-member.guard';

@ApiTags('chat')
@Controller('chats')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class ChatController {
  constructor(
    private readonly chatService: ChatService,
    private readonly chatGateway: ChatGateway,
  ) {}

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
  @UseGuards(ChatMemberGuard)
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
  getMessages(@Param('chatId') chatId: string, @Query('page') page = '1', @Query('limit') limit = '30') {
    return this.chatService.getMessages(chatId, Number(page), Number(limit));
  }

  @Post(':chatId/read')
  @UseGuards(ChatMemberGuard)
  @ApiOperation({ summary: "Mark a chat as read, resetting the caller's unread count to 0" })
  @ApiResponse({ status: 201, description: 'Marked as read' })
  @ApiResponse({ status: 403, description: 'Not a participant in this chat' })
  markAsRead(@Req() req: ChatMemberRequest) {
    return this.chatService.markAsRead(req.user.id, req.chat);
  }

  @Post(':chatId/pin')
  @UseGuards(ChatMemberGuard)
  @ApiOperation({ summary: 'Toggle pinning this chat to the top of the caller\'s own chat list (max 3 pinned)' })
  @ApiResponse({ status: 201, description: 'Pin toggled' })
  @ApiResponse({ status: 400, description: 'Already at the 3-chat pin limit' })
  @ApiResponse({ status: 403, description: 'Not a participant in this chat' })
  togglePin(@Req() req: ChatMemberRequest) {
    return this.chatService.togglePin(req.user.id, req.chat);
  }

  @Post(':chatId/mute')
  @UseGuards(ChatMemberGuard)
  @ApiOperation({ summary: "Toggle muting notifications for this chat, for the caller only" })
  @ApiResponse({ status: 201, description: 'Mute toggled' })
  @ApiResponse({ status: 403, description: 'Not a participant in this chat' })
  toggleMute(@Req() req: ChatMemberRequest) {
    return this.chatService.toggleMute(req.user.id, req.chat);
  }

  @Post(':chatId/messages/image')
  @UseGuards(ChatMemberGuard)
  @UseInterceptors(FileInterceptor('image'))
  @ApiConsumes('multipart/form-data')
  @ApiBody({ schema: { type: 'object', properties: { image: { type: 'string', format: 'binary' } } } })
  @ApiOperation({
    summary: 'Upload an image to attach to a chat message',
    description: 'Returns the URL only — send it via the sendMessage WebSocket event\'s imageUrl field to actually post the message',
  })
  @ApiResponse({ status: 201, description: '{ imageUrl, success }' })
  @ApiResponse({ status: 403, description: 'Not a participant in this chat' })
  async uploadMessageImage(@UploadedFile() file: Express.Multer.File) {
    const imageUrl = await this.chatService.uploadMessageImage(file);
    return { imageUrl, success: true };
  }

  @Post(':chatId/messages/:messageId/like')
  @UseGuards(ChatMemberGuard)
  @ApiOperation({ summary: 'Toggle a like on a message' })
  @ApiResponse({ status: 201, description: 'Like toggled' })
  @ApiResponse({ status: 403, description: 'Not a participant in this chat' })
  @ApiResponse({ status: 404, description: 'Message not found' })
  async toggleMessageLike(
    @Req() req: ChatMemberRequest,
    @Param('chatId') chatId: string,
    @Param('messageId') messageId: string,
  ) {
    const message = await this.chatService.toggleMessageLike(req.user.id, chatId, messageId);
    this.chatGateway.emitToRoom(chatId, 'messageLiked', { chatId, messageId, likes: message.likes });
    return message;
  }

  @Post(':chatId/members')
  @UseGuards(ChatAdminGuard)
  @ApiOperation({ summary: 'Add members to a group chat (admin only)' })
  @ApiResponse({ status: 201, description: 'Members added' })
  @ApiResponse({ status: 403, description: 'Not a group admin' })
  async addMembers(@Param('chatId') chatId: string, @Body() dto: AddMembersDto) {
    const chat = await this.chatService.addMembers(chatId, dto.userIds);
    this.chatGateway.emitToRoom(chatId, 'memberAdded', { chatId, userIds: dto.userIds });
    for (const userId of dto.userIds) {
      this.chatGateway.emitToUser(userId, 'memberAdded', { chatId, userIds: dto.userIds });
    }
    return chat;
  }

  @Delete(':chatId/members')
  @UseGuards(ChatAdminGuard)
  @ApiOperation({ summary: 'Remove a member from a group chat (admin only)' })
  @ApiResponse({ status: 200, description: 'Member removed' })
  @ApiResponse({ status: 403, description: 'Not a group admin, or target is the owner' })
  async removeMember(@Param('chatId') chatId: string, @Body() dto: TargetUserDto) {
    const chat = await this.chatService.removeMember(chatId, dto.userId);
    this.chatGateway.emitToRoom(chatId, 'memberRemoved', { chatId, userId: dto.userId });
    return chat;
  }

  @Post(':chatId/leave')
  @ApiOperation({ summary: 'Leave a group chat' })
  @ApiResponse({ status: 201, description: 'Left the group; ownership/admin reassigned if needed' })
  @ApiResponse({ status: 403, description: 'Not a participant in this chat' })
  async leave(@Req() req: { user: { id: string } }, @Param('chatId') chatId: string) {
    const chat = await this.chatService.leaveGroup(req.user.id, chatId);
    this.chatGateway.emitToRoom(chatId, 'memberLeft', { chatId, userId: req.user.id });
    return chat ?? { message: 'Group deleted — no members remaining' };
  }

  @Patch(':chatId/name')
  @UseGuards(ChatAdminGuard)
  @ApiOperation({ summary: 'Rename a group chat (admin only)' })
  @ApiResponse({ status: 200, description: 'Group renamed' })
  @ApiResponse({ status: 403, description: 'Not a group admin' })
  async rename(@Param('chatId') chatId: string, @Body() dto: RenameGroupDto) {
    const chat = await this.chatService.renameGroup(chatId, dto.name);
    this.chatGateway.emitToRoom(chatId, 'groupRenamed', { chatId, name: dto.name });
    return chat;
  }

  @Patch(':chatId/description')
  @UseGuards(ChatAdminGuard)
  @ApiOperation({ summary: 'Update group description (admin only)' })
  @ApiResponse({ status: 200, description: 'Description updated' })
  @ApiResponse({ status: 403, description: 'Not a group admin' })
  async updateDescription(@Param('chatId') chatId: string, @Body() dto: UpdateDescriptionDto) {
    const chat = await this.chatService.updateDescription(chatId, dto.description);
    this.chatGateway.emitToRoom(chatId, 'groupUpdated', { chatId, description: dto.description });
    return chat;
  }

  @Patch(':chatId/visibility')
  @UseGuards(ChatAdminGuard)
  @ApiOperation({ summary: 'Change group visibility — public/private (admin only)' })
  @ApiResponse({ status: 200, description: 'Visibility updated' })
  @ApiResponse({ status: 403, description: 'Not a group admin' })
  async updateVisibility(@Param('chatId') chatId: string, @Body() dto: UpdateVisibilityDto) {
    const chat = await this.chatService.updateVisibility(chatId, dto.isPublic);
    this.chatGateway.emitToRoom(chatId, 'groupUpdated', { chatId, isPublic: dto.isPublic });
    return chat;
  }

  @Patch(':chatId/admins')
  @UseGuards(ChatAdminGuard)
  @ApiOperation({ summary: 'Promote a member to group admin (admin only)' })
  @ApiResponse({ status: 200, description: 'Admin added' })
  @ApiResponse({ status: 400, description: 'Target is not a member of the group' })
  @ApiResponse({ status: 403, description: 'Not a group admin' })
  async addAdmin(@Param('chatId') chatId: string, @Body() dto: TargetUserDto) {
    const chat = await this.chatService.addAdmin(chatId, dto.userId);
    this.chatGateway.emitToRoom(chatId, 'adminAdded', { chatId, userId: dto.userId });
    return chat;
  }

  @Delete(':chatId/admins')
  @UseGuards(ChatAdminGuard)
  @ApiOperation({ summary: 'Demote a group admin back to a regular member (admin only)' })
  @ApiResponse({ status: 200, description: 'Admin removed' })
  @ApiResponse({ status: 400, description: 'Target is not an admin, or is the last remaining admin' })
  @ApiResponse({ status: 403, description: 'Not a group admin' })
  async removeAdmin(@Param('chatId') chatId: string, @Body() dto: TargetUserDto) {
    const chat = await this.chatService.removeAdmin(chatId, dto.userId);
    this.chatGateway.emitToRoom(chatId, 'adminRemoved', { chatId, userId: dto.userId });
    return chat;
  }

  @Post(':chatId/image')
  @UseGuards(ChatAdminGuard)
  @UseInterceptors(FileInterceptor('image'))
  @ApiConsumes('multipart/form-data')
  @ApiBody({ schema: { type: 'object', properties: { image: { type: 'string', format: 'binary' } } } })
  @ApiOperation({ summary: 'Upload a group chat photo (admin only)' })
  @ApiResponse({ status: 201, description: 'Group photo uploaded' })
  @ApiResponse({ status: 403, description: 'Not a group admin' })
  async uploadImage(@Param('chatId') chatId: string, @UploadedFile() file: Express.Multer.File) {
    const chat = await this.chatService.uploadGroupImage(chatId, file);
    this.chatGateway.emitToRoom(chatId, 'groupUpdated', { chatId, imageUrl: chat.imageUrl });
    return chat;
  }

  @Delete(':chatId')
  @UseGuards(ChatAdminGuard)
  @ApiOperation({ summary: 'Delete a group chat and all its messages (admin only)' })
  @ApiResponse({ status: 200, description: 'Group deleted' })
  @ApiResponse({ status: 403, description: 'Not a group admin' })
  async deleteGroup(@Param('chatId') chatId: string) {
    this.chatGateway.emitToRoom(chatId, 'groupDeleted', { chatId });
    await this.chatService.deleteGroup(chatId);
    return { message: 'Group deleted' };
  }
}

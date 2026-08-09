import { Body, Controller, Delete, Get, HttpCode, Param, Patch, Post, Req, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CommentsService } from './comments.service';
import { CreateCommentDto } from './dto/create-comment.dto';

@ApiTags('posts')
@Controller('posts/:postId/comments')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class CommentsController {
  constructor(private readonly commentsService: CommentsService) {}

  @Post()
  @ApiOperation({ summary: 'Comment on a post' })
  @ApiResponse({ status: 201, description: 'Comment created' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'Post not found' })
  create(@Req() req: { user: { id: string } }, @Param('postId') postId: string, @Body() dto: CreateCommentDto) {
    return this.commentsService.addComment(req.user.id, postId, dto);
  }

  @Get()
  @ApiOperation({ summary: 'List comments on a post' })
  @ApiResponse({ status: 200, description: 'Array of comments, oldest first' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  list(@Param('postId') postId: string) {
    return this.commentsService.getComments(postId);
  }

  @Patch(':commentId')
  @ApiOperation({ summary: 'Edit a comment (author only)' })
  @ApiResponse({ status: 200, description: 'Comment updated' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'Comment belongs to a different user' })
  @ApiResponse({ status: 404, description: 'Comment not found' })
  update(
    @Req() req: { user: { id: string } },
    @Param('commentId') commentId: string,
    @Body() dto: CreateCommentDto,
  ) {
    return this.commentsService.updateComment(req.user.id, commentId, dto);
  }

  @Delete(':commentId')
  @HttpCode(200)
  @ApiOperation({ summary: 'Delete a comment (author only)' })
  @ApiResponse({ status: 200, description: 'Comment deleted' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'Comment belongs to a different user' })
  @ApiResponse({ status: 404, description: 'Comment not found' })
  remove(@Req() req: { user: { id: string } }, @Param('commentId') commentId: string) {
    return this.commentsService.deleteComment(req.user.id, commentId);
  }
}

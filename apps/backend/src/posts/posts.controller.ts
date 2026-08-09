import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  Param,
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
import { CreatePostDto } from './dto/create-post.dto';
import { PostsService } from './posts.service';

@ApiTags('posts')
@Controller('posts')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class PostsController {
  constructor(private readonly postsService: PostsService) {}

  @Post()
  @UseInterceptors(FileInterceptor('image'))
  @ApiConsumes('multipart/form-data')
  @ApiOperation({ summary: 'Create a post with an optional image' })
  @ApiBody({
    schema: {
      type: 'object',
      required: ['habitName', 'completionNote'],
      properties: {
        habitName: { type: 'string', example: 'Morning Run' },
        completionNote: { type: 'string', example: 'Completed my 5 km run today!' },
        image: { type: 'string', format: 'binary' },
      },
    },
  })
  @ApiResponse({ status: 201, description: 'Post created' })
  @ApiResponse({ status: 400, description: 'Validation error' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  create(
    @Req() req: { user: { id: string } },
    @Body() dto: CreatePostDto,
    @UploadedFile() file?: Express.Multer.File,
  ) {
    return this.postsService.createPost(req.user.id, dto, file);
  }

  @Get()
  @ApiOperation({ summary: 'Paginated social feed' })
  @ApiQuery({ name: 'page', required: false, type: Number, example: 1 })
  @ApiQuery({ name: 'limit', required: false, type: Number, example: 20 })
  @ApiQuery({ name: 'friendsOnly', required: false, type: Boolean, example: false })
  @ApiResponse({ status: 200, description: 'Array of posts' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  getFeed(
    @Req() req: { user: { id: string } },
    @Query('page') page = '1',
    @Query('limit') limit = '20',
    @Query('friendsOnly') friendsOnly = 'false',
  ) {
    return this.postsService.getFeed(req.user.id, Number(page), Number(limit), friendsOnly === 'true');
  }

  @Get('me')
  @ApiOperation({ summary: 'My success journal — all posts by the current user' })
  @ApiResponse({ status: 200, description: 'Array of posts by the current user' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  getMyPosts(@Req() req: { user: { id: string } }) {
    return this.postsService.getMyPosts(req.user.id);
  }

  @Delete(':id')
  @HttpCode(200)
  @ApiOperation({ summary: 'Delete a post (author only) — also removes the image from storage' })
  @ApiResponse({ status: 200, description: 'Post deleted' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'Post belongs to a different user' })
  @ApiResponse({ status: 404, description: 'Post not found' })
  delete(@Req() req: { user: { id: string } }, @Param('id') id: string) {
    return this.postsService.deletePost(req.user.id, id);
  }

  @Post(':id/like')
  @ApiOperation({ summary: 'Like a post (idempotent)' })
  @ApiResponse({ status: 201, description: 'Post liked' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'Post not found' })
  like(@Req() req: { user: { id: string } }, @Param('id') id: string) {
    return this.postsService.likePost(req.user.id, id);
  }

  @Delete(':id/like')
  @HttpCode(200)
  @ApiOperation({ summary: 'Unlike a post (idempotent)' })
  @ApiResponse({ status: 200, description: 'Post unliked' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'Post not found' })
  unlike(@Req() req: { user: { id: string } }, @Param('id') id: string) {
    return this.postsService.unlikePost(req.user.id, id);
  }
}

import { Controller, Delete, Get, HttpCode, Param, Post, Req, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { FollowsService } from './follows.service';

@ApiTags('follows')
@Controller('users/:id')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class FollowsController {
  constructor(private readonly followsService: FollowsService) {}

  @Post('follow')
  @ApiOperation({ summary: 'Follow a user' })
  @ApiResponse({ status: 201, description: 'Now following the user' })
  @ApiResponse({ status: 400, description: 'Cannot follow yourself' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User not found' })
  follow(@Req() req: { user: { id: string } }, @Param('id') id: string) {
    return this.followsService.follow(req.user.id, id);
  }

  @Delete('follow')
  @HttpCode(200)
  @ApiOperation({ summary: 'Unfollow a user' })
  @ApiResponse({ status: 200, description: 'No longer following the user' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  unfollow(@Req() req: { user: { id: string } }, @Param('id') id: string) {
    return this.followsService.unfollow(req.user.id, id);
  }

  @Get('followers')
  @ApiOperation({ summary: 'User IDs of accounts following this user' })
  @ApiResponse({ status: 200, description: 'Array of follower user IDs' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  getFollowers(@Param('id') id: string) {
    return this.followsService.getFollowers(id);
  }

  @Get('following')
  @ApiOperation({ summary: 'User IDs of accounts this user follows' })
  @ApiResponse({ status: 200, description: 'Array of followed user IDs' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  getFollowing(@Param('id') id: string) {
    return this.followsService.getFollowing(id);
  }
}

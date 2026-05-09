import { Controller, Get, Req, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { UsersService } from './users.service';

@ApiTags('users')
@Controller('users')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get('me/home')
  @ApiOperation({ summary: 'Get persona, goals and daily tasks for the authenticated user' })
  @ApiResponse({ status: 200, description: 'Home page data returned successfully' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User not found' })
  getHome(@Req() req: { user: { id: string } }) {
    return this.usersService.getHomePageData(req.user.id);
  }
}

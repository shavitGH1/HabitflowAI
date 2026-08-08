import { Controller, HttpCode, Post, Req, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CoachService } from './coach.service';

@ApiTags('coach')
@Controller('coach')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class CoachController {
  constructor(private readonly coachService: CoachService) {}

  @Post('check-in')
  @HttpCode(200)
  @ApiOperation({ summary: 'Post the daily coach summary for the current user immediately' })
  @ApiResponse({ status: 200, description: 'The message the coach posted' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User not found' })
  checkIn(@Req() req: { user: { id: string } }) {
    return this.coachService.postDaily(req.user.id, true);
  }

  @Post('weekly-review')
  @HttpCode(200)
  @ApiOperation({ summary: 'Post the weekly coach review for the current user immediately' })
  @ApiResponse({ status: 200, description: 'The message the coach posted' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User not found' })
  weeklyReview(@Req() req: { user: { id: string } }) {
    return this.coachService.postWeekly(req.user.id, true);
  }
}

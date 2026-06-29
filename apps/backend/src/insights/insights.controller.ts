import { Body, Controller, Get, HttpCode, Post, Req, UseGuards } from '@nestjs/common';
import { ThrottlerGuard } from '@nestjs/throttler';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { MotivationFeedbackDto } from './dto/motivation-feedback.dto';
import { InsightsService } from './insights.service';

@ApiTags('ai')
@Controller('ai')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class InsightsController {
  constructor(private readonly insightsService: InsightsService) {}

  @Get('weekly-insights')
  @ApiOperation({ summary: 'Get the AI weekly progress review (cached per user per week)' })
  @ApiResponse({ status: 200, description: 'Weekly insights summary' })
  @ApiResponse({ status: 400, description: 'User has an unknown persona' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User not found' })
  weeklyInsights(@Req() req: { user: { id: string } }) {
    return this.insightsService.getWeeklyInsights(req.user.id);
  }

  @Post('motivation-feedback')
  @HttpCode(200)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Submit thumbs up/down feedback on a motivation message' })
  @ApiResponse({ status: 200, description: 'Feedback recorded' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User not found' })
  @ApiResponse({ status: 429, description: 'Too many requests' })
  motivationFeedback(
    @Req() req: { user: { id: string } },
    @Body() dto: MotivationFeedbackDto,
  ) {
    return this.insightsService.recordFeedback(req.user.id, dto.vote);
  }
}

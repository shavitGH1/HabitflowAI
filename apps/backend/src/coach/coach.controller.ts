import { Body, Controller, HttpCode, Post, Req, UseGuards } from '@nestjs/common';
import { ThrottlerGuard } from '@nestjs/throttler';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CoachChatDto } from '../personas/dto/coach-chat.dto';
import { ConfirmCoachChangeDto } from '../personas/dto/confirm-coach-change.dto';
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

  @Post('chat')
  @HttpCode(200)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Send a message to the coach; both sides are stored in the coach chat' })
  @ApiResponse({ status: 200, description: 'Coach reply with an optional proposedChange to confirm' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User not found' })
  @ApiResponse({ status: 429, description: 'Too many requests' })
  chat(@Req() req: { user: { id: string } }, @Body() dto: CoachChatDto) {
    return this.coachService.converse(req.user.id, dto);
  }

  @Post('chat/confirm')
  @HttpCode(200)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Apply a change the coach proposed, after explicit user confirmation' })
  @ApiResponse({ status: 200, description: 'Change applied and confirmed in the coach chat' })
  @ApiResponse({ status: 400, description: 'Only an active goal can be forfeited' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User or goal not found' })
  @ApiResponse({ status: 429, description: 'Too many requests' })
  confirmChange(@Req() req: { user: { id: string } }, @Body() dto: ConfirmCoachChangeDto) {
    return this.coachService.confirmChange(req.user.id, dto);
  }
}

import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiQuery, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { LeaderboardService } from './leaderboard.service';

@ApiTags('leaderboard')
@Controller('leaderboard')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class LeaderboardController {
  constructor(private readonly leaderboardService: LeaderboardService) {}

  @Get()
  @ApiOperation({ summary: "Current month's live leaderboard standings, ranked by points" })
  @ApiQuery({ name: 'limit', required: false, type: Number, example: 50 })
  @ApiResponse({ status: 200, description: 'Ranked standings for the current month' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  getStandings(@Query('limit') limit = '50') {
    return this.leaderboardService.getCurrentStandings(Number(limit));
  }

  @Get('archive')
  @ApiOperation({ summary: "Past months' final standings and placement achievements" })
  @ApiQuery({ name: 'monthStart', required: false, type: String, example: '2026-07-01' })
  @ApiResponse({ status: 200, description: 'Archived monthly standings, most recent first' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  getArchive(@Query('monthStart') monthStart?: string) {
    return this.leaderboardService.getArchive(monthStart);
  }
}

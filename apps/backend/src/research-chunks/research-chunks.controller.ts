import { BadRequestException, Controller, Get, Query, UseGuards } from '@nestjs/common';
import { ThrottlerGuard } from '@nestjs/throttler';
import { ApiBearerAuth, ApiOperation, ApiQuery, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { ResearchChunksService } from './research-chunks.service';

@ApiTags('research-chunks')
@Controller('research-chunks')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class ResearchChunksController {
  constructor(private readonly researchChunksService: ResearchChunksService) {}

  @Get('search')
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Search chunked research content relevant to a coaching query' })
  @ApiQuery({ name: 'query', type: String })
  @ApiResponse({ status: 200, description: 'Top matching research chunks, best match first' })
  @ApiResponse({ status: 400, description: 'Missing or empty query' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 429, description: 'Too many requests' })
  search(@Query('query') query: string) {
    if (!query || !query.trim()) {
      throw new BadRequestException('query must not be empty');
    }
    return this.researchChunksService.search(query);
  }
}

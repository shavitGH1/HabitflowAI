import { BadRequestException, Controller, Get, Query, UseGuards } from '@nestjs/common';
import { ThrottlerGuard } from '@nestjs/throttler';
import { ApiBearerAuth, ApiOperation, ApiQuery, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { ArticlesService } from './articles.service';

@ApiTags('articles')
@Controller('articles')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class ArticlesController {
  constructor(private readonly articlesService: ArticlesService) {}

  @Get('search')
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Search supporting articles relevant to a coaching query' })
  @ApiQuery({ name: 'query', type: String })
  @ApiResponse({ status: 200, description: 'Top matching articles, best match first' })
  @ApiResponse({ status: 400, description: 'Missing or empty query' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 429, description: 'Too many requests' })
  search(@Query('query') query: string) {
    if (!query || !query.trim()) {
      throw new BadRequestException('query must not be empty');
    }
    return this.articlesService.search(query);
  }
}

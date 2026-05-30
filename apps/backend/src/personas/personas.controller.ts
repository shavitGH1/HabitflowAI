import { Body, Controller, HttpCode, Post, Req, UseGuards } from '@nestjs/common';
import { ThrottlerGuard } from '@nestjs/throttler';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { ReclassifyDto } from './dto/reclassify.dto';
import { PersonasService } from './personas.service';

@ApiTags('personas')
@Controller('personas')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class PersonasController {
  constructor(private readonly personasService: PersonasService) {}

  @Post('reclassify')
  @HttpCode(200)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Reclassify the user persona based on updated goal and answers' })
  @ApiResponse({ status: 200, description: 'Persona reclassified and goals regenerated' })
  @ApiResponse({ status: 400, description: 'Invalid goal input rejected by AI' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User not found' })
  @ApiResponse({ status: 429, description: 'Too many requests' })
  reclassify(@Req() req: { user: { id: string } }, @Body() dto: ReclassifyDto) {
    return this.personasService.reclassify(req.user.id, dto);
  }
}

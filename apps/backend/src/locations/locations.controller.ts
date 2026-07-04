import { Body, Controller, HttpCode, Post, Req, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { LocationDto } from './dto/location.dto';
import { LocationsService } from './locations.service';

@ApiTags('locations')
@Controller('locations')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class LocationsController {
  constructor(private readonly locationsService: LocationsService) {}

  @Post()
  @HttpCode(200)
  @ApiOperation({ summary: 'Record a habit completion location' })
  @ApiResponse({ status: 200, description: 'Location recorded' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  recordLocation(@Req() req: { user: { id: string } }, @Body() dto: LocationDto) {
    return this.locationsService.recordLocation(req.user.id, dto);
  }
}

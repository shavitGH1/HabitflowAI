import { Body, Controller, Get, HttpCode, Post, Query, Req, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiQuery, ApiResponse, ApiTags } from '@nestjs/swagger';
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

  @Get()
  @ApiOperation({ summary: 'Get public locations within a bounding box for the Activity Map' })
  @ApiQuery({ name: 'minLat', type: Number, example: 31.0 })
  @ApiQuery({ name: 'maxLat', type: Number, example: 33.0 })
  @ApiQuery({ name: 'minLng', type: Number, example: 34.0 })
  @ApiQuery({ name: 'maxLng', type: Number, example: 36.0 })
  @ApiResponse({ status: 200, description: 'Public location records within the bounding box' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  getBbox(
    @Query('minLat') minLat: string,
    @Query('maxLat') maxLat: string,
    @Query('minLng') minLng: string,
    @Query('maxLng') maxLng: string,
  ) {
    return this.locationsService.getBbox(Number(minLat), Number(maxLat), Number(minLng), Number(maxLng));
  }
}

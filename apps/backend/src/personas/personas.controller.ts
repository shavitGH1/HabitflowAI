import { Body, Controller, Post, Req, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiResponse, ApiTags } from '@nestjs/swagger';
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
  @ApiResponse({ status: 200, description: 'Persona reclassified' })
  reclassify(@Req() req: { user: { id: string } }, @Body() dto: ReclassifyDto) {
    return this.personasService.reclassify(req.user.id, dto);
  }
}

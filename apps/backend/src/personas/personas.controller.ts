import { Body, Controller, HttpCode, Post, Req, UseGuards } from '@nestjs/common';
import { ThrottlerGuard } from '@nestjs/throttler';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { DriftCheckDto } from './dto/drift-check.dto';
import { ReclassifyDto } from './dto/reclassify.dto';
import { CoachChatDto } from './dto/coach-chat.dto';
import { ConfirmCoachChangeDto } from './dto/confirm-coach-change.dto';
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

  @Post('drift-check')
  @HttpCode(200)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Evaluate whether the user is drifting toward a different persona' })
  @ApiResponse({ status: 200, description: 'Drift evaluation result' })
  @ApiResponse({ status: 400, description: 'User has an unknown persona' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User not found' })
  @ApiResponse({ status: 429, description: 'Too many requests' })
  driftCheck(@Req() req: { user: { id: string } }, @Body() dto: DriftCheckDto) {
    return this.personasService.driftCheck(req.user.id, dto);
  }

  @Post('coach-chat')
  @HttpCode(200)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Send a message to the coaching agent and get a reply plus an optional proposed change' })
  @ApiResponse({ status: 200, description: 'Coach reply, with an optional proposedChange the user must explicitly confirm' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User not found' })
  @ApiResponse({ status: 429, description: 'Too many requests' })
  coachChat(@Req() req: { user: { id: string } }, @Body() dto: CoachChatDto) {
    return this.personasService.coachChat(req.user.id, dto);
  }

  @Post('coach-chat/confirm')
  @HttpCode(200)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Apply a coaching-agent proposed change after explicit user confirmation' })
  @ApiResponse({ status: 200, description: 'Change applied' })
  @ApiResponse({ status: 400, description: 'Only an active goal can be forfeited' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User or goal not found' })
  @ApiResponse({ status: 429, description: 'Too many requests' })
  confirmCoachChange(@Req() req: { user: { id: string } }, @Body() dto: ConfirmCoachChangeDto) {
    return this.personasService.confirmCoachChange(req.user.id, dto);
  }
}

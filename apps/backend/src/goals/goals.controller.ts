import { Body, Controller, Get, Param, Patch, Post, Req, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CreateGoalDto } from './dto/create-goal.dto';
import { TransitionGoalDto } from './dto/transition-goal.dto';
import { ResolveHabitsDto } from './dto/resolve-habits.dto';
import { GoalsService } from './goals.service';

@ApiTags('goals')
@Controller('goals')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class GoalsController {
  constructor(private readonly goalsService: GoalsService) {}

  @Post()
  @ApiOperation({ summary: 'Create a new goal — rejects if the user already has an active goal' })
  @ApiResponse({ status: 201, description: 'Goal created' })
  @ApiResponse({ status: 400, description: 'User already has an active goal, or invalid input' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  create(@Req() req: { user: { id: string } }, @Body() dto: CreateGoalDto) {
    return this.goalsService.createGoal(req.user.id, dto);
  }

  @Get('active')
  @ApiOperation({ summary: "Get the user's current active goal" })
  @ApiResponse({ status: 200, description: 'The active goal, or null if none' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  getActive(@Req() req: { user: { id: string } }) {
    return this.goalsService.getActiveGoal(req.user.id);
  }

  @Patch(':id/forfeit')
  @ApiOperation({ summary: 'Forfeit an active goal' })
  @ApiResponse({ status: 200, description: 'Goal forfeited' })
  @ApiResponse({ status: 400, description: 'Goal is not active' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'Goal belongs to a different user' })
  @ApiResponse({ status: 404, description: 'Goal not found' })
  forfeit(@Req() req: { user: { id: string } }, @Param('id') id: string) {
    return this.goalsService.forfeitGoal(req.user.id, id);
  }

  @Patch(':id/achieve')
  @ApiOperation({ summary: 'Mark an active goal as achieved — awards a medal on the user\'s profile' })
  @ApiResponse({ status: 200, description: 'Goal achieved, medal awarded' })
  @ApiResponse({ status: 400, description: 'Goal is not active' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'Goal belongs to a different user' })
  @ApiResponse({ status: 404, description: 'Goal not found' })
  achieve(@Req() req: { user: { id: string } }, @Param('id') id: string) {
    return this.goalsService.achieveGoal(req.user.id, id);
  }

  @Post(':id/transition')
  @ApiOperation({
    summary: 'Resolve a goal (achieve/forfeit) and start a new one in one step',
    description:
      'Resolves the given goal and creates a new goal. Call POST /goals/:id/resolve-habits ' +
      "next (same :id) to decide what happens to the old goal's still-active habits.",
  })
  @ApiResponse({ status: 201, description: 'Goal transitioned; returns the old goal and new goal' })
  @ApiResponse({ status: 400, description: 'Goal is not active, or invalid input' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'Goal belongs to a different user' })
  @ApiResponse({ status: 404, description: 'Goal not found' })
  transition(
    @Req() req: { user: { id: string } },
    @Param('id') id: string,
    @Body() dto: TransitionGoalDto,
  ) {
    return this.goalsService.transitionGoal(req.user.id, id, dto);
  }

  @Post(':id/resolve-habits')
  @ApiOperation({
    summary: "Decide what happens to a resolved goal's still-active habits",
    description:
      'With no `decision`, runs an AI relevance check against the new goal and auto-applies the ' +
      'result. If the AI call fails, returns `needs_decision` and touches no habits — call again ' +
      'with an explicit `decision` (or no decision, to retry the AI) to resolve it.',
  })
  @ApiResponse({ status: 201, description: 'Either resolved (habits relinked/archived) or needs_decision' })
  @ApiResponse({ status: 400, description: 'Invalid input' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'A referenced goal belongs to a different user' })
  @ApiResponse({ status: 404, description: 'Goal not found' })
  resolveHabits(
    @Req() req: { user: { id: string } },
    @Param('id') id: string,
    @Body() dto: ResolveHabitsDto,
  ) {
    return this.goalsService.resolveHabits(req.user.id, id, dto);
  }
}

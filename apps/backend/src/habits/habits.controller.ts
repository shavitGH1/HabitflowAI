import { Body, Controller, Delete, Get, HttpCode, Param, Patch, Post, Req, UseGuards } from '@nestjs/common';
import { ThrottlerGuard } from '@nestjs/throttler';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CompleteHabitDto } from './dto/complete-habit.dto';
import { CreateHabitDto } from './dto/create-habit.dto';
import { UpdateHabitDto } from './dto/update-habit.dto';
import { HabitsService } from './habits.service';

@ApiTags('habits')
@Controller('habits')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class HabitsController {
  constructor(private readonly habitsService: HabitsService) {}

  @Post()
  @UseGuards(ThrottlerGuard)
  @ApiOperation({
    summary: 'Create a new habit',
    description:
      'If goalId is set, an AI relevance check runs against the goal — on a mismatch the ' +
      'response includes a relevanceWarning but the habit is still created (soft warning, never blocks).',
  })
  @ApiResponse({ status: 201, description: 'Habit created' })
  @ApiResponse({ status: 400, description: 'Validation error, goal not active, or cap exceeded' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'Goal belongs to a different user' })
  create(@Req() req: { user: { id: string } }, @Body() dto: CreateHabitDto) {
    return this.habitsService.createHabit(req.user.id, dto);
  }

  @Get()
  @ApiOperation({ summary: "List the user's active habits" })
  @ApiResponse({ status: 200, description: 'Array of non-archived habits' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  findAll(@Req() req: { user: { id: string } }) {
    return this.habitsService.findByUserId(req.user.id);
  }

  @Patch(':id')
  @UseGuards(ThrottlerGuard)
  @ApiOperation({
    summary: 'Update habit title, description, frequency, targetCount, or goal link',
    description:
      'Pass goalId to link/relink to a different active goal, null to unlink to standalone, ' +
      'or omit it to leave the current link untouched. Relinking runs the same AI relevance ' +
      'check as creation (soft warning, never blocks).',
  })
  @ApiResponse({ status: 200, description: 'Habit updated' })
  @ApiResponse({ status: 400, description: 'Validation error, goal not active, or cap exceeded' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'Habit or goal belongs to a different user' })
  @ApiResponse({ status: 404, description: 'Habit not found' })
  update(
    @Req() req: { user: { id: string } },
    @Param('id') id: string,
    @Body() dto: UpdateHabitDto,
  ) {
    return this.habitsService.updateHabit(req.user.id, id, dto);
  }

  @Delete(':id')
  @HttpCode(200)
  @ApiOperation({ summary: 'Soft-delete (archive) a habit' })
  @ApiResponse({ status: 200, description: 'Habit archived' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'Habit belongs to a different user' })
  @ApiResponse({ status: 404, description: 'Habit not found' })
  delete(@Req() req: { user: { id: string } }, @Param('id') id: string) {
    return this.habitsService.deleteHabit(req.user.id, id);
  }

  @Patch(':id/complete')
  @HttpCode(200)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({
    summary: "Mark a habit as complete for today — appends today's date and recalculates streak",
    description:
      'An optional note ("what did you do?") is checked for plausibility against the habit — on a ' +
      'mismatch the response includes a verificationWarning but completion still succeeds (soft warning, never blocks).',
  })
  @ApiResponse({ status: 200, description: 'Habit completed; returns updated streak and completionHistory' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'Habit belongs to a different user' })
  @ApiResponse({ status: 404, description: 'Habit not found' })
  complete(@Req() req: { user: { id: string } }, @Param('id') id: string, @Body() dto: CompleteHabitDto) {
    return this.habitsService.completeHabit(req.user.id, id, dto.note);
  }

  @Get(':id/stats')
  @ApiOperation({ summary: 'Completion stats for a habit, derived from its completion history' })
  @ApiResponse({ status: 200, description: 'Aggregate completion stats' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'Habit belongs to a different user' })
  @ApiResponse({ status: 404, description: 'Habit not found' })
  getStats(@Req() req: { user: { id: string } }, @Param('id') id: string) {
    return this.habitsService.getStats(req.user.id, id);
  }
}

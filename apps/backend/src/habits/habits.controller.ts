import { Body, Controller, Delete, Get, HttpCode, Param, Patch, Post, Req, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
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
  @ApiOperation({ summary: 'Create a new habit' })
  @ApiResponse({ status: 201, description: 'Habit created' })
  @ApiResponse({ status: 400, description: 'Validation error' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
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
  @ApiOperation({ summary: 'Update habit title, description, frequency, or targetCount' })
  @ApiResponse({ status: 200, description: 'Habit updated' })
  @ApiResponse({ status: 400, description: 'Validation error' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'Habit belongs to a different user' })
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
  @ApiOperation({ summary: "Mark a habit as complete for today — appends today's date and recalculates streak" })
  @ApiResponse({ status: 200, description: 'Habit completed; returns updated streak and completionHistory' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 403, description: 'Habit belongs to a different user' })
  @ApiResponse({ status: 404, description: 'Habit not found' })
  complete(@Req() req: { user: { id: string } }, @Param('id') id: string) {
    return this.habitsService.completeHabit(req.user.id, id);
  }
}

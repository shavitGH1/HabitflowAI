import { Controller, HttpCode, Param, Patch, Req, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { TasksService } from './tasks.service';

@ApiTags('tasks')
@Controller('tasks')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class TasksController {
  constructor(private readonly tasksService: TasksService) {}

  @Patch(':taskId/complete')
  @HttpCode(200)
  @ApiOperation({ summary: 'Mark a core goal or daily variation as complete' })
  @ApiResponse({ status: 200, description: 'Task marked as complete' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User or task not found' })
  complete(@Req() req: { user: { id: string } }, @Param('taskId') taskId: string) {
    return this.tasksService.completeTask(req.user.id, taskId);
  }
}

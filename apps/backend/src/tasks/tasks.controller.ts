import { Controller, Param, Patch, Req, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiResponse, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { TasksService } from './tasks.service';

@ApiTags('tasks')
@Controller('tasks')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class TasksController {
  constructor(private readonly tasksService: TasksService) {}

  @Patch(':taskId/complete')
  @ApiResponse({ status: 200, description: 'Task completed' })
  complete(@Req() req: { user: { id: string } }, @Param('taskId') taskId: string) {
    return this.tasksService.completeTask(req.user.id, taskId);
  }
}

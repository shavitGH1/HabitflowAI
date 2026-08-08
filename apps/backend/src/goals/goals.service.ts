import { BadRequestException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { GoalData, GoalRepository } from './goal.repository';
import { UserRepository } from '../users/user.repository';
import { CreateGoalDto } from './dto/create-goal.dto';

export const GOAL_ACHIEVEMENT_MEDAL = 'goal-achiever';

@Injectable()
export class GoalsService {
  constructor(
    private readonly goalRepository: GoalRepository,
    private readonly userRepository: UserRepository,
  ) {}

  async createGoal(userId: string, dto: CreateGoalDto): Promise<GoalData> {
    const existingActive = await this.goalRepository.findActiveByUserId(userId);
    if (existingActive) {
      throw new BadRequestException('You already have an active goal — forfeit it before starting a new one');
    }

    return this.goalRepository.createGoal({
      userId,
      title: dto.title,
      targetDate: new Date(dto.targetDate),
    });
  }

  async getActiveGoal(userId: string): Promise<GoalData | null> {
    return this.goalRepository.findActiveByUserId(userId);
  }

  async forfeitGoal(userId: string, id: string): Promise<GoalData> {
    const goal = await this.goalRepository.findById(id);
    if (!goal) throw new NotFoundException('Goal not found');
    if (goal.userId !== userId) throw new ForbiddenException('You do not own this goal');
    if (goal.status !== 'active') {
      throw new BadRequestException('Only an active goal can be forfeited');
    }

    return (await this.goalRepository.updateStatus(id, 'forfeited'))!;
  }

  async achieveGoal(userId: string, id: string): Promise<GoalData> {
    const goal = await this.goalRepository.findById(id);
    if (!goal) throw new NotFoundException('Goal not found');
    if (goal.userId !== userId) throw new ForbiddenException('You do not own this goal');
    if (goal.status !== 'active') {
      throw new BadRequestException('Only an active goal can be marked as achieved');
    }

    const achieved = (await this.goalRepository.updateStatus(id, 'achieved'))!;
    await this.userRepository.addAchievement(userId, {
      goalId: id,
      medal: GOAL_ACHIEVEMENT_MEDAL,
      awardedAt: new Date().toISOString(),
    });

    return achieved;
  }
}

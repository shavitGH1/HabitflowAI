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

    try {
      return await this.goalRepository.createGoal({
        userId,
        title: dto.title,
        targetDate: new Date(dto.targetDate),
      });
    } catch (error) {
      // Two concurrent requests both passed the check above — the unique
      // partial index on {userId, status: 'active'} stops the loser here.
      if (this.isDuplicateActiveGoalError(error)) {
        throw new BadRequestException('You already have an active goal — forfeit it before starting a new one');
      }
      throw error;
    }
  }

  async getActiveGoal(userId: string): Promise<GoalData | null> {
    const active = await this.goalRepository.findActiveByUserId(userId);
    if (active) return active;

    // Transition logic: if no formal Goal record exists but the user has a goal
    // string in their profile (from onboarding or old version), create it now.
    const user = await this.userRepository.findUserById(userId);
    if (!user?.goal) return null;

    const targetDate = new Date();
    targetDate.setMonth(targetDate.getMonth() + 3); // Default 3 month window
    return this.goalRepository.findOrCreateActiveGoal({
      userId,
      title: user.goal,
      targetDate,
    });
  }

  private isDuplicateActiveGoalError(error: unknown): boolean {
    return typeof error === 'object' && error !== null && (error as { code?: number }).code === 11000;
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

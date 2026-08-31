import { BadRequestException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { GoalData, GoalRepository } from './goal.repository';
import { UserRepository } from '../users/user.repository';
import { HabitRepository } from '../habits/habit.repository';
import { AiService } from '../ai/ai.service';
import { CreateGoalDto } from './dto/create-goal.dto';
import { TransitionGoalDto } from './dto/transition-goal.dto';

export const GOAL_ACHIEVEMENT_MEDAL = 'goal-achiever';

export interface TransitionGoalResult {
  oldGoal: GoalData;
  newGoal: GoalData;
  relinkedHabitIds: string[];
  archivedHabitIds: string[];
}

@Injectable()
export class GoalsService {
  constructor(
    private readonly goalRepository: GoalRepository,
    private readonly userRepository: UserRepository,
    private readonly habitRepository: HabitRepository,
    private readonly ai: AiService,
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

  async transitionGoal(userId: string, id: string, dto: TransitionGoalDto): Promise<TransitionGoalResult> {
    const oldGoal = dto.resolution === 'achieve'
      ? await this.achieveGoal(userId, id)
      : await this.forfeitGoal(userId, id);

    const newGoal = await this.createGoal(userId, {
      title: dto.newGoalTitle,
      targetDate: dto.newGoalTargetDate,
    });

    const { isRelated } = await this.ai.checkGoalRelevance({
      oldGoalTitle: oldGoal.title,
      newGoalTitle: newGoal.title,
    });

    // Only active (not-yet-achieved) habits under the old goal move - already-achieved
    // ones stay put, still visible under the old goal's history either way.
    const habits = await this.habitRepository.findByUserId(userId);
    const carryoverHabits = habits.filter(h => h.goalId === id && !h.implementedAt);

    const relinkedHabitIds: string[] = [];
    const archivedHabitIds: string[] = [];

    for (const habit of carryoverHabits) {
      if (isRelated) {
        await this.habitRepository.updateHabit(habit.id, { goalId: newGoal.id });
        relinkedHabitIds.push(habit.id);
      } else {
        await this.habitRepository.deleteHabit(habit.id);
        archivedHabitIds.push(habit.id);
      }
    }

    return { oldGoal, newGoal, relinkedHabitIds, archivedHabitIds };
  }
}

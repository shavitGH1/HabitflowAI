import { BadRequestException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { HabitData, HabitRepository } from './habit.repository';
import { GoalData, GoalRepository } from '../goals/goal.repository';
import { AiService } from '../ai/ai.service';
import { CreateHabitDto } from './dto/create-habit.dto';
import { UpdateHabitDto } from './dto/update-habit.dto';

export const MAX_HABITS_PER_GOAL = 3;
export const MAX_STANDALONE_HABITS = 2;

export type HabitWithWarning = HabitData & { relevanceWarning?: string };

@Injectable()
export class HabitsService {
  constructor(
    private readonly habitRepository: HabitRepository,
    private readonly goalRepository: GoalRepository,
    private readonly ai: AiService,
  ) {}

  async createHabit(userId: string, dto: CreateHabitDto): Promise<HabitWithWarning> {
    let relevanceWarning: string | undefined;

    if (dto.goalId) {
      const goal = await this.assertCanLinkToGoal(userId, dto.goalId);
      relevanceWarning = await this.checkRelevance(goal, dto.title, dto.description);
    } else {
      await this.assertCanGoStandalone(userId);
    }

    const habit = await this.habitRepository.createHabit({
      userId,
      title: dto.title,
      description: dto.description,
      frequency: dto.frequency,
      targetCount: dto.targetCount,
      goalId: dto.goalId,
    });

    return relevanceWarning ? { ...habit, relevanceWarning } : habit;
  }

  async findByUserId(userId: string): Promise<HabitData[]> {
    return this.habitRepository.findByUserId(userId);
  }

  async updateHabit(userId: string, id: string, dto: UpdateHabitDto): Promise<HabitWithWarning> {
    const habit = await this.habitRepository.findById(id);
    if (!habit) throw new NotFoundException('Habit not found');
    if (habit.userId !== userId) throw new ForbiddenException('You do not own this habit');

    let relevanceWarning: string | undefined;

    if (dto.goalId !== undefined) {
      if (dto.goalId === null) {
        await this.assertCanGoStandalone(userId, id);
      } else {
        const goal = await this.assertCanLinkToGoal(userId, dto.goalId, id);
        relevanceWarning = await this.checkRelevance(goal, dto.title ?? habit.title, dto.description ?? habit.description);
      }
    }

    const updated = (await this.habitRepository.updateHabit(id, {
      title: dto.title,
      description: dto.description,
      frequency: dto.frequency,
      targetCount: dto.targetCount,
      goalId: dto.goalId,
    }))!;

    return relevanceWarning ? { ...updated, relevanceWarning } : updated;
  }

  async deleteHabit(userId: string, id: string): Promise<HabitData> {
    const habit = await this.habitRepository.findById(id);
    if (!habit) throw new NotFoundException('Habit not found');
    if (habit.userId !== userId) throw new ForbiddenException('You do not own this habit');
    return (await this.habitRepository.deleteHabit(id))!;
  }

  async completeHabit(userId: string, id: string): Promise<HabitData> {
    const habit = await this.habitRepository.findById(id);
    if (!habit) throw new NotFoundException('Habit not found');
    if (habit.userId !== userId) throw new ForbiddenException('You do not own this habit');
    return (await this.habitRepository.completeHabit(id))!;
  }

  private async checkRelevance(goal: GoalData, habitTitle: string, habitDescription?: string): Promise<string | undefined> {
    const relevance = await this.ai.checkHabitGoalRelevance({
      goalTitle: goal.title,
      habitTitle,
      habitDescription,
    });
    return relevance.isRelated ? undefined : relevance.reason;
  }

  private async assertCanLinkToGoal(userId: string, goalId: string, excludeHabitId?: string): Promise<GoalData> {
    const goal = await this.goalRepository.findById(goalId);
    if (!goal) throw new BadRequestException('Goal not found');
    if (goal.userId !== userId) throw new ForbiddenException('You do not own this goal');
    if (goal.status !== 'active') {
      throw new BadRequestException('Habits can only be linked to an active goal');
    }

    const existingHabits = await this.habitRepository.findByUserId(userId);
    const linkedCount = existingHabits.filter(h => h.id !== excludeHabitId && h.goalId === goalId).length;
    if (linkedCount >= MAX_HABITS_PER_GOAL) {
      throw new BadRequestException(`A goal can have at most ${MAX_HABITS_PER_GOAL} linked habits`);
    }

    return goal;
  }

  private async assertCanGoStandalone(userId: string, excludeHabitId?: string): Promise<void> {
    const existingHabits = await this.habitRepository.findByUserId(userId);
    const standaloneCount = existingHabits.filter(h => h.id !== excludeHabitId && !h.goalId).length;
    if (standaloneCount >= MAX_STANDALONE_HABITS) {
      throw new BadRequestException(`You can have at most ${MAX_STANDALONE_HABITS} standalone habits`);
    }
  }
}

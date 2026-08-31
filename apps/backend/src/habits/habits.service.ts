import { BadRequestException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { HabitData, HabitRepository } from './habit.repository';
import { GoalData, GoalRepository } from '../goals/goal.repository';
import { AiService } from '../ai/ai.service';
import { LeaderboardService } from '../leaderboard/leaderboard.service';
import { CreateHabitDto } from './dto/create-habit.dto';
import { UpdateHabitDto } from './dto/update-habit.dto';
import { daysBetween } from './utils/consistency.utils';
import { MIN_STREAK_FOR_MANUAL_ACHIEVEMENT, canManuallyAchieve } from './utils/streak.utils';

export const MAX_HABITS_PER_GOAL = 3;
export const MAX_STANDALONE_HABITS = 2;

export type HabitWithWarning = HabitData & { relevanceWarning?: string; verificationWarning?: string };

export interface HabitStats {
  habitId: string;
  totalCompletions: number;
  currentStreak: number;
  consistencyScore: number;
  daysSinceCreation: number;
  completionRate: number;
  implementedAt?: string;
}

@Injectable()
export class HabitsService {
  constructor(
    private readonly habitRepository: HabitRepository,
    private readonly goalRepository: GoalRepository,
    private readonly ai: AiService,
    private readonly leaderboardService: LeaderboardService,
  ) {}

  async createHabit(userId: string, dto: CreateHabitDto): Promise<HabitWithWarning> {
    let relevanceWarning: string | undefined;
    let finalGoalId = dto.goalId;

    if (dto.goalId) {
      const goal = await this.assertCanLinkToGoal(userId, dto.goalId);
      const relevance = await this.ai.checkHabitGoalRelevance({
        goalTitle: goal.title,
        habitTitle: dto.title,
        habitDescription: dto.description,
      });

      if (!relevance.isRelated) {
        relevanceWarning = relevance.reason;
        // If unrelated, try to demote to standalone if there's room
        try {
          await this.assertCanGoStandalone(userId);
          finalGoalId = undefined;
        } catch (e) {
          // No room in standalone? Keep it linked but with the warning
        }
      }
    } else {
      await this.assertCanGoStandalone(userId);
    }

    const habit = await this.habitRepository.createHabit({
      userId,
      title: dto.title,
      description: dto.description,
      frequency: dto.frequency,
      targetCount: dto.targetCount,
      goalId: finalGoalId,
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
    if (habit.implementedAt) {
      throw new BadRequestException('An achieved habit cannot be abandoned');
    }
    return (await this.habitRepository.deleteHabit(id))!;
  }

  async completeHabit(userId: string, id: string, note?: string, date?: string): Promise<HabitWithWarning> {
    const habit = await this.habitRepository.findById(id);
    if (!habit) throw new NotFoundException('Habit not found');
    if (habit.userId !== userId) throw new ForbiddenException('You do not own this habit');

    // completeHabit() only appends today's date if it isn't already there, but
    // doesn't error on a repeat call for the same day — check first so
    // leaderboard points aren't awarded twice for one day's completion.
    const today = date ?? new Date().toISOString().split('T')[0];
    const alreadyCompletedToday = habit.completionHistory.includes(today);

    const completed = (await this.habitRepository.completeHabit(id, note, date))!;

    if (!alreadyCompletedToday) {
      await this.leaderboardService.recordCompletion(userId);
    }

    if (!note) return completed;

    const verification = await this.ai.checkTaskVerification({ habitTitle: habit.title, note });
    return verification.isPlausible ? completed : { ...completed, verificationWarning: verification.reason };
  }

  async markHabitAchieved(userId: string, id: string): Promise<HabitData> {
    const habit = await this.habitRepository.findById(id);
    if (!habit) throw new NotFoundException('Habit not found');
    if (habit.userId !== userId) throw new ForbiddenException('You do not own this habit');

    if (habit.implementedAt) {
      throw new BadRequestException('Habit is already marked as achieved');
    }
    if (!canManuallyAchieve(habit.streak)) {
      throw new BadRequestException(
        `Streak must be at least ${MIN_STREAK_FOR_MANUAL_ACHIEVEMENT} days to mark this habit as achieved`,
      );
    }

    return (await this.habitRepository.markAchieved(id))!;
  }

  async getStats(userId: string, id: string): Promise<HabitStats> {
    const habit = await this.habitRepository.findById(id);
    if (!habit) throw new NotFoundException('Habit not found');
    if (habit.userId !== userId) throw new ForbiddenException('You do not own this habit');

    const today = new Date().toISOString().split('T')[0];
    const daysSinceCreation = daysBetween(habit.createdAt.split('T')[0], today) + 1;

    return {
      habitId: habit.id,
      totalCompletions: habit.completionHistory.length,
      currentStreak: habit.streak,
      consistencyScore: habit.consistencyScore,
      daysSinceCreation,
      completionRate: habit.completionHistory.length / daysSinceCreation,
      implementedAt: habit.implementedAt,
    };
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
    const linkedCount = existingHabits.filter(
      h => h.id !== excludeHabitId && h.goalId === goalId && !h.implementedAt,
    ).length;
    if (linkedCount >= MAX_HABITS_PER_GOAL) {
      throw new BadRequestException(`A goal can have at most ${MAX_HABITS_PER_GOAL} linked habits`);
    }

    return goal;
  }

  private async assertCanGoStandalone(userId: string, excludeHabitId?: string): Promise<void> {
    const existingHabits = await this.habitRepository.findByUserId(userId);
    const standaloneCount = existingHabits.filter(
      h => h.id !== excludeHabitId && !h.goalId && !h.implementedAt,
    ).length;
    if (standaloneCount >= MAX_STANDALONE_HABITS) {
      throw new BadRequestException(`You can have at most ${MAX_STANDALONE_HABITS} standalone habits`);
    }
  }
}

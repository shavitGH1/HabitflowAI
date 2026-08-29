import { BadRequestException, Inject, Injectable, NotFoundException, UnauthorizedException } from '@nestjs/common';
import bcrypt from 'bcrypt';
import { v4 as uuidv4 } from 'uuid';
import { AiService } from '../ai/ai.service';
import { IStorageAdapter, STORAGE_ADAPTER } from '../storage/storage.adapter';
import { HabitRepository } from '../habits/habit.repository';
import { GoalRepository } from '../goals/goal.repository';
import { UserRepository } from './user.repository';

const NAME_CHANGE_COOLDOWN_MONTHS = 3;

@Injectable()
export class UsersService {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly habitRepository: HabitRepository,
    private readonly goalRepository: GoalRepository,
    private readonly ai: AiService,
    @Inject(STORAGE_ADAPTER) private readonly storage: IStorageAdapter,
  ) {}

  async getAllUsers(): Promise<{ id: string; email: string; firstName: string; lastName: string; profilePicture?: string }[]> {
    const users = await this.userRepository.findAllUsers();
    return users.map(u => ({ id: u.id, email: u.email, firstName: u.firstName, lastName: u.lastName, profilePicture: u.profilePicture }));
  }

  async getHomePageData(userId: string) {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    const habits = await this.habitRepository.findByUserId(userId);
    const today = new Date().toISOString().split('T')[0];
    if (user.tasksLastGeneratedDate !== today) {
      const habitInputs = habits.map(h => ({ id: h.id, title: h.title }));
      const newDailyTasks = await this.ai.generateDailyVariations(user, new Date().getDay(), habitInputs);
      const updatedTasks = newDailyTasks.map(t => ({ ...t, id: uuidv4(), completed: false }));
      const updated = await this.userRepository.updateUserDailyTasks(userId, updatedTasks);
      user.dailyVariations = updated?.dailyVariations ?? updatedTasks;
    }

    // Dynamic mapping for "General" header issue:
    // If a task description exactly matches a habit title, it's a redundant high-level task;
    // we filter it out to prevent duplicates.
    const habitMapByTitle = new Map(habits.map(h => [h.title.toLowerCase().trim(), h.id]));
    const seenDescriptions = new Set<string>();

    const enrichTask = (t: any) => {
      if (!t) return null;
      const descTrimmed = t.description.toLowerCase().trim();

      // 1. Remove tasks that are exactly the habit title (duplicates)
      if (habitMapByTitle.has(descTrimmed)) {
        return null;
      }

      // 2. Remove duplicate descriptions within the same response
      if (seenDescriptions.has(descTrimmed)) {
        return null;
      }
      seenDescriptions.add(descTrimmed);

      // 3. If already has habitId, keep it
      if (t.habitId) return t;

      // 4. Smart linking for "General" (Goal/Persona) tasks that mention a habit
      for (const habit of habits) {
        const titleLower = habit.title.toLowerCase().trim();
        // If the description contains the habit title, link it to that habit
        if (descTrimmed.includes(titleLower)) {
          return { ...t, habitId: habit.id, genre: 'habit' };
        }
      }
      return t;
    };

    const coreGoals = user.coreGoals.map(enrichTask).filter(t => t !== null);
    const dailyVariations = user.dailyVariations.map(enrichTask).filter(t => t !== null);

    let consistencyScore = 0.0;
    let goalHabitHistory: string[] = [];

    const engagedHabits = habits.filter(h => h.completionHistory.length > 0);
    if (engagedHabits.length > 0) {
      const totalScore = engagedHabits.reduce((acc, h) => acc + (h.consistencyScore || 0), 0);
      consistencyScore = totalScore / engagedHabits.length;
    }

    const activeGoal = await this.goalRepository.findActiveByUserId(userId);
    if (activeGoal) {
      const goalHabits = habits.filter((h) => h.goalId === activeGoal.id);
      goalHabitHistory = [...new Set(goalHabits.flatMap((h) => h.completionHistory))];
    }

    const achievements = await Promise.all(
      (user.achievements ?? []).map(async (a) => {
        const goal = await this.goalRepository.findById(a.goalId);
        return { goalId: a.goalId, goalTitle: goal?.title ?? 'Goal', medal: a.medal, awardedAt: a.awardedAt };
      }),
    );

    return {
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      profilePicture: user.profilePicture,
      goal: user.goal,
      personaType: user.personaType,
      motivationalMessage: user.motivationalMessage,
      coreGoals: coreGoals,
      dailyVariations: dailyVariations,
      portfolioSummary: user.portfolioSummary,
      tips: user.tips,
      failurePatterns: user.failurePatterns,
      confidenceScore: consistencyScore,
      completionHistory: goalHabitHistory,
      achievements,
      nameChangedAt: user.nameChangedAt,
      authProvider: user.authProvider,
      success: true,
    };
  }

  async updateProfilePicture(userId: string, profilePicture: string) {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    const updated = await this.userRepository.updateProfilePicture(userId, profilePicture);
    return { profilePicture: updated?.profilePicture ?? profilePicture, success: true };
  }

  async updateName(userId: string, firstName: string, lastName: string) {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    if (user.nameChangedAt) {
      const nextEligible = new Date(user.nameChangedAt);
      nextEligible.setMonth(nextEligible.getMonth() + NAME_CHANGE_COOLDOWN_MONTHS);
      if (nextEligible > new Date()) {
        throw new BadRequestException(
          `You can change your name again on ${nextEligible.toISOString().split('T')[0]}`,
        );
      }
    }

    const updated = await this.userRepository.updateName(userId, firstName, lastName);
    return {
      firstName: updated?.firstName ?? firstName,
      lastName: updated?.lastName ?? lastName,
      nameChangedAt: updated?.nameChangedAt,
      success: true,
    };
  }

  async changePassword(userId: string, currentPassword: string, newPassword: string) {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');
    if (user.authProvider === 'google') {
      throw new BadRequestException("Google accounts don't have a password to change");
    }

    const isCurrentPasswordValid = await bcrypt.compare(currentPassword, user.password);
    if (!isCurrentPasswordValid) throw new UnauthorizedException('Current password is incorrect');

    const hashedPassword = await bcrypt.hash(newPassword, 10);
    await this.userRepository.updatePassword(userId, hashedPassword);
    return { success: true };
  }

  async uploadAvatar(userId: string, file: Express.Multer.File) {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    const profilePicture = await this.storage.upload(file);
    // Replace an old uploaded avatar so orphaned files don't pile up. Preset keys
    // never reach storage, so they are safe to leave untouched.
    if (user.profilePicture?.startsWith('/uploads/')) {
      await this.storage.delete(user.profilePicture);
    }

    await this.userRepository.updateProfilePicture(userId, profilePicture);
    return { profilePicture, success: true };
  }
}

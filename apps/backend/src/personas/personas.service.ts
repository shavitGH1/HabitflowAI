import { v4 as uuidv4 } from 'uuid';
import { BadRequestException, Inject, Injectable, NotFoundException } from '@nestjs/common';
import { AiService } from '../ai/ai.service';
import { logger } from '../logger';
import { PERSONA_TYPES, PILLARS, Pillar, PersonaType } from '../ai/pillars';
import { BehaviorSnapshot } from '../ai/prompts/persona-drift-detector.prompt';
import { DriftResult } from '../ai/features/persona-drift-detector.feature';
import { FIREBASE_MESSAGING } from '../notifications/firebase.module';
import { DriftFlagRepository } from '../notifications/drift-flag.repository';
import { HabitData, HabitRepository } from '../habits/habit.repository';
import { UserData, UserRepository } from '../users/user.repository';
import { GoalData, GoalRepository } from '../goals/goal.repository';
import { GoalsService } from '../goals/goals.service';
import { GoalTask } from '../dto/goal.dto';
import { DriftCheckDto } from './dto/drift-check.dto';
import { ReclassifyDto } from './dto/reclassify.dto';
import { ConfirmCoachChangeDto } from './dto/confirm-coach-change.dto';
import { Messaging } from 'firebase-admin/messaging';

export type ConfirmCoachChangeResult =
  | { type: 'personaSwitch'; personaType: PersonaType }
  | { type: 'adjustDifficulty'; dailyVariations: GoalTask[] }
  | { type: 'forfeitGoal'; goal: GoalData };

@Injectable()
export class PersonasService {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly habitRepository: HabitRepository,
    private readonly goalRepository: GoalRepository,
    private readonly goalsService: GoalsService,
    private readonly ai: AiService,
    private readonly driftFlagRepository: DriftFlagRepository,
    @Inject(FIREBASE_MESSAGING) private readonly messaging: Messaging,
  ) {}

  async reclassify(userId: string, { goal, openAnswers }: ReclassifyDto) {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    const classification = await this.ai.classifyPersonaWeighted({ goal, openAnswers });
    if (!classification.isValid) {
      throw new BadRequestException(`Invalid input: ${classification.errorReason}`);
    }

    const { personaType, weightedBreakdown } = classification;

    await this.userRepository.updateUserPersona(userId, {
      goal,
      personaType,
      personaBreakdown: weightedBreakdown,
      weightedScores: weightedBreakdown,
    });

    return { personaType, weightedBreakdown, success: true };
  }

  async driftCheck(userId: string, overrides?: DriftCheckDto): Promise<DriftResult> {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');
    return this.evaluateDrift(user, overrides);
  }

  async evaluateAllUsersDrift(): Promise<void> {
    const users = await this.userRepository.findAllUsers();
    logger.info({ count: users.length }, 'weekly drift evaluation started');

    for (const user of users) {
      try {
        const result = await this.evaluateDrift(user);
        if (result.driftDetected) {
          logger.warn(
            { userId: user.id, driftScore: result.driftScore, suggested: result.newSuggestedPersona },
            'persona drift detected',
          );
          await this.driftFlagRepository.create({
            userId: user.id,
            detectedAt: new Date(),
            driftScore: result.driftScore,
            suggestedPersona: result.newSuggestedPersona ?? '',
          });
          if (user.fcmToken) {
            await this.messaging.send({
              token: user.fcmToken,
              notification: { body: 'Your habit style may be shifting — tap to check' },
              data: { screen: 'coach-chat' },
            });
          }
        }
      } catch (error) {
        logger.error({ userId: user.id, err: error }, 'drift evaluation failed for user');
      }
    }
  }

  private async evaluateDrift(user: UserData, overrides?: DriftCheckDto): Promise<DriftResult> {
    const currentPersona = this.toPersonaType(user.personaType);
    if (!currentPersona) {
      throw new BadRequestException(`User has an unknown persona: ${user.personaType}`);
    }

    return this.ai.detectDrift({
      currentPersona,
      baselineBreakdown: this.buildBaseline(user),
      behaviorSnapshot: await this.buildSnapshot(user, overrides),
    });
  }

  private buildBaseline(user: UserData): Record<Pillar, number> {
    const stored = user.weightedScores ?? user.personaBreakdown ?? {};
    const baseline = {} as Record<Pillar, number>;
    for (const pillar of PILLARS) {
      baseline[pillar] = stored[pillar] ?? 0;
    }
    return baseline;
  }

  private async buildSnapshot(user: UserData, overrides?: DriftCheckDto): Promise<BehaviorSnapshot> {
    const tasks = [...user.coreGoals, ...user.dailyVariations];
    const completed = tasks.filter((t) => t.completed);
    const skipped = tasks.filter((t) => !t.completed);
    const derivedRate = tasks.length ? completed.length / tasks.length : 0;
    const tally = this.ai.getFeedbackTally(user.id);
    const habits = await this.habitRepository.findByUserId(user.id);

    return {
      observationWindowDays: 7,
      recentCompletionRate: overrides?.recentCompletionRate ?? derivedRate,
      activeStreak: overrides?.activeStreak ?? this.activeStreak(habits),
      completedHabits: overrides?.completedHabits ?? completed.map((t) => t.description),
      skippedHabits: overrides?.skippedHabits ?? skipped.map((t) => t.description),
      positiveFeedbackCount: tally.positiveFeedbackCount,
      negativeFeedbackCount: tally.negativeFeedbackCount,
    };
  }

  async confirmCoachChange(userId: string, dto: ConfirmCoachChangeDto): Promise<ConfirmCoachChangeResult> {
    switch (dto.type) {
      case 'personaSwitch':
        return this.applyPersonaSwitch(userId, dto.suggestedPersona!);
      case 'adjustDifficulty':
        return this.applyDifficultyAdjustment(userId, dto.direction!);
      case 'forfeitGoal':
        return this.applyGoalForfeit(userId, dto.goalId!);
    }
  }

  private async applyPersonaSwitch(userId: string, suggestedPersona: PersonaType): Promise<ConfirmCoachChangeResult> {
    const updated = await this.userRepository.updateUserPersona(userId, { personaType: suggestedPersona });
    if (!updated) throw new NotFoundException('User not found');
    return { type: 'personaSwitch', personaType: suggestedPersona };
  }

  private async applyDifficultyAdjustment(
    userId: string,
    direction: 'increase' | 'decrease',
  ): Promise<ConfirmCoachChangeResult> {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    const newTasks = await this.ai.generateDailyVariations(user, new Date().getDay(), direction);
    const updatedTasks = newTasks.map((t) => ({ ...t, id: uuidv4(), completed: false }));
    await this.userRepository.updateUserDailyTasks(userId, updatedTasks);
    return { type: 'adjustDifficulty', dailyVariations: updatedTasks };
  }

  private async applyGoalForfeit(userId: string, goalId: string): Promise<ConfirmCoachChangeResult> {
    const goal = await this.goalsService.forfeitGoal(userId, goalId);
    return { type: 'forfeitGoal', goal };
  }

  private activeStreak(habits: HabitData[]): number {
    return habits.reduce((max, h) => Math.max(max, h.streak), 0);
  }

  private toPersonaType(value: string): PersonaType | null {
    return (PERSONA_TYPES as readonly string[]).includes(value) ? (value as PersonaType) : null;
  }
}

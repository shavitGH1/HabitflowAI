import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Habit, HabitDocument } from './schemas/habit.schema';
import { calculateStreak } from './utils/streak.utils';
import { calculateConsistencyScore, isImplemented } from './utils/consistency.utils';
import { logger } from '../logger';

export interface HabitData {
  id: string;
  userId: string;
  title: string;
  description: string;
  frequency: 'daily' | 'weekly';
  targetCount: number;
  streak: number;
  completionHistory: string[];
  persona: string;
  isArchived: boolean;
  goalId?: string;
  consistencyScore: number;
  implementedAt?: string;
  createdAt: string;
}

export interface CreateHabitInput {
  userId: string;
  title: string;
  description?: string;
  frequency: 'daily' | 'weekly';
  targetCount?: number;
  persona?: string;
  goalId?: string;
}

export interface UpdateHabitInput {
  title?: string;
  description?: string;
  frequency?: 'daily' | 'weekly';
  targetCount?: number;
  goalId?: string | null;
}

@Injectable()
export class HabitRepository {
  constructor(@InjectModel(Habit.name) private readonly habitModel: Model<HabitDocument>) {}

  async createHabit(input: CreateHabitInput): Promise<HabitData> {
    try {
      const doc = await this.habitModel.create(input);
      return this.toHabitData(doc);
    } catch (error) {
      logger.error({ err: error }, 'DB error in createHabit');
      throw error;
    }
  }

  async findByUserId(userId: string): Promise<HabitData[]> {
    const docs = await this.habitModel.find({ userId, isArchived: false }).sort({ createdAt: -1 });
    return docs.map(d => this.toHabitData(d));
  }

  async findById(id: string): Promise<HabitData | null> {
    const doc = await this.habitModel.findById(id);
    return doc ? this.toHabitData(doc) : null;
  }

  async updateHabit(id: string, updates: UpdateHabitInput): Promise<HabitData | null> {
    const doc = await this.habitModel.findByIdAndUpdate(id, updates, { returnDocument: 'after' });
    return doc ? this.toHabitData(doc) : null;
  }

  async deleteHabit(id: string): Promise<HabitData | null> {
    const doc = await this.habitModel.findByIdAndUpdate(id, { isArchived: true }, { returnDocument: 'after' });
    return doc ? this.toHabitData(doc) : null;
  }

  async completeHabit(id: string): Promise<HabitData | null> {
    const today = new Date().toISOString().split('T')[0];
    const doc = await this.habitModel.findById(id);
    if (!doc) return null;

    if (!doc.completionHistory.includes(today)) {
      doc.completionHistory.push(today);
    }

    doc.streak = calculateStreak(doc.completionHistory);

    const createdAt = (doc as unknown as { createdAt: Date }).createdAt.toISOString().split('T')[0];
    doc.consistencyScore = calculateConsistencyScore(doc.completionHistory, createdAt, today);

    if (!doc.implementedAt && isImplemented(doc.consistencyScore, createdAt, today)) {
      doc.implementedAt = new Date();
    }

    const saved = await doc.save();
    return this.toHabitData(saved);
  }

  private toHabitData(doc: HabitDocument): HabitData {
    return {
      id: doc._id.toString(),
      userId: doc.userId,
      title: doc.title,
      description: doc.description,
      frequency: doc.frequency,
      targetCount: doc.targetCount,
      streak: doc.streak,
      completionHistory: doc.completionHistory,
      persona: doc.persona,
      isArchived: doc.isArchived,
      goalId: doc.goalId ?? undefined,
      consistencyScore: doc.consistencyScore,
      implementedAt: doc.implementedAt?.toISOString(),
      createdAt: (doc as unknown as { createdAt: Date }).createdAt?.toISOString() ?? '',
    };
  }
}

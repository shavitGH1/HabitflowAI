import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Habit, HabitDocument } from './schemas/habit.schema';
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
  createdAt: string;
}

export interface CreateHabitInput {
  userId: string;
  title: string;
  description?: string;
  frequency: 'daily' | 'weekly';
  targetCount?: number;
  persona?: string;
}

export interface UpdateHabitInput {
  title?: string;
  description?: string;
  frequency?: 'daily' | 'weekly';
  targetCount?: number;
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
    const doc = await this.habitModel.findByIdAndUpdate(id, updates, { new: true });
    return doc ? this.toHabitData(doc) : null;
  }

  async deleteHabit(id: string): Promise<HabitData | null> {
    const doc = await this.habitModel.findByIdAndUpdate(id, { isArchived: true }, { new: true });
    return doc ? this.toHabitData(doc) : null;
  }

  async completeHabit(id: string): Promise<HabitData | null> {
    const today = new Date().toISOString().split('T')[0];
    const doc = await this.habitModel.findById(id);
    if (!doc) return null;

    if (!doc.completionHistory.includes(today)) {
      doc.completionHistory.push(today);
    }

    doc.streak = this.calculateStreak(doc.completionHistory);
    const saved = await doc.save();
    return this.toHabitData(saved);
  }

  private calculateStreak(history: string[]): number {
    const sorted = [...new Set(history)].sort().reverse();
    if (!sorted.length) return 0;

    let streak = 0;
    const cursor = new Date();
    cursor.setHours(0, 0, 0, 0);

    for (const dateStr of sorted) {
      const expected = cursor.toISOString().split('T')[0];
      if (dateStr === expected) {
        streak++;
        cursor.setDate(cursor.getDate() - 1);
      } else {
        break;
      }
    }

    return streak;
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
      createdAt: (doc as unknown as { createdAt: Date }).createdAt?.toISOString() ?? '',
    };
  }
}

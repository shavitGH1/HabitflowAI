import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Goal, GoalDocument, GoalStatus } from './schemas/goal.schema';
import { logger } from '../logger';

export interface GoalData {
  id: string;
  userId: string;
  title: string;
  targetDate: string;
  status: GoalStatus;
  createdAt: string;
}

export interface CreateGoalInput {
  userId: string;
  title: string;
  targetDate: Date;
}

@Injectable()
export class GoalRepository {
  constructor(@InjectModel(Goal.name) private readonly goalModel: Model<GoalDocument>) {}

  async createGoal(input: CreateGoalInput): Promise<GoalData> {
    try {
      const doc = await this.goalModel.create(input);
      return this.toGoalData(doc);
    } catch (error) {
      logger.error({ err: error }, 'DB error in createGoal');
      throw error;
    }
  }

  /**
   * Atomically returns the user's active goal, creating it from the given
   * fallback fields if none exists yet. Safe under concurrent callers: the
   * unique partial index on {userId, status: 'active'} means at most one
   * upsert can win, and everyone else's findOneAndUpdate just returns the
   * winner's doc instead of racing past it into a duplicate.
   */
  async findOrCreateActiveGoal(input: CreateGoalInput): Promise<GoalData> {
    const doc = await this.goalModel.findOneAndUpdate(
      { userId: input.userId, status: 'active' },
      { $setOnInsert: { userId: input.userId, title: input.title, targetDate: input.targetDate, status: 'active' } },
      { upsert: true, new: true, setDefaultsOnInsert: true },
    );
    return this.toGoalData(doc);
  }

  async findActiveByUserId(userId: string): Promise<GoalData | null> {
    const doc = await this.goalModel.findOne({ userId, status: 'active' });
    return doc ? this.toGoalData(doc) : null;
  }

  async findById(id: string): Promise<GoalData | null> {
    const doc = await this.goalModel.findById(id);
    return doc ? this.toGoalData(doc) : null;
  }

  async updateStatus(id: string, status: GoalStatus): Promise<GoalData | null> {
    const doc = await this.goalModel.findByIdAndUpdate(id, { status }, { returnDocument: 'after' });
    return doc ? this.toGoalData(doc) : null;
  }

  private toGoalData(doc: GoalDocument): GoalData {
    return {
      id: doc._id.toString(),
      userId: doc.userId,
      title: doc.title,
      targetDate: doc.targetDate.toISOString(),
      status: doc.status,
      createdAt: (doc as unknown as { createdAt: Date }).createdAt?.toISOString() ?? '',
    };
  }
}

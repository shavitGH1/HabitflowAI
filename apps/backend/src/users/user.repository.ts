import { Injectable } from '@nestjs/common';
import { logger } from '../logger';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { GoalTask } from '../dto/goal.dto';
import { User, UserDocument } from './schemas/user.schema';

export interface Achievement {
  goalId: string;
  medal: string;
  awardedAt: string;
}

export interface UserData {
  id: string;
  email: string;
  password: string;
  goal: string;
  personaType: string;
  motivationalMessage: string;
  coreGoals: GoalTask[];
  dailyVariations: GoalTask[];
  tasksLastGeneratedDate: string;
  refreshToken?: string;
  personaBreakdown?: Record<string, number>;
  weightedScores?: Record<string, number>;
  portfolioSummary?: string;
  tips?: string[];
  failurePatterns?: string[];
  confidenceScore?: number;
  fcmToken?: string;
  achievements?: Achievement[];
}

@Injectable()
export class UserRepository {
  constructor(@InjectModel(User.name) private readonly userModel: Model<UserDocument>) {}

  async saveUser(data: Omit<UserData, 'id'>): Promise<UserData> {
    try {
      const instance = new this.userModel();
      Object.assign(instance, data);
      // Mongoose 9 + TS: save() return type causes deep type inference — cast via unknown
      const doc = (await instance.save()) as unknown as UserDocument;
      return this.toUserData(doc);
    } catch (error) {
      logger.error({ err: error }, 'DB error in saveUser');
      throw error;
    }
  }

  async findUserById(id: string): Promise<UserData | null> {
    const doc = await this.userModel.findById(id);
    return doc ? this.toUserData(doc) : null;
  }

  async findUserByEmail(email: string): Promise<UserData | null> {
    const doc = await this.userModel.findOne({ email });
    return doc ? this.toUserData(doc) : null;
  }

  async findAllUsers(): Promise<UserData[]> {
    const docs = await this.userModel.find();
    return docs.map((doc) => this.toUserData(doc));
  }

  async findUserEmailsByIds(ids: string[]): Promise<{ id: string; email: string }[]> {
    const docs = await this.userModel
      .find({ _id: { $in: ids } })
      .select('_id email')
      .exec();
    return docs.map(doc => ({ id: (doc._id as unknown as string).toString(), email: doc.email }));
  }

  async updateUserDailyTasks(userId: string, newDailyTasks: GoalTask[]): Promise<UserData | null> {
    const doc = await this.userModel.findByIdAndUpdate(
      userId,
      {
        dailyVariations: newDailyTasks,
        tasksLastGeneratedDate: new Date().toISOString().split('T')[0],
      },
      { new: true },
    );
    return doc ? this.toUserData(doc) : null;
  }

  async updateUserRefreshToken(userId: string, refreshToken?: string): Promise<UserData | null> {
    const doc = await this.userModel.findByIdAndUpdate(
      userId,
      { refreshToken: refreshToken ?? null },
      { new: true },
    );
    return doc ? this.toUserData(doc) : null;
  }

  async updateUserFcmToken(userId: string, fcmToken: string): Promise<UserData | null> {
    const doc = await this.userModel.findByIdAndUpdate(
      userId,
      { fcmToken },
      { new: true },
    );
    return doc ? this.toUserData(doc) : null;
  }

  async updateUserPersona(
    userId: string,
    updates: Partial<Pick<UserData, 'goal' | 'personaType' | 'motivationalMessage' | 'coreGoals' | 'dailyVariations' | 'tasksLastGeneratedDate' | 'personaBreakdown' | 'weightedScores'>>,
  ): Promise<UserData | null> {
    const doc = await this.userModel.findByIdAndUpdate(userId, updates, { new: true });
    return doc ? this.toUserData(doc) : null;
  }

  async addAchievement(userId: string, achievement: Achievement): Promise<UserData | null> {
    const doc = await this.userModel.findByIdAndUpdate(
      userId,
      { $push: { achievements: achievement } },
      { new: true },
    );
    return doc ? this.toUserData(doc) : null;
  }

  async completeTask(userId: string, taskId: string): Promise<boolean> {
    const inCore = await this.userModel.findOneAndUpdate(
      { _id: userId, 'coreGoals.id': taskId },
      { $set: { 'coreGoals.$.completed': true } },
    );
    if (inCore) return true;

    const inDaily = await this.userModel.findOneAndUpdate(
      { _id: userId, 'dailyVariations.id': taskId },
      { $set: { 'dailyVariations.$.completed': true } },
    );
    return !!inDaily;
  }

  private toUserData(doc: UserDocument): UserData {
    return {
      id: doc._id.toString(),
      email: doc.email,
      password: doc.password,
      goal: doc.goal,
      personaType: doc.personaType,
      motivationalMessage: doc.motivationalMessage,
      coreGoals: doc.coreGoals.map(g => ({ id: g.id, description: g.description, points: g.points, completed: g.completed })),
      dailyVariations: doc.dailyVariations.map(g => ({ id: g.id, description: g.description, points: g.points, completed: g.completed })),
      tasksLastGeneratedDate: doc.tasksLastGeneratedDate,
      refreshToken: doc.refreshToken,
      personaBreakdown: doc.personaBreakdown,
      weightedScores: doc.weightedScores,
      portfolioSummary: doc.portfolioSummary,
      tips: doc.tips,
      failurePatterns: doc.failurePatterns,
      confidenceScore: doc.confidenceScore,
      fcmToken: doc.fcmToken,
      achievements: (doc.achievements ?? []).map(a => ({ goalId: a.goalId, medal: a.medal, awardedAt: a.awardedAt.toISOString() })),
    };
  }
}

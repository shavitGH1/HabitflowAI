import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { LeaderboardWeek, LeaderboardWeekDocument } from './schemas/leaderboard-week.schema';
import { LeaderboardMonth, LeaderboardMonthDocument } from './schemas/leaderboard-month.schema';
import { LeaderboardArchive, LeaderboardArchiveDocument } from './schemas/leaderboard-archive.schema';
import { DayProgress } from './utils/scoring.utils';

export interface WeekData {
  userId: string;
  weekStart: string;
  monthStart: string;
  weekPoints: number;
  days: DayProgress[];
  perfectWeekBonusAwarded: boolean;
}

export interface StandingData {
  userId: string;
  points: number;
}

export interface ArchivedStandingData extends StandingData {
  rank: number;
  medal?: string;
}

export interface ArchiveData {
  monthStart: string;
  standings: ArchivedStandingData[];
}

export interface RecordCompletionInput {
  userId: string;
  weekStart: string;
  monthStart: string;
  pointsDelta: number;
  day: DayProgress;
}

@Injectable()
export class LeaderboardRepository {
  constructor(
    @InjectModel(LeaderboardWeek.name) private readonly weekModel: Model<LeaderboardWeekDocument>,
    @InjectModel(LeaderboardMonth.name) private readonly monthModel: Model<LeaderboardMonthDocument>,
    @InjectModel(LeaderboardArchive.name) private readonly archiveModel: Model<LeaderboardArchiveDocument>,
  ) {}

  async findWeek(userId: string, weekStart: string): Promise<WeekData | null> {
    const doc = await this.weekModel.findOne({ userId, weekStart });
    return doc ? this.toWeekData(doc) : null;
  }

  async findWeeksByWeekStart(weekStart: string): Promise<WeekData[]> {
    const docs = await this.weekModel.find({ weekStart });
    return docs.map(d => this.toWeekData(d));
  }

  async recordCompletion(input: RecordCompletionInput): Promise<void> {
    const { userId, weekStart, monthStart, pointsDelta, day } = input;

    let week = await this.weekModel.findOne({ userId, weekStart });
    if (!week) {
      week = new this.weekModel({ userId, weekStart, monthStart, weekPoints: 0, days: [] });
    }
    week.weekPoints += pointsDelta;
    const existingDay = week.days.find(d => d.date === day.date);
    if (existingDay) {
      existingDay.completedCount = day.completedCount;
      existingDay.rosterSize = day.rosterSize;
      existingDay.halfBonusAwarded = day.halfBonusAwarded;
      existingDay.allBonusAwarded = day.allBonusAwarded;
    } else {
      week.days.push(day);
    }
    await week.save();

    await this.monthModel.findOneAndUpdate(
      { userId, monthStart },
      { $inc: { monthPoints: pointsDelta }, $setOnInsert: { userId, monthStart } },
      { upsert: true },
    );
  }

  async awardPerfectWeekBonus(userId: string, weekStart: string, monthStart: string, bonus: number): Promise<void> {
    await this.weekModel.updateOne(
      { userId, weekStart },
      { $inc: { weekPoints: bonus }, $set: { perfectWeekBonusAwarded: true } },
    );
    await this.monthModel.findOneAndUpdate(
      { userId, monthStart },
      { $inc: { monthPoints: bonus }, $setOnInsert: { userId, monthStart } },
      { upsert: true },
    );
  }

  async getMonthStandings(monthStart: string, limit?: number): Promise<StandingData[]> {
    let query = this.monthModel.find({ monthStart }).sort({ monthPoints: -1 });
    if (limit) query = query.limit(limit);
    const docs = await query;
    return docs.map(d => ({ userId: d.userId, points: d.monthPoints }));
  }

  async archiveMonth(monthStart: string, standings: ArchivedStandingData[]): Promise<void> {
    await this.archiveModel.findOneAndUpdate(
      { monthStart },
      { monthStart, standings },
      { upsert: true },
    );
  }

  async findArchive(monthStart?: string): Promise<ArchiveData[]> {
    const docs = monthStart
      ? await this.archiveModel.find({ monthStart })
      : await this.archiveModel.find().sort({ monthStart: -1 });
    return docs.map(d => ({
      monthStart: d.monthStart,
      standings: d.standings.map(s => ({ userId: s.userId, points: s.points, rank: s.rank, medal: s.medal })),
    }));
  }

  private toWeekData(doc: LeaderboardWeekDocument): WeekData {
    return {
      userId: doc.userId,
      weekStart: doc.weekStart,
      monthStart: doc.monthStart,
      weekPoints: doc.weekPoints,
      days: doc.days.map(d => ({
        date: d.date,
        completedCount: d.completedCount,
        rosterSize: d.rosterSize,
        halfBonusAwarded: d.halfBonusAwarded,
        allBonusAwarded: d.allBonusAwarded,
      })),
      perfectWeekBonusAwarded: doc.perfectWeekBonusAwarded,
    };
  }
}

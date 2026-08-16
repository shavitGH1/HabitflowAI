import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument, Types } from 'mongoose';

@Schema({ _id: false })
export class DayProgress {
  @Prop({ required: true })
  date: string;

  @Prop({ required: true, default: 0 })
  completedCount: number;

  @Prop({ required: true, default: 0 })
  rosterSize: number;

  @Prop({ default: false })
  halfBonusAwarded: boolean;

  @Prop({ default: false })
  allBonusAwarded: boolean;
}

const DayProgressSchema = SchemaFactory.createForClass(DayProgress);

// One document per (userId, weekStart) — live-updated on every completion.
// Never reset in place; a new week just gets a new document.
@Schema({ timestamps: true })
export class LeaderboardWeek {
  @Prop({ required: true })
  userId: string;

  @Prop({ required: true })
  weekStart: string;

  // Month this week's points are credited to — the month containing weekStart,
  // fixed at creation so a week spanning two months doesn't split its points.
  @Prop({ required: true })
  monthStart: string;

  @Prop({ required: true, default: 0 })
  weekPoints: number;

  @Prop({ type: [DayProgressSchema], default: [] })
  days: Types.DocumentArray<DayProgress>;

  @Prop({ default: false })
  perfectWeekBonusAwarded: boolean;
}

export type LeaderboardWeekDocument = HydratedDocument<LeaderboardWeek>;
export const LeaderboardWeekSchema = SchemaFactory.createForClass(LeaderboardWeek);
LeaderboardWeekSchema.index({ userId: 1, weekStart: 1 }, { unique: true });

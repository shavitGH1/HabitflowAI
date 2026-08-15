import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

// One document per (userId, monthStart) — a running total incremented in
// lockstep with LeaderboardWeek, not assembled by summing weeks at read time.
// Never reset in place; a new month just gets a new document.
@Schema({ timestamps: true })
export class LeaderboardMonth {
  @Prop({ required: true })
  userId: string;

  @Prop({ required: true })
  monthStart: string;

  @Prop({ required: true, default: 0 })
  monthPoints: number;
}

export type LeaderboardMonthDocument = HydratedDocument<LeaderboardMonth>;
export const LeaderboardMonthSchema = SchemaFactory.createForClass(LeaderboardMonth);
LeaderboardMonthSchema.index({ userId: 1, monthStart: 1 }, { unique: true });

import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument, Types } from 'mongoose';

@Schema({ _id: false })
export class ArchivedStanding {
  @Prop({ required: true })
  userId: string;

  @Prop({ required: true })
  points: number;

  @Prop({ required: true })
  rank: number;

  @Prop()
  medal?: string;
}

const ArchivedStandingSchema = SchemaFactory.createForClass(ArchivedStanding);

// Final ranked standings for one past month, written once by the monthly cron.
@Schema({ timestamps: true })
export class LeaderboardArchive {
  @Prop({ required: true, unique: true })
  monthStart: string;

  @Prop({ type: [ArchivedStandingSchema], default: [] })
  standings: Types.DocumentArray<ArchivedStanding>;
}

export type LeaderboardArchiveDocument = HydratedDocument<LeaderboardArchive>;
export const LeaderboardArchiveSchema = SchemaFactory.createForClass(LeaderboardArchive);

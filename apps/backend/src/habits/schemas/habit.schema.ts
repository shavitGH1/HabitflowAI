import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

@Schema({ timestamps: true })
export class Habit {
  @Prop({ required: true })
  userId: string;

  @Prop({ required: true })
  title: string;

  @Prop({ default: '' })
  description: string;

  @Prop({ required: true, enum: ['daily', 'weekly'] })
  frequency: 'daily' | 'weekly';

  @Prop({ default: 1 })
  targetCount: number;

  @Prop({ default: 0 })
  streak: number;

  @Prop({ type: [String], default: [] })
  completionHistory: string[];

  @Prop({ default: '' })
  persona: string;

  @Prop({ default: false })
  isArchived: boolean;
}

export const HabitSchema = SchemaFactory.createForClass(Habit);
HabitSchema.index({ userId: 1 });
HabitSchema.index({ createdAt: -1 });
export type HabitDocument = HydratedDocument<Habit>;

import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

export type GoalStatus = 'active' | 'achieved' | 'forfeited';

@Schema({ timestamps: true })
export class Goal {
  @Prop({ required: true })
  userId: string;

  @Prop({ required: true })
  title: string;

  @Prop({ required: true })
  targetDate: Date;

  @Prop({ required: true, enum: ['active', 'achieved', 'forfeited'], default: 'active' })
  status: GoalStatus;
}

export const GoalSchema = SchemaFactory.createForClass(Goal);
GoalSchema.index({ userId: 1, status: 1 });
GoalSchema.index({ userId: 1 }, { unique: true, partialFilterExpression: { status: 'active' } });
export type GoalDocument = HydratedDocument<Goal>;

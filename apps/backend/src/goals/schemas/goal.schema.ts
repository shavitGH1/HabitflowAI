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
// A user can only ever have one active goal at a time — enforced at the DB
// level so concurrent createGoal()/getActiveGoal() calls can't race past
// the application-level check and produce two "active" goals for one user.
GoalSchema.index({ userId: 1 }, { unique: true, partialFilterExpression: { status: 'active' } });
export type GoalDocument = HydratedDocument<Goal>;

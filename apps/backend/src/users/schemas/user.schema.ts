import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

@Schema({ _id: false })
class GoalTask {
  @Prop({ required: true })
  id: string;

  @Prop({ required: true })
  description: string;

  @Prop({ required: true, default: 0 })
  points: number;

  @Prop({ default: false })
  completed: boolean;
}

const GoalTaskSchema = SchemaFactory.createForClass(GoalTask);

@Schema({ timestamps: true })
export class User {
  @Prop({ required: true, unique: true })
  email: string;

  @Prop({ required: true })
  password: string;

  @Prop({ required: true })
  goal: string;

  @Prop({ required: true })
  personaType: string;

  @Prop({ default: '' })
  motivationalMessage: string;

  @Prop({ type: [GoalTaskSchema], default: [] })
  coreGoals: GoalTask[];

  @Prop({ type: [GoalTaskSchema], default: [] })
  dailyVariations: GoalTask[];

  @Prop({ required: true })
  tasksLastGeneratedDate: string;

  @Prop()
  refreshToken?: string;

  @Prop({ type: Object, default: {} })
  personaBreakdown: Record<string, number>;
}

export const UserSchema = SchemaFactory.createForClass(User);
export type UserDocument = HydratedDocument<User>;

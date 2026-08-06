import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

@Schema({ timestamps: true })
export class Chat {
  @Prop({ type: [String], required: true })
  participantIds: string[];

  @Prop({ default: false })
  isGroup: boolean;

  @Prop()
  name?: string;
}

export const ChatSchema = SchemaFactory.createForClass(Chat);
ChatSchema.index({ participantIds: 1 });
export type ChatDocument = HydratedDocument<Chat>;

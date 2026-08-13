import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

@Schema({ timestamps: true })
export class Chat {
  @Prop({ type: [String], required: true })
  participantIds: string[];

  @Prop({ default: false })
  isGroup: boolean;

  @Prop({ default: false })
  isPublic: boolean;

  @Prop()
  name?: string;

  @Prop({ type: [String], default: [] })
  admins: string[];

  @Prop()
  owner?: string;

  @Prop()
  description?: string;

  @Prop()
  imageUrl?: string;

  @Prop()
  lastMessage?: string;

  @Prop({ type: Map, of: Number, default: {} })
  unreadCount: Map<string, number>;
}

export const ChatSchema = SchemaFactory.createForClass(Chat);
ChatSchema.index({ participantIds: 1 });
export type ChatDocument = HydratedDocument<Chat>;

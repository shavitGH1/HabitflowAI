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

  // Drives chat-list ordering — deliberately separate from Mongoose's auto-managed
  // `updatedAt` (bumped by *any* update: marking read, pinning, muting), which would
  // otherwise reorder the whole list just from opening a chat. Only postMessage() sets this.
  @Prop()
  lastMessageAt?: Date;

  @Prop()
  lastMessageSenderId?: string;

  @Prop({ type: Map, of: Number, default: {} })
  unreadCount: Map<string, number>;

  // Up to 3 pinned chats per user (enforced in ChatService.togglePin, not here) float
  // to the top of that user's own chat list; muted chats just suppress notifications.
  // Scoped by userId directly on Chat, matching unreadCount's existing pattern.
  @Prop({ type: [String], default: [] })
  pinnedBy: string[];

  @Prop({ type: [String], default: [] })
  mutedBy: string[];
}

export const ChatSchema = SchemaFactory.createForClass(Chat);
ChatSchema.index({ participantIds: 1 });
export type ChatDocument = HydratedDocument<Chat>;

import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

@Schema({ timestamps: true })
export class Message {
  @Prop({ required: true })
  chatId: string;

  @Prop({ required: true })
  senderId: string;

  @Prop()
  text?: string;

  @Prop()
  imageUrl?: string;
}

export const MessageSchema = SchemaFactory.createForClass(Message);
MessageSchema.index({ chatId: 1, createdAt: -1 });
export type MessageDocument = HydratedDocument<Message>;

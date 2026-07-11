import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

@Schema({ timestamps: true })
export class Post {
  @Prop({ required: true })
  authorId: string;

  @Prop({ required: true })
  habitName: string;

  @Prop({ required: true })
  completionNote: string;

  @Prop()
  imageUrl?: string;

  @Prop({ default: 0 })
  likeCount: number;
}

export const PostSchema = SchemaFactory.createForClass(Post);
PostSchema.index({ authorId: 1 });
PostSchema.index({ createdAt: -1 });
export type PostDocument = HydratedDocument<Post>;

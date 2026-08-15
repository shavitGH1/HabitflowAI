import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument, Schema as MongooseSchema } from 'mongoose';
import { User } from '../../users/schemas/user.schema';

@Schema({ timestamps: true })
export class Post {
  @Prop({ type: MongooseSchema.Types.ObjectId, ref: 'User', required: true })
  authorId: MongooseSchema.Types.ObjectId;

  @Prop({ required: true })
  habitName: string;

  @Prop({ required: true })
  completionNote: string;

  @Prop()
  imageUrl?: string;

  @Prop({ type: [String], default: [] })
  likes: string[];
}

export const PostSchema = SchemaFactory.createForClass(Post);
PostSchema.index({ authorId: 1 });
PostSchema.index({ createdAt: -1 });
export type PostDocument = HydratedDocument<Post>;

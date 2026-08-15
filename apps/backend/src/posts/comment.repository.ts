import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Comment, CommentDocument } from './schemas/comment.schema';

export interface CommentData {
  id: string;
  postId: string;
  userId: string;
  userEmail?: string;
  userName?: string;
  text: string;
  createdAt: string;
}

export interface CreateCommentInput {
  postId: string;
  userId: string;
  text: string;
}

@Injectable()
export class CommentRepository {
  constructor(@InjectModel(Comment.name) private readonly commentModel: Model<CommentDocument>) {}

  async create(input: CreateCommentInput): Promise<CommentData> {
    const saved = await new this.commentModel({
        ...input,
        postId: new Types.ObjectId(input.postId),
        userId: new Types.ObjectId(input.userId)
    }).save();
    return (await this.findById(saved._id.toString()))!;
  }

  async findByPostId(postId: string): Promise<CommentData[]> {
    const comments = await this.commentModel.aggregate([
      { $match: { postId: new Types.ObjectId(postId) } },
      { $sort: { createdAt: 1 } },
      {
        $lookup: {
          from: 'users',
          localField: 'userId',
          foreignField: '_id',
          as: 'user'
        }
      },
      { $unwind: { path: '$user', preserveNullAndEmptyArrays: true } }
    ]).exec();

    return comments.map(c => this.fromAggregateToCommentData(c));
  }

  async findById(id: string): Promise<CommentData | null> {
    const comments = await this.commentModel.aggregate([
      { $match: { _id: new Types.ObjectId(id) } },
      {
        $lookup: {
          from: 'users',
          localField: 'userId',
          foreignField: '_id',
          as: 'user'
        }
      },
      { $unwind: { path: '$user', preserveNullAndEmptyArrays: true } }
    ]).exec();

    return comments.length > 0 ? this.fromAggregateToCommentData(comments[0]) : null;
  }

  async updateText(id: string, text: string): Promise<CommentData | null> {
    await this.commentModel.findByIdAndUpdate(id, { text }).exec();
    return this.findById(id);
  }

  async delete(id: string): Promise<void> {
    await this.commentModel.findByIdAndDelete(id).exec();
  }

  private fromAggregateToCommentData(doc: any): CommentData {
    return {
      id: doc._id.toString(),
      postId: doc.postId.toString(),
      userId: doc.userId.toString(),
      userEmail: doc.user?.email,
      userName: [doc.user?.firstName, doc.user?.lastName].filter(Boolean).join(' ')
        || doc.user?.email?.split('@')[0],
      text: doc.text,
      createdAt: doc.createdAt.toISOString(),
    };
  }
}

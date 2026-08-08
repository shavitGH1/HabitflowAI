import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Comment, CommentDocument } from './schemas/comment.schema';

export interface CommentData {
  id: string;
  postId: string;
  userId: string;
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
    const saved = await new this.commentModel(input).save();
    return this.toCommentData(saved);
  }

  async findByPostId(postId: string): Promise<CommentData[]> {
    const comments = await this.commentModel.find({ postId }).sort({ createdAt: 1 }).exec();
    return comments.map(doc => this.toCommentData(doc));
  }

  async findById(id: string): Promise<CommentData | null> {
    const comment = await this.commentModel.findById(id).exec();
    return comment ? this.toCommentData(comment) : null;
  }

  async updateText(id: string, text: string): Promise<CommentData | null> {
    const doc = await this.commentModel.findByIdAndUpdate(id, { text }, { returnDocument: 'after' }).exec();
    return doc ? this.toCommentData(doc) : null;
  }

  async delete(id: string): Promise<void> {
    await this.commentModel.findByIdAndDelete(id).exec();
  }

  private toCommentData(doc: CommentDocument): CommentData {
    return {
      id: (doc._id as { toString(): string }).toString(),
      postId: doc.postId,
      userId: doc.userId,
      text: doc.text,
      createdAt: (doc as unknown as { createdAt: Date }).createdAt.toISOString(),
    };
  }
}

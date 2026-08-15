import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Post, PostDocument } from './schemas/post.schema';

export interface PostData {
  id: string;
  authorId: string;
  authorEmail?: string;
  authorName?: string;
  habitName: string;
  completionNote: string;
  imageUrl?: string;
  likes: string[];
  createdAt: string;
}

export interface CreatePostInput {
  authorId: string;
  habitName: string;
  completionNote: string;
  imageUrl?: string;
}

@Injectable()
export class PostRepository {
  constructor(@InjectModel(Post.name) private readonly postModel: Model<PostDocument>) {}

  async createPost(input: CreatePostInput): Promise<PostData> {
    const saved = await new this.postModel({
        ...input,
        authorId: new Types.ObjectId(input.authorId)
    }).save();
    return (await this.findById(saved._id.toString()))!;
  }

  async findPaginated(page: number, limit: number): Promise<PostData[]> {
    const posts = await this.postModel.aggregate([
      { $sort: { createdAt: -1 } },
      { $skip: (page - 1) * limit },
      { $limit: limit },
      {
        $lookup: {
          from: 'users',
          localField: 'authorId',
          foreignField: '_id',
          as: 'author'
        }
      },
      { $unwind: { path: '$author', preserveNullAndEmptyArrays: true } }
    ]).exec();

    return posts.map(p => this.fromAggregateToPostData(p));
  }

  async findByAuthorId(authorId: string): Promise<PostData[]> {
    const posts = await this.postModel.aggregate([
      { $match: { authorId: new Types.ObjectId(authorId) } },
      { $sort: { createdAt: -1 } },
      {
        $lookup: {
          from: 'users',
          localField: 'authorId',
          foreignField: '_id',
          as: 'author'
        }
      },
      { $unwind: { path: '$author', preserveNullAndEmptyArrays: true } }
    ]).exec();

    return posts.map(p => this.fromAggregateToPostData(p));
  }

  async findPaginatedByAuthorIds(authorIds: string[], page: number, limit: number): Promise<PostData[]> {
    const objectIds = authorIds.map(id => new Types.ObjectId(id));
    const posts = await this.postModel.aggregate([
      { $match: { authorId: { $in: objectIds } } },
      { $sort: { createdAt: -1 } },
      { $skip: (page - 1) * limit },
      { $limit: limit },
      {
        $lookup: {
          from: 'users',
          localField: 'authorId',
          foreignField: '_id',
          as: 'author'
        }
      },
      { $unwind: { path: '$author', preserveNullAndEmptyArrays: true } }
    ]).exec();

    return posts.map(p => this.fromAggregateToPostData(p));
  }

  async findById(id: string): Promise<PostData | null> {
    const posts = await this.postModel.aggregate([
      { $match: { _id: new Types.ObjectId(id) } },
      {
        $lookup: {
          from: 'users',
          localField: 'authorId',
          foreignField: '_id',
          as: 'author'
        }
      },
      { $unwind: { path: '$author', preserveNullAndEmptyArrays: true } }
    ]).exec();

    return posts.length > 0 ? this.fromAggregateToPostData(posts[0]) : null;
  }

  async setLikes(id: string, likes: string[]): Promise<PostData | null> {
    await this.postModel.findByIdAndUpdate(id, { likes }).exec();
    return this.findById(id);
  }

  async deletePost(id: string): Promise<void> {
    await this.postModel.findByIdAndDelete(id).exec();
  }

  private fromAggregateToPostData(doc: any): PostData {
    return {
      id: doc._id.toString(),
      authorId: doc.authorId.toString(),
      authorEmail: doc.author?.email,
      authorName: doc.author?.email ? doc.author.email.split('@')[0] : undefined,
      habitName: doc.habitName,
      completionNote: doc.completionNote,
      imageUrl: doc.imageUrl,
      likes: doc.likes || [],
      createdAt: (doc.createdAt as Date)?.toISOString() || new Date().toISOString(),
    };
  }
}

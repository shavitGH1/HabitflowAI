import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Post, PostDocument } from './schemas/post.schema';

export interface PostData {
  id: string;
  authorId: string;
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
    const saved = await new this.postModel(input).save();
    return this.toPostData(saved);
  }

  async findPaginated(page: number, limit: number): Promise<PostData[]> {
    const posts = await this.postModel
      .find()
      .sort({ createdAt: -1 })
      .skip((page - 1) * limit)
      .limit(limit)
      .exec();
    return posts.map(doc => this.toPostData(doc));
  }

  async findByAuthorId(authorId: string): Promise<PostData[]> {
    const posts = await this.postModel.find({ authorId }).sort({ createdAt: -1 }).exec();
    return posts.map(doc => this.toPostData(doc));
  }

  async findPaginatedByAuthorIds(authorIds: string[], page: number, limit: number): Promise<PostData[]> {
    const posts = await this.postModel
      .find({ authorId: { $in: authorIds } })
      .sort({ createdAt: -1 })
      .skip((page - 1) * limit)
      .limit(limit)
      .exec();
    return posts.map(doc => this.toPostData(doc));
  }

  async findById(id: string): Promise<PostData | null> {
    const post = await this.postModel.findById(id).exec();
    return post ? this.toPostData(post) : null;
  }

  async setLikes(id: string, likes: string[]): Promise<PostData | null> {
    const doc = await this.postModel.findByIdAndUpdate(id, { likes }, { returnDocument: 'after' }).exec();
    return doc ? this.toPostData(doc) : null;
  }

  async deletePost(id: string): Promise<void> {
    await this.postModel.findByIdAndDelete(id).exec();
  }

  private toPostData(doc: PostDocument): PostData {
    return {
      id: (doc._id as { toString(): string }).toString(),
      authorId: doc.authorId,
      habitName: doc.habitName,
      completionNote: doc.completionNote,
      imageUrl: doc.imageUrl,
      likes: doc.likes,
      createdAt: (doc as unknown as { createdAt: Date }).createdAt.toISOString(),
    };
  }
}

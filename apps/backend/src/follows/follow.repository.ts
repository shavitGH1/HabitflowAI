import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Follow, FollowDocument } from './schemas/follow.schema';

@Injectable()
export class FollowRepository {
  constructor(@InjectModel(Follow.name) private readonly followModel: Model<FollowDocument>) {}

  async follow(followerId: string, followingId: string): Promise<void> {
    await this.followModel.updateOne(
      { followerId, followingId },
      { $setOnInsert: { followerId, followingId } },
      { upsert: true },
    );
  }

  async unfollow(followerId: string, followingId: string): Promise<void> {
    await this.followModel.deleteOne({ followerId, followingId });
  }

  async findFollowerIds(followingId: string): Promise<string[]> {
    const docs = await this.followModel.find({ followingId }).exec();
    return docs.map(doc => doc.followerId);
  }

  async findFollowingIds(followerId: string): Promise<string[]> {
    const docs = await this.followModel.find({ followerId }).exec();
    return docs.map(doc => doc.followingId);
  }
}

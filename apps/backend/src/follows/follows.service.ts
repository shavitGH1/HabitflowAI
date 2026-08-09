import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { FollowRepository } from './follow.repository';
import { UserRepository } from '../users/user.repository';

@Injectable()
export class FollowsService {
  constructor(
    private readonly followRepository: FollowRepository,
    private readonly userRepository: UserRepository,
  ) {}

  async follow(followerId: string, followingId: string): Promise<void> {
    if (followerId === followingId) throw new BadRequestException('You cannot follow yourself');
    const target = await this.userRepository.findUserById(followingId);
    if (!target) throw new NotFoundException('User not found');
    await this.followRepository.follow(followerId, followingId);
  }

  async unfollow(followerId: string, followingId: string): Promise<void> {
    await this.followRepository.unfollow(followerId, followingId);
  }

  async getFollowers(userId: string): Promise<string[]> {
    return this.followRepository.findFollowerIds(userId);
  }

  async getFollowing(userId: string): Promise<string[]> {
    return this.followRepository.findFollowingIds(userId);
  }
}

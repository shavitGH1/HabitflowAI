import { ForbiddenException, Inject, Injectable, NotFoundException } from '@nestjs/common';
import { CreatePostDto } from './dto/create-post.dto';
import { PostData, PostRepository } from './post.repository';
import { IStorageAdapter, STORAGE_ADAPTER } from '../storage/storage.adapter';
import { FollowRepository } from '../follows/follow.repository';

@Injectable()
export class PostsService {
  constructor(
    private readonly postRepository: PostRepository,
    private readonly followRepository: FollowRepository,
    @Inject(STORAGE_ADAPTER) private readonly storage: IStorageAdapter,
  ) {}

  async createPost(
    authorId: string,
    dto: CreatePostDto,
    file?: Express.Multer.File,
  ): Promise<PostData> {
    let imageUrl: string | undefined;
    if (file) {
      imageUrl = await this.storage.upload(file);
    }

    return this.postRepository.createPost({
      authorId,
      habitName: dto.habitName,
      completionNote: dto.completionNote,
      imageUrl,
    });
  }

  async getFeed(userId: string, page: number, limit: number, friendsOnly: boolean): Promise<PostData[]> {
    if (!friendsOnly) return this.postRepository.findPaginated(page, limit);

    const followingIds = await this.followRepository.findFollowingIds(userId);
    if (followingIds.length === 0) return [];
    return this.postRepository.findPaginatedByAuthorIds(followingIds, page, limit);
  }

  async getMyPosts(authorId: string): Promise<PostData[]> {
    return this.postRepository.findByAuthorId(authorId);
  }

  async deletePost(authorId: string, id: string): Promise<void> {
    const post = await this.postRepository.findById(id);
    if (!post) throw new NotFoundException('Post not found');
    if (post.authorId !== authorId) throw new ForbiddenException();
    if (post.imageUrl) {
      await this.storage.delete(post.imageUrl);
    }
    await this.postRepository.deletePost(id);
  }

  async likePost(userId: string, id: string): Promise<PostData> {
    const post = await this.getPostOrThrow(id);
    if (post.likes.includes(userId)) return post;
    return (await this.postRepository.setLikes(id, [...post.likes, userId]))!;
  }

  async unlikePost(userId: string, id: string): Promise<PostData> {
    const post = await this.getPostOrThrow(id);
    if (!post.likes.includes(userId)) return post;
    return (await this.postRepository.setLikes(id, post.likes.filter(likeId => likeId !== userId)))!;
  }

  private async getPostOrThrow(id: string): Promise<PostData> {
    const post = await this.postRepository.findById(id);
    if (!post) throw new NotFoundException('Post not found');
    return post;
  }
}

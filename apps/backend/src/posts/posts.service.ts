import { ForbiddenException, Inject, Injectable, NotFoundException } from '@nestjs/common';
import { CreatePostDto } from './dto/create-post.dto';
import { PostData, PostRepository } from './post.repository';
import { IStorageAdapter, STORAGE_ADAPTER } from '../storage/storage.adapter';

@Injectable()
export class PostsService {
  constructor(
    private readonly postRepository: PostRepository,
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

  async getFeed(page: number, limit: number): Promise<PostData[]> {
    return this.postRepository.findPaginated(page, limit);
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
}

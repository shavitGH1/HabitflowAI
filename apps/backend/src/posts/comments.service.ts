import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { CommentData, CommentRepository } from './comment.repository';
import { PostRepository } from './post.repository';
import { CreateCommentDto } from './dto/create-comment.dto';

@Injectable()
export class CommentsService {
  constructor(
    private readonly commentRepository: CommentRepository,
    private readonly postRepository: PostRepository,
  ) {}

  async addComment(userId: string, postId: string, dto: CreateCommentDto): Promise<CommentData> {
    const post = await this.postRepository.findById(postId);
    if (!post) throw new NotFoundException('Post not found');
    return this.commentRepository.create({ postId, userId, text: dto.text });
  }

  async getComments(postId: string): Promise<CommentData[]> {
    return this.commentRepository.findByPostId(postId);
  }

  async updateComment(userId: string, id: string, dto: CreateCommentDto): Promise<CommentData> {
    await this.getOwnedComment(userId, id);
    return (await this.commentRepository.updateText(id, dto.text))!;
  }

  async deleteComment(userId: string, id: string): Promise<void> {
    await this.getOwnedComment(userId, id);
    await this.commentRepository.delete(id);
  }

  private async getOwnedComment(userId: string, id: string): Promise<CommentData> {
    const comment = await this.commentRepository.findById(id);
    if (!comment) throw new NotFoundException('Comment not found');
    if (comment.userId !== userId) throw new ForbiddenException();
    return comment;
  }

  async likeComment(userId: string, id: string): Promise<CommentData> {
    const comment = await this.getCommentOrThrow(id);
    if (comment.likes.includes(userId)) return comment;
    return (await this.commentRepository.setLikes(id, [...comment.likes, userId]))!;
  }

  async unlikeComment(userId: string, id: string): Promise<CommentData> {
    const comment = await this.getCommentOrThrow(id);
    if (!comment.likes.includes(userId)) return comment;
    return (await this.commentRepository.setLikes(id, comment.likes.filter(likeId => likeId !== userId)))!;
  }

  private async getCommentOrThrow(id: string): Promise<CommentData> {
    const comment = await this.commentRepository.findById(id);
    if (!comment) throw new NotFoundException('Comment not found');
    return comment;
  }
}

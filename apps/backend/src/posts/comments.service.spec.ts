import { ForbiddenException, NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { CommentData, CommentRepository } from './comment.repository';
import { PostData, PostRepository } from './post.repository';
import { CommentsService } from './comments.service';

const mockCommentRepository = {
  create: jest.fn(),
  findByPostId: jest.fn(),
  findById: jest.fn(),
  updateText: jest.fn(),
  delete: jest.fn(),
};

const mockPostRepository = {
  findById: jest.fn(),
};

const USER_ID = 'user-123';
const OTHER_USER_ID = 'user-999';
const POST_ID = 'post-abc';
const COMMENT_ID = 'comment-abc';

const makePost = (overrides: Partial<PostData> = {}): PostData => ({
  id: POST_ID,
  authorId: USER_ID,
  habitName: 'Morning Run',
  completionNote: 'Done!',
  likes: [],
  createdAt: new Date().toISOString(),
  ...overrides,
});

const makeComment = (overrides: Partial<CommentData> = {}): CommentData => ({
  id: COMMENT_ID,
  postId: POST_ID,
  userId: USER_ID,
  text: 'Nice work!',
  createdAt: new Date().toISOString(),
  ...overrides,
});

describe('CommentsService', () => {
  let service: CommentsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        CommentsService,
        { provide: CommentRepository, useValue: mockCommentRepository },
        { provide: PostRepository, useValue: mockPostRepository },
      ],
    }).compile();

    service = module.get<CommentsService>(CommentsService);
    jest.clearAllMocks();
  });

  describe('addComment()', () => {
    it('throws NotFoundException when the post does not exist', async () => {
      mockPostRepository.findById.mockResolvedValue(null);

      await expect(service.addComment(USER_ID, POST_ID, { text: 'hi' })).rejects.toThrow(NotFoundException);
    });

    it('creates the comment when the post exists', async () => {
      const created = makeComment();
      mockPostRepository.findById.mockResolvedValue(makePost());
      mockCommentRepository.create.mockResolvedValue(created);

      const result = await service.addComment(USER_ID, POST_ID, { text: 'Nice work!' });

      expect(mockCommentRepository.create).toHaveBeenCalledWith({ postId: POST_ID, userId: USER_ID, text: 'Nice work!' });
      expect(result).toEqual(created);
    });
  });

  describe('updateComment()', () => {
    it('throws NotFoundException when the comment does not exist', async () => {
      mockCommentRepository.findById.mockResolvedValue(null);

      await expect(service.updateComment(USER_ID, COMMENT_ID, { text: 'edit' })).rejects.toThrow(NotFoundException);
    });

    it('throws ForbiddenException when the comment belongs to a different user', async () => {
      mockCommentRepository.findById.mockResolvedValue(makeComment({ userId: OTHER_USER_ID }));

      await expect(service.updateComment(USER_ID, COMMENT_ID, { text: 'edit' })).rejects.toThrow(ForbiddenException);
    });

    it('updates the text when the user owns the comment', async () => {
      mockCommentRepository.findById.mockResolvedValue(makeComment());
      mockCommentRepository.updateText.mockResolvedValue(makeComment({ text: 'edited' }));

      const result = await service.updateComment(USER_ID, COMMENT_ID, { text: 'edited' });

      expect(mockCommentRepository.updateText).toHaveBeenCalledWith(COMMENT_ID, 'edited');
      expect(result.text).toBe('edited');
    });
  });

  describe('deleteComment()', () => {
    it('throws ForbiddenException when the comment belongs to a different user', async () => {
      mockCommentRepository.findById.mockResolvedValue(makeComment({ userId: OTHER_USER_ID }));

      await expect(service.deleteComment(USER_ID, COMMENT_ID)).rejects.toThrow(ForbiddenException);
      expect(mockCommentRepository.delete).not.toHaveBeenCalled();
    });

    it('deletes the comment when the user owns it', async () => {
      mockCommentRepository.findById.mockResolvedValue(makeComment());

      await service.deleteComment(USER_ID, COMMENT_ID);

      expect(mockCommentRepository.delete).toHaveBeenCalledWith(COMMENT_ID);
    });
  });
});

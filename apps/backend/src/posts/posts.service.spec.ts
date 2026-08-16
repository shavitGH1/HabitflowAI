import { NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { PostData, PostRepository } from './post.repository';
import { FollowRepository } from '../follows/follow.repository';
import { STORAGE_ADAPTER } from '../storage/storage.adapter';
import { PostsService } from './posts.service';

const mockPostRepository = {
  findPaginated: jest.fn(),
  findPaginatedByAuthorIds: jest.fn(),
  findById: jest.fn(),
  setLikes: jest.fn(),
};

const mockFollowRepository = {
  findFollowingIds: jest.fn(),
};

const mockStorage = {
  upload: jest.fn(),
  delete: jest.fn(),
};

const USER_ID = 'user-123';
const OTHER_USER_ID = 'user-999';
const POST_ID = 'post-abc';

const makePost = (overrides: Partial<PostData> = {}): PostData => ({
  id: POST_ID,
  authorId: OTHER_USER_ID,
  habitName: 'Morning Run',
  completionNote: 'Done!',
  likes: [],
  commentCount: 0,
  createdAt: new Date().toISOString(),
  ...overrides,
});

describe('PostsService', () => {
  let service: PostsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        PostsService,
        { provide: PostRepository, useValue: mockPostRepository },
        { provide: FollowRepository, useValue: mockFollowRepository },
        { provide: STORAGE_ADAPTER, useValue: mockStorage },
      ],
    }).compile();

    service = module.get<PostsService>(PostsService);
    jest.clearAllMocks();
  });

  describe('likePost()', () => {
    it('throws NotFoundException when the post does not exist', async () => {
      mockPostRepository.findById.mockResolvedValue(null);

      await expect(service.likePost(USER_ID, POST_ID)).rejects.toThrow(NotFoundException);
    });

    it('adds the user to likes when not already liked', async () => {
      mockPostRepository.findById.mockResolvedValue(makePost({ likes: [] }));
      mockPostRepository.setLikes.mockResolvedValue(makePost({ likes: [USER_ID] }));

      const result = await service.likePost(USER_ID, POST_ID);

      expect(mockPostRepository.setLikes).toHaveBeenCalledWith(POST_ID, [USER_ID]);
      expect(result.likes).toEqual([USER_ID]);
    });

    it('is idempotent — liking twice does not duplicate the userId', async () => {
      mockPostRepository.findById.mockResolvedValue(makePost({ likes: [USER_ID] }));

      const result = await service.likePost(USER_ID, POST_ID);

      expect(mockPostRepository.setLikes).not.toHaveBeenCalled();
      expect(result.likes).toEqual([USER_ID]);
    });
  });

  describe('unlikePost()', () => {
    it('removes the user from likes when present', async () => {
      mockPostRepository.findById.mockResolvedValue(makePost({ likes: [USER_ID, OTHER_USER_ID] }));
      mockPostRepository.setLikes.mockResolvedValue(makePost({ likes: [OTHER_USER_ID] }));

      const result = await service.unlikePost(USER_ID, POST_ID);

      expect(mockPostRepository.setLikes).toHaveBeenCalledWith(POST_ID, [OTHER_USER_ID]);
      expect(result.likes).toEqual([OTHER_USER_ID]);
    });

    it('is idempotent — unliking a post the user never liked is a no-op', async () => {
      mockPostRepository.findById.mockResolvedValue(makePost({ likes: [] }));

      const result = await service.unlikePost(USER_ID, POST_ID);

      expect(mockPostRepository.setLikes).not.toHaveBeenCalled();
      expect(result.likes).toEqual([]);
    });
  });

  describe('getFeed()', () => {
    it('returns the global paginated feed when friendsOnly is false', async () => {
      mockPostRepository.findPaginated.mockResolvedValue([makePost()]);

      await service.getFeed(USER_ID, 1, 20, false);

      expect(mockPostRepository.findPaginated).toHaveBeenCalledWith(1, 20);
      expect(mockFollowRepository.findFollowingIds).not.toHaveBeenCalled();
    });

    it('filters to followed authors when friendsOnly is true', async () => {
      const posts = [makePost()];
      mockFollowRepository.findFollowingIds.mockResolvedValue([OTHER_USER_ID]);
      mockPostRepository.findPaginatedByAuthorIds.mockResolvedValue(posts);

      const result = await service.getFeed(USER_ID, 1, 20, true);

      expect(mockPostRepository.findPaginatedByAuthorIds).toHaveBeenCalledWith([OTHER_USER_ID], 1, 20);
      expect(result).toEqual(posts);
    });

    it('short-circuits to an empty feed when the user follows nobody', async () => {
      mockFollowRepository.findFollowingIds.mockResolvedValue([]);

      const result = await service.getFeed(USER_ID, 1, 20, true);

      expect(mockPostRepository.findPaginatedByAuthorIds).not.toHaveBeenCalled();
      expect(result).toEqual([]);
    });
  });
});

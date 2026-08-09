import { BadRequestException, NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { FollowRepository } from './follow.repository';
import { UserData, UserRepository } from '../users/user.repository';
import { FollowsService } from './follows.service';

const mockFollowRepository = {
  follow: jest.fn(),
  unfollow: jest.fn(),
  findFollowerIds: jest.fn(),
  findFollowingIds: jest.fn(),
};

const mockUserRepository = {
  findUserById: jest.fn(),
};

const USER_ID = 'user-123';
const TARGET_ID = 'user-999';

const makeUser = (overrides: Partial<UserData> = {}): UserData => ({
  id: TARGET_ID,
  email: 'target@example.com',
  password: 'hashed',
  goal: 'Stay consistent',
  personaType: 'Achiever',
  motivationalMessage: 'Keep going',
  coreGoals: [],
  dailyVariations: [],
  tasksLastGeneratedDate: '',
  ...overrides,
});

describe('FollowsService', () => {
  let service: FollowsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        FollowsService,
        { provide: FollowRepository, useValue: mockFollowRepository },
        { provide: UserRepository, useValue: mockUserRepository },
      ],
    }).compile();

    service = module.get<FollowsService>(FollowsService);
    jest.clearAllMocks();
  });

  describe('follow()', () => {
    it('throws BadRequestException when following yourself', async () => {
      await expect(service.follow(USER_ID, USER_ID)).rejects.toThrow(BadRequestException);
      expect(mockFollowRepository.follow).not.toHaveBeenCalled();
    });

    it('throws NotFoundException when the target user does not exist', async () => {
      mockUserRepository.findUserById.mockResolvedValue(null);

      await expect(service.follow(USER_ID, TARGET_ID)).rejects.toThrow(NotFoundException);
    });

    it('creates the follow relationship when the target exists', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser());

      await service.follow(USER_ID, TARGET_ID);

      expect(mockFollowRepository.follow).toHaveBeenCalledWith(USER_ID, TARGET_ID);
    });
  });

  describe('unfollow()', () => {
    it('delegates straight to the repository', async () => {
      await service.unfollow(USER_ID, TARGET_ID);

      expect(mockFollowRepository.unfollow).toHaveBeenCalledWith(USER_ID, TARGET_ID);
    });
  });

  describe('getFollowers() / getFollowing()', () => {
    it('returns follower ids for a user', async () => {
      mockFollowRepository.findFollowerIds.mockResolvedValue([USER_ID]);

      const result = await service.getFollowers(TARGET_ID);

      expect(mockFollowRepository.findFollowerIds).toHaveBeenCalledWith(TARGET_ID);
      expect(result).toEqual([USER_ID]);
    });

    it('returns following ids for a user', async () => {
      mockFollowRepository.findFollowingIds.mockResolvedValue([TARGET_ID]);

      const result = await service.getFollowing(USER_ID);

      expect(mockFollowRepository.findFollowingIds).toHaveBeenCalledWith(USER_ID);
      expect(result).toEqual([TARGET_ID]);
    });
  });
});

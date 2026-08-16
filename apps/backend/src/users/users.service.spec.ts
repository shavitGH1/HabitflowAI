import { NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { AiService } from '../ai/ai.service';
import { STORAGE_ADAPTER } from '../storage/storage.adapter';
import { UserData, UserRepository } from './user.repository';
import { UsersService } from './users.service';

const mockUserRepository = {
  findUserById: jest.fn(),
  findAllUsers: jest.fn(),
  updateProfilePicture: jest.fn(),
};

const mockAiService = {
  generateDailyVariations: jest.fn(),
};

const mockStorage = {
  upload: jest.fn(),
  delete: jest.fn(),
};

const USER_ID = 'user-123';

const makeUser = (overrides: Partial<UserData> = {}): UserData => ({
  id: USER_ID,
  email: 'user@example.com',
  firstName: 'Test',
  lastName: 'User',
  password: 'hashed',
  goal: 'Stay consistent',
  personaType: 'Achiever',
  motivationalMessage: 'Keep going',
  coreGoals: [],
  dailyVariations: [],
  tasksLastGeneratedDate: '',
  ...overrides,
});

describe('UsersService', () => {
  let service: UsersService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        UsersService,
        { provide: UserRepository, useValue: mockUserRepository },
        { provide: AiService, useValue: mockAiService },
        { provide: STORAGE_ADAPTER, useValue: mockStorage },
      ],
    }).compile();

    service = module.get<UsersService>(UsersService);
    jest.clearAllMocks();
  });

  describe('getAllUsers()', () => {
    it('returns id, email, name and profilePicture for every user', async () => {
      mockUserRepository.findAllUsers.mockResolvedValue([
        makeUser({ profilePicture: 'preset:3' }),
        makeUser({ id: 'user-456', email: 'other@example.com', profilePicture: undefined }),
      ]);

      const result = await service.getAllUsers();

      expect(result).toEqual([
        { id: USER_ID, email: 'user@example.com', firstName: 'Test', lastName: 'User', profilePicture: 'preset:3' },
        { id: 'user-456', email: 'other@example.com', firstName: 'Test', lastName: 'User', profilePicture: undefined },
      ]);
    });
  });

  describe('updateProfilePicture()', () => {
    it('throws NotFoundException when the user does not exist', async () => {
      mockUserRepository.findUserById.mockResolvedValue(null);

      await expect(service.updateProfilePicture(USER_ID, 'preset:1')).rejects.toThrow(NotFoundException);
      expect(mockUserRepository.updateProfilePicture).not.toHaveBeenCalled();
    });

    it('persists the preset key and returns it', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser());
      mockUserRepository.updateProfilePicture.mockResolvedValue(makeUser({ profilePicture: 'preset:2' }));

      const result = await service.updateProfilePicture(USER_ID, 'preset:2');

      expect(mockUserRepository.updateProfilePicture).toHaveBeenCalledWith(USER_ID, 'preset:2');
      expect(result).toEqual({ profilePicture: 'preset:2', success: true });
    });
  });

  describe('uploadAvatar()', () => {
    const file = { originalname: 'me.jpg', buffer: Buffer.from('fake-image') } as Express.Multer.File;

    it('uploads the file, persists the returned URL, and does not delete a preset avatar', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ profilePicture: 'preset:1' }));
      mockStorage.upload.mockResolvedValue('/uploads/abc.jpg');
      mockUserRepository.updateProfilePicture.mockResolvedValue(makeUser({ profilePicture: '/uploads/abc.jpg' }));

      const result = await service.uploadAvatar(USER_ID, file);

      expect(mockStorage.upload).toHaveBeenCalledWith(file);
      expect(mockStorage.delete).not.toHaveBeenCalled();
      expect(mockUserRepository.updateProfilePicture).toHaveBeenCalledWith(USER_ID, '/uploads/abc.jpg');
      expect(result).toEqual({ profilePicture: '/uploads/abc.jpg', success: true });
    });

    it('deletes the previous uploaded avatar before persisting the new one', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ profilePicture: '/uploads/old.jpg' }));
      mockStorage.upload.mockResolvedValue('/uploads/new.jpg');
      mockUserRepository.updateProfilePicture.mockResolvedValue(makeUser({ profilePicture: '/uploads/new.jpg' }));

      await service.uploadAvatar(USER_ID, file);

      expect(mockStorage.delete).toHaveBeenCalledWith('/uploads/old.jpg');
    });

    it('throws NotFoundException when the user does not exist', async () => {
      mockUserRepository.findUserById.mockResolvedValue(null);

      await expect(service.uploadAvatar(USER_ID, file)).rejects.toThrow(NotFoundException);
      expect(mockStorage.upload).not.toHaveBeenCalled();
    });
  });
});

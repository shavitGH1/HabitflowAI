import { BadRequestException, NotFoundException, UnauthorizedException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import bcrypt from 'bcrypt';
import { AiService } from '../ai/ai.service';
import { STORAGE_ADAPTER } from '../storage/storage.adapter';
import { UserData, UserRepository } from './user.repository';
import { UsersService } from './users.service';

const mockUserRepository = {
  findUserById: jest.fn(),
  findAllUsers: jest.fn(),
  updateProfilePicture: jest.fn(),
  updateName: jest.fn(),
  updatePassword: jest.fn(),
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
  authProvider: 'local',
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

  describe('updateName()', () => {
    it('throws NotFoundException when the user does not exist', async () => {
      mockUserRepository.findUserById.mockResolvedValue(null);

      await expect(service.updateName(USER_ID, 'Alex', 'Morgan')).rejects.toThrow(NotFoundException);
      expect(mockUserRepository.updateName).not.toHaveBeenCalled();
    });

    it('updates the name when it has never been changed before', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ nameChangedAt: undefined }));
      mockUserRepository.updateName.mockResolvedValue(
        makeUser({ firstName: 'Alex', lastName: 'Morgan', nameChangedAt: '2026-08-19T00:00:00.000Z' }),
      );

      const result = await service.updateName(USER_ID, 'Alex', 'Morgan');

      expect(mockUserRepository.updateName).toHaveBeenCalledWith(USER_ID, 'Alex', 'Morgan');
      expect(result).toEqual({
        firstName: 'Alex',
        lastName: 'Morgan',
        nameChangedAt: '2026-08-19T00:00:00.000Z',
        success: true,
      });
    });

    it('updates the name when the 3-month cooldown has already elapsed', async () => {
      const fourMonthsAgo = new Date();
      fourMonthsAgo.setMonth(fourMonthsAgo.getMonth() - 4);
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ nameChangedAt: fourMonthsAgo.toISOString() }));
      mockUserRepository.updateName.mockResolvedValue(makeUser({ firstName: 'Alex', lastName: 'Morgan' }));

      await service.updateName(USER_ID, 'Alex', 'Morgan');

      expect(mockUserRepository.updateName).toHaveBeenCalledWith(USER_ID, 'Alex', 'Morgan');
    });

    it('rejects the change when still within the 3-month cooldown', async () => {
      const oneMonthAgo = new Date();
      oneMonthAgo.setMonth(oneMonthAgo.getMonth() - 1);
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ nameChangedAt: oneMonthAgo.toISOString() }));

      await expect(service.updateName(USER_ID, 'Alex', 'Morgan')).rejects.toThrow(BadRequestException);
      expect(mockUserRepository.updateName).not.toHaveBeenCalled();
    });
  });

  describe('changePassword()', () => {
    it('throws NotFoundException when the user does not exist', async () => {
      mockUserRepository.findUserById.mockResolvedValue(null);

      await expect(service.changePassword(USER_ID, 'old-pass', 'new-pass')).rejects.toThrow(NotFoundException);
    });

    it('throws BadRequestException for a Google account, without checking the current password', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ authProvider: 'google' }));

      await expect(service.changePassword(USER_ID, 'anything', 'new-pass')).rejects.toThrow(BadRequestException);
      expect(mockUserRepository.updatePassword).not.toHaveBeenCalled();
    });

    it('throws UnauthorizedException when the current password is wrong', async () => {
      mockUserRepository.findUserById.mockResolvedValue(
        makeUser({ password: await bcrypt.hash('correct-pass', 10) }),
      );

      await expect(service.changePassword(USER_ID, 'wrong-pass', 'new-pass')).rejects.toThrow(UnauthorizedException);
      expect(mockUserRepository.updatePassword).not.toHaveBeenCalled();
    });

    it('hashes and persists the new password when the current one is correct', async () => {
      mockUserRepository.findUserById.mockResolvedValue(
        makeUser({ password: await bcrypt.hash('correct-pass', 10) }),
      );
      mockUserRepository.updatePassword.mockResolvedValue(makeUser());

      const result = await service.changePassword(USER_ID, 'correct-pass', 'new-pass');

      expect(mockUserRepository.updatePassword).toHaveBeenCalledWith(USER_ID, expect.any(String));
      const persistedHash = mockUserRepository.updatePassword.mock.calls[0][1];
      expect(await bcrypt.compare('new-pass', persistedHash)).toBe(true);
      expect(result).toEqual({ success: true });
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

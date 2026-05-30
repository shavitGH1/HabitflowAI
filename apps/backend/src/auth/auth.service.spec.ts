import { BadRequestException, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Test, TestingModule } from '@nestjs/testing';
import bcrypt from 'bcrypt';
import { AiService } from '../ai/ai.service';
import { UserRepository } from '../users/user.repository';
import { AuthService } from './auth.service';

jest.mock('bcrypt', () => ({
  hash: jest.fn().mockResolvedValue('hashed_password'),
  compare: jest.fn(),
}));

const mockUserRepository = {
  findUserByEmail: jest.fn(),
  findUserById: jest.fn(),
  saveUser: jest.fn(),
  updateUserDailyTasks: jest.fn(),
  updateUserRefreshToken: jest.fn(),
};

const mockAiService = {
  classifyPersona: jest.fn(),
  generateInitialGoals: jest.fn(),
  generateDailyVariations: jest.fn(),
};

const mockConfigService = {
  get: jest.fn().mockImplementation((key: string) => {
    const values: Record<string, string> = {
      JWT_SECRET: 'test-secret',
      JWT_REFRESH_SECRET: 'test-refresh-secret',
      JWT_ACCESS_EXPIRATION: '15m',
      JWT_REFRESH_EXPIRATION: '7d',
    };
    return values[key];
  }),
};

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AuthService,
        { provide: UserRepository, useValue: mockUserRepository },
        { provide: AiService, useValue: mockAiService },
        { provide: ConfigService, useValue: mockConfigService },
      ],
    }).compile();

    service = module.get<AuthService>(AuthService);
    jest.clearAllMocks();
  });

  describe('register()', () => {
    it('saves user and returns userId on valid input', async () => {
      mockUserRepository.findUserByEmail.mockResolvedValue(null);
      mockAiService.classifyPersona.mockResolvedValue({ isValid: true, personaType: 'Achiever' });
      mockAiService.generateInitialGoals.mockResolvedValue({
        isValid: true,
        motivationalMessage: 'Keep going!',
        coreGoals: [{ description: 'Morning run', points: 20 }],
        dailyVariations: [{ description: 'Stretch for 10 min', points: 10 }],
      });
      mockUserRepository.saveUser.mockResolvedValue({ id: 'user-123' });

      const result = await service.register({
        email: 'test@example.com',
        password: 'password123',
        goal: 'Run a marathon',
        quizAnswers: ['I want to achieve goals', 'I track with checklists', 'I reflect and retry'],
      });

      expect(mockUserRepository.saveUser).toHaveBeenCalledTimes(1);
      expect(result).toMatchObject({ userId: 'user-123', success: true });
    });

    it('throws BadRequestException if email already exists', async () => {
      mockUserRepository.findUserByEmail.mockResolvedValue({ id: 'existing-user' });

      await expect(
        service.register({
          email: 'taken@example.com',
          password: 'password123',
          goal: 'Some goal',
          quizAnswers: ['a', 'b', 'c'],
        }),
      ).rejects.toThrow(BadRequestException);

      expect(mockAiService.classifyPersona).not.toHaveBeenCalled();
    });
  });

  describe('login()', () => {
    it('returns access and refresh tokens on valid credentials', async () => {
      const today = new Date().toISOString().split('T')[0];
      mockUserRepository.findUserByEmail.mockResolvedValue({
        id: 'user-123',
        email: 'test@example.com',
        password: 'hashed_password',
        tasksLastGeneratedDate: today,
      });
      (bcrypt.compare as jest.Mock).mockResolvedValue(true);
      mockUserRepository.updateUserRefreshToken.mockResolvedValue(undefined);

      const result = await service.login({ email: 'test@example.com', password: 'password123' });

      expect(result.success).toBe(true);
      expect(result.accessToken).toBeDefined();
      expect(result.refreshToken).toBeDefined();
    });

    it('throws UnauthorizedException on wrong password', async () => {
      mockUserRepository.findUserByEmail.mockResolvedValue({
        id: 'user-123',
        password: 'hashed_password',
        tasksLastGeneratedDate: new Date().toISOString().split('T')[0],
      });
      (bcrypt.compare as jest.Mock).mockResolvedValue(false);

      await expect(
        service.login({ email: 'test@example.com', password: 'wrong_password' }),
      ).rejects.toThrow(UnauthorizedException);
    });
  });
});

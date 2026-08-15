import { BadRequestException, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Test, TestingModule } from '@nestjs/testing';
import bcrypt from 'bcrypt';
import { AiService } from '../ai/ai.service';
import { HabitRepository } from '../habits/habit.repository';
import { UserRepository } from '../users/user.repository';
import { AuthService } from './auth.service';

jest.mock('bcrypt', () => ({
  hash: jest.fn().mockResolvedValue('hashed_password'),
  compare: jest.fn(),
}));

const GOAL = 'Run a marathon';

const OPEN_ANSWERS = [
  'I want to build a consistent study routine',
  'I stuck with Duolingo for months — daily streaks kept me going',
  'Yes, I studied with friends and we held each other accountable',
  'I switch up topics or locations when things feel stale',
  'I want to set a good example for my younger siblings',
  'I prefer a fixed schedule: same time and place every day',
];

const mockUserRepository = {
  findUserByEmail: jest.fn(),
  findUserById: jest.fn(),
  saveUser: jest.fn(),
  updateUserDailyTasks: jest.fn(),
  updateUserRefreshToken: jest.fn(),
};

const mockHabitRepository = {
  createHabit: jest.fn(),
};

const mockAiService = {
  classifyPersonaWeighted: jest.fn(),
  generatePortfolio: jest.fn(),
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
        { provide: HabitRepository, useValue: mockHabitRepository },
        { provide: AiService, useValue: mockAiService },
        { provide: ConfigService, useValue: mockConfigService },
      ],
    }).compile();

    service = module.get<AuthService>(AuthService);
    jest.clearAllMocks();
  });

  describe('register()', () => {
    it('saves user and returns userId + portfolioSummary on valid input', async () => {
      mockUserRepository.findUserByEmail.mockResolvedValue(null);
      mockAiService.classifyPersonaWeighted.mockResolvedValue({
        isValid: true,
        personaType: 'Achiever',
        weightedBreakdown: { Achievement: 80, Growth: 10, Connection: 0, Exploration: 0, Purpose: 5, Structure: 5 },
        confidenceScore: 0.9,
      });
      mockAiService.generatePortfolio.mockResolvedValue({
        summary: 'You are driven by results and measurable progress.',
        tips: ['Set daily targets', 'Track streaks', 'Celebrate small wins'],
        failurePatterns: ['Losing momentum when progress feels invisible'],
        coreGoals: [{ description: 'Morning run', points: 20 }],
        dailyVariations: [{ description: 'Stretch for 10 min', points: 10 }],
      });
      mockUserRepository.saveUser.mockResolvedValue({
        id: 'user-123',
        coreGoals: [{ id: 'goal-1', description: 'Morning run', points: 20, completed: false }],
      });

      const result = await service.register({ email: 'test@example.com', password: 'password123', goal: GOAL, openAnswers: OPEN_ANSWERS });

      expect(mockAiService.classifyPersonaWeighted).toHaveBeenCalledTimes(1);
      expect(mockAiService.classifyPersonaWeighted).toHaveBeenCalledWith({ goal: GOAL, openAnswers: OPEN_ANSWERS });
      expect(mockAiService.generatePortfolio).toHaveBeenCalledTimes(1);
      expect(mockUserRepository.saveUser).toHaveBeenCalledTimes(1);
      expect(result).toMatchObject({ userId: 'user-123', portfolioSummary: 'You are driven by results and measurable progress.', success: true });
    });

    it('seeds a daily habit from the stated goal so the coach recognizes it immediately', async () => {
      mockUserRepository.findUserByEmail.mockResolvedValue(null);
      mockAiService.classifyPersonaWeighted.mockResolvedValue({
        isValid: true,
        personaType: 'Achiever',
        weightedBreakdown: { Achievement: 80, Growth: 10, Connection: 0, Exploration: 0, Purpose: 5, Structure: 5 },
        confidenceScore: 0.9,
      });
      mockAiService.generatePortfolio.mockResolvedValue({
        summary: 'You are driven by results and measurable progress.',
        tips: ['Set daily targets', 'Track streaks', 'Celebrate small wins'],
        failurePatterns: ['Losing momentum when progress feels invisible'],
        coreGoals: [{ description: 'Morning run', points: 20 }],
        dailyVariations: [{ description: 'Stretch for 10 min', points: 10 }],
      });
      mockUserRepository.saveUser.mockResolvedValue({
        id: 'user-123',
        coreGoals: [{ id: 'goal-1', description: 'Morning run', points: 20, completed: false }],
      });

      await service.register({ email: 'test@example.com', password: 'password123', goal: GOAL, openAnswers: OPEN_ANSWERS });

      expect(mockHabitRepository.createHabit).toHaveBeenCalledTimes(1);
      expect(mockHabitRepository.createHabit).toHaveBeenCalledWith({
        userId: 'user-123',
        title: GOAL,
        frequency: 'daily',
      });
    });

    it('throws BadRequestException if email already exists', async () => {
      mockUserRepository.findUserByEmail.mockResolvedValue({ id: 'existing-user' });

      await expect(
        service.register({ email: 'taken@example.com', password: 'password123', goal: GOAL, openAnswers: OPEN_ANSWERS }),
      ).rejects.toThrow(BadRequestException);

      expect(mockAiService.classifyPersonaWeighted).not.toHaveBeenCalled();
    });

    it('throws BadRequestException and does not save user if classification fails', async () => {
      mockUserRepository.findUserByEmail.mockResolvedValue(null);
      mockAiService.classifyPersonaWeighted.mockResolvedValue({
        isValid: false,
        errorReason: 'Answers are too vague to classify a persona.',
      });

      await expect(
        service.register({ email: 'test@example.com', password: 'password123', goal: GOAL, openAnswers: OPEN_ANSWERS }),
      ).rejects.toThrow(BadRequestException);

      expect(mockAiService.generatePortfolio).not.toHaveBeenCalled();
      expect(mockUserRepository.saveUser).not.toHaveBeenCalled();
    });

    it('uses the dedicated goal field (not the first background answer) and passes all answers to classifier', async () => {
      mockUserRepository.findUserByEmail.mockResolvedValue(null);
      mockAiService.classifyPersonaWeighted.mockResolvedValue({
        isValid: true,
        personaType: 'Achiever',
        weightedBreakdown: { Achievement: 80, Growth: 10, Connection: 0, Exploration: 0, Purpose: 5, Structure: 5 },
        confidenceScore: 0.85,
      });
      mockAiService.generatePortfolio.mockResolvedValue({
        summary: 'Goal-driven summary.',
        tips: ['tip1', 'tip2', 'tip3'],
        failurePatterns: ['pattern1'],
        coreGoals: [{ description: 'Goal', points: 10 }],
        dailyVariations: [{ description: 'Daily', points: 5 }],
      });
      mockUserRepository.saveUser.mockResolvedValue({ id: 'user-456', coreGoals: [] });

      await service.register({ email: 'test@example.com', password: 'password123', goal: GOAL, openAnswers: OPEN_ANSWERS });

      expect(mockAiService.classifyPersonaWeighted).toHaveBeenCalledWith(
        expect.objectContaining({ goal: GOAL, openAnswers: OPEN_ANSWERS }),
      );
      expect(mockAiService.classifyPersonaWeighted).not.toHaveBeenCalledWith(
        expect.objectContaining({ goal: OPEN_ANSWERS[0] }),
      );
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

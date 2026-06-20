import { BadRequestException, Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { v4 as uuidv4 } from 'uuid';
import { AiService } from '../ai/ai.service';
import { UserRepository } from '../users/user.repository';
import { LoginDto } from './dto/login.dto';
import { RefreshDto } from './dto/refresh.dto';
import { RegisterDto } from './dto/register.dto';
import { logger } from '../logger';

@Injectable()
export class AuthService {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly ai: AiService,
    private readonly config: ConfigService,
  ) {}

  async register({ email, password, openAnswers }: RegisterDto) {
    if (await this.userRepository.findUserByEmail(email)) {
      throw new BadRequestException('User with this email already exists');
    }

    const goal = openAnswers[0];

    const classification = await this.ai.classifyPersonaWeighted({ goal, openAnswers });
    if (!classification.isValid) {
      throw new BadRequestException(`Invalid input: ${classification.errorReason}`);
    }

    const { personaType, weightedBreakdown, confidenceScore } = classification;
    const portfolio = await this.ai.generatePortfolio({ goal, openAnswers, personaType, weightedBreakdown });

    const hashedPassword = await bcrypt.hash(password, 10);
    const today = new Date();

    const newUser = await this.userRepository.saveUser({
      email,
      password: hashedPassword,
      goal,
      personaType,
      motivationalMessage: portfolio.summary,
      coreGoals: portfolio.coreGoals.map(g => ({ ...g, id: uuidv4(), completed: false })),
      dailyVariations: portfolio.dailyVariations.map(g => ({ ...g, id: uuidv4(), completed: false })),
      tasksLastGeneratedDate: today.toISOString().split('T')[0],
      personaBreakdown: weightedBreakdown,
      weightedScores: weightedBreakdown,
      portfolioSummary: portfolio.summary,
      tips: portfolio.tips,
      failurePatterns: portfolio.failurePatterns,
      confidenceScore,
    });

    logger.info({ userId: newUser.id }, 'user registered');
    return {
      message: 'User registered successfully',
      userId: newUser.id,
      personaType: newUser.personaType,
      motivationalMessage: newUser.motivationalMessage,
      portfolioSummary: portfolio.summary,
      coreGoals: newUser.coreGoals,
      success: true,
    };
  }

  async login({ email, password }: LoginDto) {
    const user = await this.userRepository.findUserByEmail(email);
    if (!user) {
      logger.warn({ email }, 'login failed: unknown email');
      throw new UnauthorizedException('Invalid credentials');
    }

    const isPasswordValid = await bcrypt.compare(password, user.password);
    if (!isPasswordValid) {
      logger.warn({ email }, 'login failed: invalid password');
      throw new UnauthorizedException('Invalid credentials');
    }

    const today = new Date();
    const todayStr = today.toISOString().split('T')[0];
    if (user.tasksLastGeneratedDate !== todayStr) {
      const newDailyTasks = await this.ai.generateDailyVariations(user, today.getDay());
      await this.userRepository.updateUserDailyTasks(
        user.id,
        newDailyTasks.map(t => ({ ...t, id: uuidv4(), completed: false })),
      );
    }

    const accessToken = jwt.sign({ id: user.id }, this.config.get<string>('JWT_SECRET')!, { expiresIn: this.config.get<string>('JWT_ACCESS_EXPIRATION') });
    const refreshToken = jwt.sign({ id: user.id }, this.config.get<string>('JWT_REFRESH_SECRET')!, { expiresIn: this.config.get<string>('JWT_REFRESH_EXPIRATION') });
    await this.userRepository.updateUserRefreshToken(user.id, refreshToken);

    logger.info({ userId: user.id }, 'user logged in');
    return { accessToken, refreshToken, success: true };
  }

  async refresh({ refreshToken }: RefreshDto) {
    try {
      const decoded = jwt.verify(refreshToken, this.config.get<string>('JWT_REFRESH_SECRET')!) as { id: string };
      const user = await this.userRepository.findUserById(decoded.id);
      if (!user || user.refreshToken !== refreshToken) throw new Error();

      const accessToken = jwt.sign({ id: user.id }, this.config.get<string>('JWT_SECRET')!, { expiresIn: this.config.get<string>('JWT_ACCESS_EXPIRATION') });
      return { accessToken, success: true };
    } catch {
      throw new UnauthorizedException('Invalid refresh token');
    }
  }

  async checkEmail(email: string) {
    const existing = await this.userRepository.findUserByEmail(email);
    return { available: !existing };
  }

  async logout(userId: string) {
    await this.userRepository.updateUserRefreshToken(userId, undefined);
    return { message: 'Successfully logged out', success: true };
  }
}

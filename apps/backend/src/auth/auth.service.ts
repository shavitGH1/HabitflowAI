import { BadRequestException, Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { randomBytes } from 'crypto';
import { v4 as uuidv4 } from 'uuid';
import { OAuth2Client } from 'google-auth-library';
import { AiService } from '../ai/ai.service';
import { HabitRepository } from '../habits/habit.repository';
import { GoalRepository } from '../goals/goal.repository';
import { UserData, UserRepository } from '../users/user.repository';
import { GoogleRegisterDto } from './dto/google-register.dto';
import { LoginDto } from './dto/login.dto';
import { RefreshDto } from './dto/refresh.dto';
import { RegisterDto } from './dto/register.dto';
import { logger } from '../logger';

const GOOGLE_SIGNUP_TOKEN_PURPOSE = 'google-signup';

interface GoogleSignupTokenPayload {
  email: string;
  firstName: string;
  lastName: string;
  purpose: typeof GOOGLE_SIGNUP_TOKEN_PURPOSE;
}

@Injectable()
export class AuthService {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly habitRepository: HabitRepository,
    private readonly goalRepository: GoalRepository,
    private readonly ai: AiService,
    private readonly config: ConfigService,
  ) {}

  async register({ email, firstName, lastName, password, goal, openAnswers, fcmToken }: RegisterDto) {
    if (await this.userRepository.findUserByEmail(email)) {
      throw new BadRequestException('User with this email already exists');
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    const newUser = await this.completeRegistration({
      email,
      firstName: firstName ?? '',
      lastName: lastName ?? '',
      hashedPassword,
      authProvider: 'local',
      goal,
      openAnswers,
      fcmToken,
    });

    return {
      message: 'User registered successfully',
      userId: newUser.id,
      personaType: newUser.personaType,
      motivationalMessage: newUser.motivationalMessage,
      portfolioSummary: newUser.portfolioSummary,
      coreGoals: newUser.coreGoals,
      success: true,
    };
  }

  // Google Sign-In accounts have no password the user ever set or sees — a random
  // hash is stored so User.password stays required with no schema/migration change,
  // and email/password login simply can't succeed against it (not a real credential).
  async registerViaGoogle({ signupToken, goal, openAnswers, fcmToken }: GoogleRegisterDto) {
    let payload: GoogleSignupTokenPayload;
    try {
      payload = jwt.verify(signupToken, this.config.get<string>('JWT_SECRET')!) as GoogleSignupTokenPayload;
    } catch {
      throw new UnauthorizedException('Signup session expired — please sign in with Google again');
    }
    if (payload.purpose !== GOOGLE_SIGNUP_TOKEN_PURPOSE) {
      throw new UnauthorizedException('Invalid signup token');
    }
    if (await this.userRepository.findUserByEmail(payload.email)) {
      throw new BadRequestException('User with this email already exists');
    }

    const hashedPassword = await bcrypt.hash(randomBytes(32).toString('hex'), 10);
    const newUser = await this.completeRegistration({
      email: payload.email,
      firstName: payload.firstName,
      lastName: payload.lastName,
      hashedPassword,
      authProvider: 'google',
      goal,
      openAnswers,
      fcmToken,
    });

    const { accessToken, refreshToken } = await this.issueTokens(newUser.id);
    logger.info({ userId: newUser.id }, 'user registered via Google');
    return {
      message: 'User registered successfully',
      userId: newUser.id,
      personaType: newUser.personaType,
      motivationalMessage: newUser.motivationalMessage,
      portfolioSummary: newUser.portfolioSummary,
      coreGoals: newUser.coreGoals,
      accessToken,
      refreshToken,
      success: true,
    };
  }

  private async completeRegistration(input: {
    email: string;
    firstName: string;
    lastName: string;
    hashedPassword: string;
    authProvider: 'local' | 'google';
    goal: string;
    openAnswers: string[];
    fcmToken?: string;
  }): Promise<UserData> {
    const { email, firstName, lastName, hashedPassword, authProvider, goal, openAnswers, fcmToken } = input;

    const classification = await this.ai.classifyPersonaWeighted({ goal, openAnswers });
    if (!classification.isValid) {
      throw new BadRequestException(`Invalid input: ${classification.errorReason}`);
    }

    const { personaType, weightedBreakdown, confidenceScore } = classification;
    const portfolio = await this.ai.generatePortfolio({ goal, openAnswers, personaType, weightedBreakdown });

    const today = new Date();
    const newUser = await this.userRepository.saveUser({
      email,
      firstName,
      lastName,
      password: hashedPassword,
      authProvider,
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
      fcmToken: fcmToken ?? undefined,
    });

    // Create an active goal record so it's actionable on the client
    const targetDate = new Date();
    targetDate.setMonth(targetDate.getMonth() + 3); // Default 3 month goal
    await this.goalRepository.createGoal({
      userId: newUser.id,
      title: goal,
      targetDate,
    });

    logger.info({ userId: newUser.id }, 'user registered');
    return newUser;
  }

  private async issueTokens(userId: string): Promise<{ accessToken: string; refreshToken: string }> {
    const accessToken = jwt.sign({ id: userId }, this.config.get<string>('JWT_SECRET')!, { expiresIn: this.config.get<string>('JWT_ACCESS_EXPIRATION') });
    const refreshToken = jwt.sign({ id: userId }, this.config.get<string>('JWT_REFRESH_SECRET')!, { expiresIn: this.config.get<string>('JWT_REFRESH_EXPIRATION') });
    await this.userRepository.updateUserRefreshToken(userId, refreshToken);
    return { accessToken, refreshToken };
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
      const habits = await this.habitRepository.findByUserId(user.id);
      const habitInputs = habits.map(h => ({ id: h.id, title: h.title }));
      const newDailyTasks = await this.ai.generateDailyVariations(user, today.getDay(), habitInputs);
      await this.userRepository.updateUserDailyTasks(
        user.id,
        newDailyTasks.map(t => ({ ...t, id: uuidv4(), completed: false })),
      );
    }

    const { accessToken, refreshToken } = await this.issueTokens(user.id);

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

  getOnboardingSuggestions(goal: string) {
    return this.ai.getOnboardingSuggestions(goal);
  }

  async updateFcmToken(userId: string, fcmToken: string) {
    await this.userRepository.updateUserFcmToken(userId, fcmToken);
    return { message: 'FCM token updated', success: true };
  }

  async handleGoogleAuth({ email, firstName, lastName }: { email: string; firstName: string; lastName: string }) {
    const user = await this.userRepository.findUserByEmail(email);
    if (user) {
      const { accessToken, refreshToken } = await this.issueTokens(user.id);
      logger.info({ userId: user.id }, 'user authenticated via Google');
      return { accessToken, refreshToken, success: true, isNewUser: false };
    }

    // No account yet — hand back a short-lived signup token instead of a hard 401,
    // so the client can go straight into the onboarding quiz (email/name already
    // known from Google, no password to collect) and finish via POST /auth/register-google.
    const payload: GoogleSignupTokenPayload = { email, firstName, lastName, purpose: GOOGLE_SIGNUP_TOKEN_PURPOSE };
    const signupToken = jwt.sign(payload, this.config.get<string>('JWT_SECRET')!, { expiresIn: '15m' });
    return { isNewUser: true, signupToken, email, firstName, lastName, success: true };
  }

  // Native Android Sign-In (Credential Manager) hands the app a Google ID token
  // directly — no browser redirect involved. Verified server-side against our own
  // GOOGLE_CLIENT_ID (the audience), then funneled through the exact same
  // isNewUser branching as the browser-based flow above.
  async verifyGoogleIdToken(idToken: string) {
    const clientId = this.config.get<string>('GOOGLE_CLIENT_ID')!;
    const client = new OAuth2Client(clientId);

    let payload: { email?: string; given_name?: string; family_name?: string } | undefined;
    try {
      const ticket = await client.verifyIdToken({ idToken, audience: clientId });
      payload = ticket.getPayload();
    } catch {
      throw new UnauthorizedException('Invalid Google ID token');
    }
    if (!payload?.email) {
      throw new UnauthorizedException('Invalid Google ID token');
    }

    return this.handleGoogleAuth({
      email: payload.email,
      firstName: payload.given_name ?? '',
      lastName: payload.family_name ?? '',
    });
  }

  async logout(userId: string) {
    await this.userRepository.updateUserRefreshToken(userId, undefined);
    return { message: 'Successfully logged out', success: true };
  }
}

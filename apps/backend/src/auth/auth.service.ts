import { BadRequestException, Injectable, UnauthorizedException } from '@nestjs/common';
import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { v4 as uuidv4 } from 'uuid';
import { classifyPersona, generateDailyVariations, generateInitialGoals } from '../services/aiService';
import { findUserByEmail, findUserById, saveUser, updateUserDailyTasks, updateUserRefreshToken } from '../repository/userRepository';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { RefreshDto } from './dto/refresh.dto';

@Injectable()
export class AuthService {
  async register({ email, password, goal, quizAnswers }: RegisterDto) {
    if (findUserByEmail(email)) {
      throw new BadRequestException('User with this email already exists');
    }

    const classification = await classifyPersona(goal, quizAnswers);
    if (!classification.isValid || !classification.personaType) {
      throw new BadRequestException(`Invalid input: ${classification.errorReason}`);
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    const today = new Date();
    const goals = await generateInitialGoals(
      { goal, personaType: classification.personaType, email },
      today.getDay(),
    );

    const newUser = saveUser({
      email,
      password: hashedPassword,
      goal,
      personaType: classification.personaType,
      motivationalMessage: goals.motivationalMessage ?? '',
      coreGoals: goals.coreGoals?.map(g => ({ ...g, id: uuidv4(), completed: false })) ?? [],
      dailyVariations: goals.dailyVariations?.map(g => ({ ...g, id: uuidv4(), completed: false })) ?? [],
      tasksLastGeneratedDate: today.toISOString().split('T')[0],
    });

    return { message: 'User registered successfully', userId: newUser.id, success: true };
  }

  async login({ email, password }: LoginDto) {
    const user = findUserByEmail(email);
    if (!user) throw new UnauthorizedException('Invalid credentials');

    const isPasswordValid = await bcrypt.compare(password, user.password);
    if (!isPasswordValid) throw new UnauthorizedException('Invalid credentials');

    const today = new Date();
    const todayStr = today.toISOString().split('T')[0];
    if (user.tasksLastGeneratedDate !== todayStr) {
      const newDailyTasks = await generateDailyVariations(user, today.getDay());
      updateUserDailyTasks(user.id, newDailyTasks.map(t => ({ ...t, id: uuidv4(), completed: false })));
    }

    const accessToken = jwt.sign({ id: user.id }, process.env.JWT_SECRET as string, { expiresIn: '15m' });
    const refreshToken = jwt.sign({ id: user.id }, process.env.JWT_REFRESH_SECRET as string, { expiresIn: '7d' });
    updateUserRefreshToken(user.id, refreshToken);

    return { accessToken, refreshToken, success: true };
  }

  refresh({ refreshToken }: RefreshDto) {
    try {
      const decoded = jwt.verify(refreshToken, process.env.JWT_REFRESH_SECRET as string) as { id: string };
      const user = findUserById(decoded.id);
      if (!user || user.refreshToken !== refreshToken) throw new Error();

      const accessToken = jwt.sign({ id: user.id }, process.env.JWT_SECRET as string, { expiresIn: '15m' });
      return { accessToken, success: true };
    } catch {
      throw new UnauthorizedException('Invalid refresh token');
    }
  }

  logout(userId: string) {
    updateUserRefreshToken(userId, undefined);
    return { message: 'Successfully logged out', success: true };
  }
}

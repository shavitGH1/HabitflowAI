import { Request, Response } from 'express';
import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { findUserByEmail, findUserById, saveUser, updateUserDailyTasks, updateUserRefreshToken } from '../repository/userRepository';
import { classifyPersona, generateInitialGoals, generateDailyVariations } from '../services/aiService';
import { v4 as uuidv4 } from 'uuid';
import { AuthenticatedRequest } from '../middleware/authMiddleware';

export async function register(req: Request, res: Response) {
  try {
    const { email, password, goal, quizAnswers } = req.body;

    if (!email || !password || !goal || !quizAnswers) {
      return res.status(400).json({ message: 'Request body must contain email, password, goal, and quizAnswers', success: false });
    }

    const existingUser = findUserByEmail(email);
    if (existingUser) {
      return res.status(400).json({ message: 'User with this email already exists', success: false });
    }

    const classificationResult = await classifyPersona(goal, quizAnswers);
    if (!classificationResult.isValid || !classificationResult.personaType) {
      return res.status(400).json({ message: `Invalid input: ${classificationResult.errorReason}`, success: false });
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    const today = new Date();
    const goals = await generateInitialGoals({ goal, personaType: classificationResult.personaType, email }, today.getDay());

    const newUser = saveUser({
      email,
      password: hashedPassword,
      goal,
      personaType: classificationResult.personaType,
      motivationalMessage: goals.motivationalMessage || '',
      coreGoals: goals.coreGoals?.map(g => ({ ...g, id: uuidv4(), completed: false })) || [],
      dailyVariations: goals.dailyVariations?.map(g => ({ ...g, id: uuidv4(), completed: false })) || [],
      tasksLastGeneratedDate: today.toISOString().split('T')[0],
    });

    res.status(201).json({ message: 'User registered successfully', userId: newUser.id, success: true });
  } catch (error: any) {
    console.error('Error in register:', error);
    res.status(500).json({ message: 'Internal Server Error', success: false });
  }
}

export async function login(req: Request, res: Response) {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: 'Request body must contain email and password', success: false });
    }

    const user = findUserByEmail(email);
    if (!user) {
      return res.status(401).json({ message: 'Invalid credentials', success: false });
    }

    const isPasswordValid = await bcrypt.compare(password, user.password);
    if (!isPasswordValid) {
      return res.status(401).json({ message: 'Invalid credentials', success: false });
    }

    const today = new Date();
    const todayStr = today.toISOString().split('T')[0];
    if (user.tasksLastGeneratedDate !== todayStr) {
      console.log('Generating new daily tasks for user on login:', user.id);
      const newDailyTasks = await generateDailyVariations(user, today.getDay());
      const updatedTasks = newDailyTasks.map(task => ({ ...task, id: uuidv4(), completed: false }));
      updateUserDailyTasks(user.id, updatedTasks);
    }

    const accessToken = jwt.sign({ id: user.id }, process.env.JWT_SECRET as string, { expiresIn: '15m' });
    const refreshToken = jwt.sign({ id: user.id }, process.env.JWT_REFRESH_SECRET as string, { expiresIn: '7d' });

    updateUserRefreshToken(user.id, refreshToken);

    res.status(200).json({ accessToken, refreshToken, success: true });
  } catch (error: any) {
    console.error('Error in login:', error);
    res.status(500).json({ message: 'Internal Server Error', success: false });
  }
}

export async function refresh(req: Request, res: Response) {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) {
      return res.status(401).json({ message: 'Refresh token is required', success: false });
    }

    const decoded = jwt.verify(refreshToken, process.env.JWT_REFRESH_SECRET as string) as { id: string };
    const user = findUserById(decoded.id);

    if (!user || user.refreshToken !== refreshToken) {
      return res.status(403).json({ message: 'Invalid refresh token', success: false });
    }

    const accessToken = jwt.sign({ id: user.id }, process.env.JWT_SECRET as string, { expiresIn: '15m' });

    res.status(200).json({ accessToken, success: true });
  } catch (error) {
    return res.status(403).json({ message: 'Invalid refresh token', success: false });
  }
}

export async function logout(req: AuthenticatedRequest, res: Response) {
  try {
    const userId = req.user?.id;
    if (!userId) {
      return res.status(401).json({ message: 'Unauthorized', success: false });
    }

    updateUserRefreshToken(userId, undefined);
    res.status(200).json({ message: 'Successfully logged out', success: true });
  } catch (error: any) {
    console.error('Error in logout:', error);
    res.status(500).json({ message: 'Internal Server Error', success: false });
  }
}

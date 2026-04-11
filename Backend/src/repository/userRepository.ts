import { v4 as uuidv4 } from 'uuid';
import { GoalTask } from '../dto/goal.dto';

// This interface defines the structure of our user data.
export interface User {
  id: string;
  email: string;
  password: string;
  goal: string;
  personaType: string;
  motivationalMessage: string;
  coreGoals: GoalTask[];
  dailyVariations: GoalTask[];
  tasksLastGeneratedDate: string; // YYYY-MM-DD format
  refreshToken?: string;
}

// An in-memory store for our users.
const userStore = new Map<string, User>();

/**
 * Saves a new user to the in-memory store.
 */
export function saveUser(userData: Omit<User, 'id'>): User {
  const user: User = {
    id: uuidv4(),
    ...userData,
  };
  userStore.set(user.id, user);
  console.log('User saved to in-memory store:', user);
  return user;
}

/**
 * Finds a user by their ID.
 */
export function findUserById(id: string): User | undefined {
  const user = userStore.get(id);
  console.log(`User lookup by ID (${id}):`, user);
  return user;
}

/**
 * Finds a user by their email.
 */
export function findUserByEmail(email: string): User | undefined {
  for (const user of userStore.values()) {
    if (user.email === email) {
      console.log(`User lookup by email (${email}):`, user);
      return user;
    }
  }
  return undefined;
}

/**
 * Updates a user's daily tasks and the generation date.
 */
export function updateUserDailyTasks(userId: string, newDailyTasks: GoalTask[]): User | undefined {
  const user = findUserById(userId);
  if (user) {
    user.dailyVariations = newDailyTasks;
    user.tasksLastGeneratedDate = new Date().toISOString().split('T')[0];
    userStore.set(userId, user);
    console.log('Updated user daily tasks:', user);
    return user;
  }
  return undefined;
}

/**
 * Updates a user's refresh token.
 */
export function updateUserRefreshToken(userId: string, refreshToken?: string): User | undefined {
  const user = findUserById(userId);
  if (user) {
    user.refreshToken = refreshToken;
    userStore.set(userId, user);
    console.log('Updated user refresh token:', user);
    return user;
  }
  return undefined;
}

/**
 * Updates a user's persona, goal, and tasks.
 */
export function updateUserPersona(userId: string, updates: Partial<Pick<User, 'goal' | 'personaType' | 'motivationalMessage' | 'coreGoals' | 'dailyVariations' | 'tasksLastGeneratedDate'>>): User | undefined {
    const user = findUserById(userId);
    if (user) {
        Object.assign(user, updates);
        userStore.set(userId, user);
        console.log('Updated user persona:', user);
        return user;
    }
    return undefined;
}

import { v4 as uuidv4 } from 'uuid';

// This interface defines the structure of our user data.
export interface User {
  id: string;
  goal: string;
  personaType: string;
}

// An in-memory store for our users.
// We use a Map for efficient lookups by ID.
const userStore = new Map<string, User>();

/**
 * Saves a new user to the in-memory store.
 * @param goal - The user's goal.
 * @param personaType - The user's classified persona.
 * @returns The newly created user object with a generated UUID.
 */
export function saveUser(goal: string, personaType: string): User {
  const user: User = {
    id: uuidv4(),
    goal,
    personaType,
  };
  userStore.set(user.id, user);
  console.log('User saved to in-memory store:', user);
  return user;
}

/**
 * Finds a user by their ID.
 * @param id - The UUID of the user to find.
 * @returns The user object if found, otherwise undefined.
 */
export function findUserById(id: string): User | undefined {
  const user = userStore.get(id);
  console.log(`User lookup by ID (${id}):`, user);
  return user;
}

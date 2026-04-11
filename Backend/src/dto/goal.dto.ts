/**
 * @swagger
 * components:
 *   schemas:
 *     GenerateGoalsRequest:
 *       type: object
 *       required:
 *         - userId
 *         - dayOfWeek
 *       properties:
 *         userId:
 *           type: string
 *           description: The unique identifier for the user.
 *         dayOfWeek:
 *           type: number
 *           description: The target day of the week (e.g., 0 for Sunday, 1 for Monday).
 *     GoalTask:
 *       type: object
 *       properties:
 *         description:
 *           type: string
 *           description: The description of the habit-related task.
 *         points:
 *           type: number
 *           description: The point value assigned to completing the task.
 *     GenerateGoalsResponse:
 *       type: object
 *       properties:
 *         isValid:
 *           type: boolean
 *           description: Whether the user's input was considered valid by the AI.
 *         errorReason:
 *           type: string
 *           description: The reason for invalid input, if applicable.
 *         personaType:
 *           type: string
 *           description: The classified persona type.
 *         motivationalMessage:
 *           type: string
 *           description: A personalized motivational message.
 *         coreGoals:
 *           type: array
 *           items:
 *             $ref: '#/components/schemas/GoalTask'
 *           description: A list of core habit-related goals.
 *         dailyVariations:
 *           type: array
 *           items:
 *             $ref: '#/components/schemas/GoalTask'
 *           description: A list of daily variations for the habit.
 */

export interface GenerateGoalsRequest {
  userId: string;
  dayOfWeek: number;
}

export interface GoalTask {
  description: string;
  points: number;
}

export interface GenerateGoalsResponse {
  isValid: boolean;
  errorReason?: string;
  personaType?: string;
  motivationalMessage?: string;
  coreGoals?: GoalTask[];
  dailyVariations?: GoalTask[];
}

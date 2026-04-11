/**
 * @swagger
 * components:
 *   schemas:
 *     UserHomePageResponse:
 *       type: object
 *       properties:
 *         goal:
 *           type: string
 *         personaType:
 *           type: string
 *         motivationalMessage:
 *           type: string
 *         coreGoals:
 *           type: array
 *           items:
 *             $ref: '#/components/schemas/GoalTask'
 *         dailyVariations:
 *           type: array
 *           items:
 *             $ref: '#/components/schemas/GoalTask'
 *         success:
 *           type: boolean
 */

import { GoalTask } from "./goal.dto";

export interface UserHomePageResponse {
    goal: string;
    personaType: string;
    motivationalMessage: string;
    coreGoals: GoalTask[];
    dailyVariations: GoalTask[];
    success: boolean;
}

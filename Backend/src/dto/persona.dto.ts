/**
 * @swagger
 * components:
 *   schemas:
 *     ClassifyPersonaRequest:
 *       type: object
 *       required:
 *         - goal
 *         - quizAnswers
 *       properties:
 *         goal:
 *           type: string
 *           description: The user's free-text habit goal.
 *         quizAnswers:
 *           type: array
 *           items:
 *             type: string
 *           description: Array of answers to personality questions.
 *     ClassifyPersonaResponse:
 *       type: object
 *       properties:
 *         id:
 *           type: string
 *           description: The unique identifier for the user.
 *         personaType:
 *           type: string
 *           description: The classified persona type.
 *         success:
 *           type: boolean
 *           description: Indicates if the request was successful.
 */

export interface ClassifyPersonaRequest {
  goal: string;
  quizAnswers: string[];
}

export interface ClassifyPersonaResponse {
  id: string;
  personaType: string;
  success: boolean;
}

/**
 * @swagger
 * components:
 *   schemas:
 *     PersonaRequestDto:
 *       type: object
 *       required:
 *         - goal
 *         - quizAnswers
 *       properties:
 *         goal:
 *           type: string
 *           description: The user's free-text habit goal
 *         quizAnswers:
 *           type: array
 *           items:
 *             type: string
 *           description: Array of answers to personality questions
 *     PersonaResponseDto:
 *       type: object
 *       properties:
 *         personaType:
 *           type: string
 *           enum: [Architect, Achiever]
 *           description: The classified persona type
 *         motivationalMessage:
 *           type: string
 *           description: A personalized motivational message
 *         success:
 *           type: boolean
 *           description: Indicates if the request was successful
 */
export interface PersonaRequestDto {
  goal: string;
  quizAnswers: string[];
}

export interface PersonaResponseDto {
  personaType: 'Architect' | 'Achiever';
  motivationalMessage: string;
  success: boolean;
}

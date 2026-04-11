import { Router } from 'express';
import { generateGoalsController } from '../controllers/goalsController';

const router = Router();

/**
 * @swagger
 * /api/v1/goals/generate:
 *   post:
 *     summary: Generate goals for a user
 *     tags: [Goals]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/GenerateGoalsRequest'
 *     responses:
 *       200:
 *         description: Successfully generated goals
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/GenerateGoalsResponse'
 *       400:
 *         description: Invalid input
 *       404:
 *         description: User not found
 *       500:
 *         description: Internal server error
 */
router.post('/generate', generateGoalsController);

export default router;

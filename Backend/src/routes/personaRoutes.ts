import { Router } from 'express';
import { reclassifyPersona } from '../controllers/personaController';
import { authMiddleware } from '../middleware/authMiddleware';

const router = Router();

/**
 * @swagger
 * /api/v1/personas/reclassify:
 *   post:
 *     summary: Reclassify a user's persona and generate new goals
 *     tags: [Personas]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               goal:
 *                 type: string
 *               quizAnswers:
 *                 type: array
 *                 items:
 *                   type: string
 *     responses:
 *       200:
 *         description: Successfully reclassified persona and updated user data
 *       400:
 *         description: Invalid input
 *       401:
 *         description: Unauthorized
 *       404:
 *         description: User not found
 *       500:
 *         description: Internal server error
 */
router.post('/reclassify', authMiddleware, reclassifyPersona);

export default router;

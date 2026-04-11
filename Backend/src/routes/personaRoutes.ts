import { Router } from 'express';
import { classifyPersonaController } from '../controllers/personaController';

const router = Router();

/**
 * @swagger
 * /api/v1/personas/classify:
 *   post:
 *     summary: Classify user persona and create a user session
 *     tags: [Personas]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/ClassifyPersonaRequest'
 *     responses:
 *       200:
 *         description: The user persona was successfully classified
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ClassifyPersonaResponse'
 *       400:
 *         description: Invalid input
 *       500:
 *         description: Internal server error
 */
router.post('/classify', classifyPersonaController);

export default router;

import { Router } from 'express';
import { classifyPersona } from '../controllers/personaController';

const router = Router();

/**
 * @swagger
 * /api/v1/personas/classify:
 *   post:
 *     summary: Classify user persona
 *     tags: [Personas]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/PersonaRequestDto'
 *     responses:
 *       200:
 *         description: The user persona was successfully classified
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/PersonaResponseDto'
 *       400:
 *         description: Bad request
 *       500:
 *         description: Internal server error
 */
router.post('/classify', classifyPersona);

export default router;

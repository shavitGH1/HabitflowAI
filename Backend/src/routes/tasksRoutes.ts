import { Router } from 'express';
import { completeTask } from '../controllers/tasksController';
import { authMiddleware } from '../middleware/authMiddleware';

const router = Router();

/**
 * @swagger
 * /api/v1/tasks/{taskId}/complete:
 *   patch:
 *     summary: Mark a task as complete
 *     tags: [Tasks]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: taskId
 *         required: true
 *         schema:
 *           type: string
 *     responses:
 *       200:
 *         description: Task marked as complete
 *       401:
 *         description: Unauthorized
 *       404:
 *         description: Task not found
 *       500:
 *         description: Internal server error
 */
router.patch('/:taskId/complete', authMiddleware, completeTask);

export default router;

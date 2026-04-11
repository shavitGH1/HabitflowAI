import { Router } from 'express';
import { getHomePageData } from '../controllers/userController';
import { authMiddleware } from '../middleware/authMiddleware';

const router = Router();

/**
 * @swagger
 * /api/v1/users/me/home:
 *   get:
 *     summary: Get all data for the user's home page
 *     tags: [Users]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Successfully retrieved home page data
 *       401:
 *         description: Unauthorized
 *       404:
 *         description: User not found
 *       500:
 *         description: Internal server error
 */
router.get('/me/home', authMiddleware, getHomePageData);

export default router;

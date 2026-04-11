import express from 'express';
import dotenv from 'dotenv';
import authRoutes from './routes/authRoutes';
import tasksRoutes from './routes/tasksRoutes';
import userRoutes from './routes/userRoutes';
import personaRoutes from './routes/personaRoutes';
import swaggerUi from 'swagger-ui-express';
import { swaggerSpec } from './config/swagger';
import { listAvailableModels } from './services/aiService';

dotenv.config();

const app = express();
const port = process.env.PORT || 3000;

app.use(express.json());

// Logger middleware
app.use((req, res, next) => {
  const start = Date.now();
  const clientIp = req.socket.remoteAddress;
  const timestamp = new Date().toLocaleString('en-US', { timeZone: 'Asia/Jerusalem' });

  // Log on request received
  console.log(`[${timestamp}] Request received: ${req.method} ${req.originalUrl} - IP: ${clientIp}`);
  
  // Log request body
  if (req.body && Object.keys(req.body).length > 0) {
    console.log('Request Body:', JSON.stringify(req.body, null, 2));
  }

  res.on('finish', () => {
    const duration = Date.now() - start;
    const finishTimestamp = new Date().toLocaleString('en-US', { timeZone: 'Asia/Jerusalem' });
    // Log on response finished
    console.log(
      `[${finishTimestamp}] Request finished: ${req.method} ${req.originalUrl} ${res.statusCode} - ${duration}ms - IP: ${clientIp}`
    );
  });
  
  next();
});

// Temporary route to list available models
app.get('/api/v1/models', async (req, res) => {
  const models = await listAvailableModels();
  res.status(200).json(models);
});

app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec));
app.use('/api/v1/auth', authRoutes);
app.use('/api/v1/tasks', tasksRoutes);
app.use('/api/v1/users', userRoutes);
app.use('/api/v1/personas', personaRoutes);

app.listen(port, () => {
  console.log(`Server is running on port ${port}`);
});

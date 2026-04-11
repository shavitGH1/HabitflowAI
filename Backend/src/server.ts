import express from 'express';
import dotenv from 'dotenv';
import personaRoutes from './routes/personaRoutes';
import goalsRoutes from './routes/goalsRoutes'; // Import goals routes
import swaggerUi from 'swagger-ui-express';
import { swaggerSpec } from './config/swagger';

dotenv.config();

const app = express();
const port = process.env.PORT || 3000;

app.use(express.json());

// Logger middleware
app.use((req, res, next) => {
  const start = Date.now();
  const clientIp = req.socket.remoteAddress;

  // Log on request received
  console.log(`Request received: ${req.method} ${req.originalUrl} - IP: ${clientIp}`);
  
  // Log request body
  if (req.body && Object.keys(req.body).length > 0) {
    console.log('Request Body:', JSON.stringify(req.body, null, 2));
  }

  res.on('finish', () => {
    const duration = Date.now() - start;
    // Log on response finished
    console.log(
      `Request finished: ${req.method} ${req.originalUrl} ${res.statusCode} - ${duration}ms - IP: ${clientIp}`
    );
  });
  
  next();
});

app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec));
app.use('/api/v1/personas', personaRoutes);
app.use('/api/v1/goals', goalsRoutes); // Register goals routes

app.listen(port, () => {
  console.log(`Server is running on port ${port}`);
});

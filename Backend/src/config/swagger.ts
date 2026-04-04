import swaggerJsdoc from 'swagger-jsdoc';

const options = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'HabitFlow AI API',
      version: '1.0.0',
      description: 'API documentation for the HabitFlow AI backend',
    },
    servers: [
      {
        url: 'http://localhost:3000',
      },
    ],
  },
  apis: ['./src/routes/*.ts', './src/dto/*.ts'],
};

export const swaggerSpec = swaggerJsdoc(options);

import 'reflect-metadata';
import { join } from 'path';
import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { NestExpressApplication } from '@nestjs/platform-express';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { logger } from './logger';

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule);
  app.useStaticAssets(join(process.cwd(), 'uploads'), { prefix: '/uploads' });

  app.setGlobalPrefix('api/v1');
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));

  const swaggerConfig = new DocumentBuilder()
    .setTitle('HabitFlow AI')
    .setDescription('HabitFlow AI API')
    .setVersion('1.0')
    .addBearerAuth()
    .build();
  SwaggerModule.setup('api-docs', app, SwaggerModule.createDocument(app, swaggerConfig));

  const port = process.env.PORT ?? 3000;
  await app.listen(port, '0.0.0.0');
  logger.info(`Application is running on: http://localhost:${port}`);
}

bootstrap();

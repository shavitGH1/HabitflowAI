import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { MongooseModule } from '@nestjs/mongoose';
import { ScheduleModule } from '@nestjs/schedule';
import { ThrottlerModule } from '@nestjs/throttler';
import * as Joi from 'joi';
import { AuthModule } from './auth/auth.module';
import { ChatModule } from './chat/chat.module';
import { CoachModule } from './coach/coach.module';
import { FollowsModule } from './follows/follows.module';
import { GoalsModule } from './goals/goals.module';
import { HabitsModule } from './habits/habits.module';
import { InsightsModule } from './insights/insights.module';
import { PostsModule } from './posts/posts.module';
import { LocationsModule } from './locations/locations.module';
import { PersonasModule } from './personas/personas.module';
import { TasksModule } from './tasks/tasks.module';
import { UsersModule } from './users/users.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
      validationSchema: Joi.object({
        PORT: Joi.number().default(3000),
        MONGO_URI: Joi.string().required(),
        GEMINI_API_KEY: Joi.string().required(),
        JWT_SECRET: Joi.string().required(),
        JWT_REFRESH_SECRET: Joi.string().required(),
        JWT_ACCESS_EXPIRATION: Joi.string().default('15m'),
        JWT_REFRESH_EXPIRATION: Joi.string().default('7d'),
        GOOGLE_CLIENT_ID: Joi.string().required(),
        GOOGLE_CLIENT_SECRET: Joi.string().required(),
        GOOGLE_CALLBACK_URL: Joi.string().required(),
        FIREBASE_PROJECT_ID: Joi.string().required(),
        FIREBASE_PRIVATE_KEY: Joi.string().required(),
        FIREBASE_CLIENT_EMAIL: Joi.string().required(),
      }),
      validationOptions: { abortEarly: true },
    }),
    MongooseModule.forRootAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        uri: config.get<string>('MONGO_URI'),
      }),
    }),
    ThrottlerModule.forRoot([{ ttl: 60000, limit: 5 }]),
    ScheduleModule.forRoot(),
    AuthModule,
    UsersModule,
    TasksModule,
    PersonasModule,
    InsightsModule,
    LocationsModule,
    HabitsModule,
    PostsModule,
    ChatModule,
    CoachModule,
    GoalsModule,
    FollowsModule,
  ],
})
export class AppModule {}

import { INestApplication, ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Test } from '@nestjs/testing';
import { ThrottlerGuard } from '@nestjs/throttler';
import { MongoMemoryServer } from 'mongodb-memory-server';
import request from 'supertest';
import { AiService } from '../ai/ai.service';
import { AppModule } from '../app.module';
import { FIREBASE_MESSAGING } from '../notifications/firebase.module';

export const OPEN_ANSWERS = [
  'I want to build a consistent study routine and track real progress each week',
  'I stuck with Duolingo for months because daily streaks kept me accountable',
  'Yes — I joined a study group and we pushed each other to show up every day',
  'I switch up locations or topics to keep things fresh when routine gets stale',
  'I want to set a good example for my younger siblings and make my family proud',
  'I prefer a fixed schedule: same time, same place, every day without exception',
];

export const KNOWN_PERSONA_TYPES = [
  'Achiever', 'Grower', 'Socializer', 'Explorer', 'Altruist', 'Architect',
];

const mockAiService = {
  classifyPersonaWeighted: jest.fn().mockResolvedValue({
    isValid: true,
    personaType: 'Achiever',
    weightedBreakdown: { Achievement: 80, Growth: 10, Connection: 0, Exploration: 0, Purpose: 5, Structure: 5 },
    confidenceScore: 0.9,
  }),
  generatePortfolio: jest.fn().mockResolvedValue({
    summary: 'You are driven by measurable results and consistent progress.',
    tips: ['Set daily targets', 'Track streaks', 'Celebrate small wins'],
    failurePatterns: ['Losing momentum when progress feels invisible'],
    coreGoals: [
      { description: 'Morning run', points: 20 },
      { description: 'Read 30 minutes', points: 15 },
    ],
    dailyVariations: [{ description: 'Stretch for 10 min', points: 10 }],
  }),
  generateDailyVariations: jest.fn().mockResolvedValue([
    { description: 'Daily stretch', points: 10 },
  ]),
  detectDrift: jest.fn().mockResolvedValue({
    driftDetected: false,
    driftScore: 0.05,
    newSuggestedPersona: null,
    currentBreakdown: {},
    rationale: 'Behavior is stable.',
  }),
  getWeeklyInsights: jest.fn().mockResolvedValue({ summary: 'Great week overall.' }),
  checkTaskVerification: jest.fn().mockResolvedValue({ isPlausible: true, reason: '' }),
  recordMotivationFeedback: jest.fn().mockReturnValue({ positiveFeedbackCount: 1, negativeFeedbackCount: 0 }),
  getFeedbackTally: jest.fn().mockReturnValue({ positiveFeedbackCount: 0, negativeFeedbackCount: 0 }),
  classifyPersona: jest.fn(),
  generateInitialGoals: jest.fn(),
  getDailyMotivation: jest.fn(),
};

const mockMessaging = { send: jest.fn().mockResolvedValue('msg-id') };

export async function createTestApp(): Promise<{ app: INestApplication; mongod: MongoMemoryServer }> {
  const mongod = await MongoMemoryServer.create();
  process.env['MONGO_URI'] = mongod.getUri();

  const moduleFixture = await Test.createTestingModule({ imports: [AppModule] })
    .overrideProvider(ConfigService)
    .useFactory({ factory: () => ({ get: (key: string) => process.env[key] }) })
    .overrideProvider(AiService).useValue(mockAiService)
    .overrideProvider(FIREBASE_MESSAGING).useValue(mockMessaging)
    .overrideGuard(ThrottlerGuard).useValue({ canActivate: () => true })
    .compile();

  const app = moduleFixture.createNestApplication();
  app.setGlobalPrefix('api/v1');
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
  await app.init();

  return { app, mongod };
}

export async function closeTestApp(app: INestApplication, mongod: MongoMemoryServer): Promise<void> {
  await app.close();
  await mongod.stop();
}

export const agent = (app: INestApplication) => request(app.getHttpServer());

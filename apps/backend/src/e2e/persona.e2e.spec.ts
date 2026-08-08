import { INestApplication } from '@nestjs/common';
import { MongoMemoryServer } from 'mongodb-memory-server';
import { KNOWN_PERSONA_TYPES, OPEN_ANSWERS, agent, closeTestApp, createTestApp } from './app.helper';

describe('Persona classification (e2e)', () => {
  let app: INestApplication;
  let mongod: MongoMemoryServer;

  beforeAll(async () => {
    ({ app, mongod } = await createTestApp());
  });

  afterAll(async () => {
    await closeTestApp(app, mongod);
  });

  it('POST /auth/register — returns a valid personaType from the known set', async () => {
    const res = await agent(app)
      .post('/api/v1/auth/register')
      .send({
        email: 'e2e-persona@test.com',
        password: 'Password123',
        openAnswers: OPEN_ANSWERS,
      })
      .expect(201);

    expect(KNOWN_PERSONA_TYPES).toContain(res.body.personaType);
  });

  it('POST /auth/register — returns non-empty coreGoals from the portfolio', async () => {
    const res = await agent(app)
      .post('/api/v1/auth/register')
      .send({
        email: 'e2e-persona-2@test.com',
        password: 'Password123',
        openAnswers: OPEN_ANSWERS,
      })
      .expect(201);

    expect(Array.isArray(res.body.coreGoals)).toBe(true);
    expect(res.body.coreGoals.length).toBeGreaterThan(0);
    expect(res.body.coreGoals[0].description).toBeDefined();
  });

  it('POST /auth/register — rejects registration with wrong number of answers', async () => {
    await agent(app)
      .post('/api/v1/auth/register')
      .send({
        email: 'e2e-persona-bad@test.com',
        password: 'Password123',
        openAnswers: ['Only one answer'],
      })
      .expect(400);
  });
});

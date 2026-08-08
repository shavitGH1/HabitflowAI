import { INestApplication } from '@nestjs/common';
import { MongoMemoryServer } from 'mongodb-memory-server';
import { OPEN_ANSWERS, agent, closeTestApp, createTestApp } from './app.helper';

describe('Auth (e2e)', () => {
  let app: INestApplication;
  let mongod: MongoMemoryServer;
  let accessToken: string;
  let refreshToken: string;

  const credentials = { email: 'e2e-auth@test.com', password: 'Password123' };

  beforeAll(async () => {
    ({ app, mongod } = await createTestApp());
  });

  afterAll(async () => {
    await closeTestApp(app, mongod);
  });

  it('POST /auth/register — returns userId, personaType, and success', async () => {
    const res = await agent(app)
      .post('/api/v1/auth/register')
      .send({ ...credentials, openAnswers: OPEN_ANSWERS })
      .expect(201);

    expect(res.body.userId).toBeDefined();
    expect(res.body.personaType).toBeDefined();
    expect(res.body.success).toBe(true);
  });

  it('POST /auth/login — returns accessToken and refreshToken', async () => {
    const res = await agent(app)
      .post('/api/v1/auth/login')
      .send(credentials)
      .expect(200);

    expect(res.body.accessToken).toBeDefined();
    expect(res.body.refreshToken).toBeDefined();
    expect(res.body.success).toBe(true);

    accessToken = res.body.accessToken;
    refreshToken = res.body.refreshToken;
  });

  it('POST /auth/refresh — returns a new accessToken', async () => {
    const res = await agent(app)
      .post('/api/v1/auth/refresh')
      .send({ refreshToken })
      .expect(200);

    expect(res.body.accessToken).toBeDefined();
    expect(res.body.success).toBe(true);
  });

  it('POST /auth/logout — returns 200', async () => {
    await agent(app)
      .post('/api/v1/auth/logout')
      .set('Authorization', `Bearer ${accessToken}`)
      .expect(200);
  });

  it('POST /auth/refresh — returns 401 after logout (token invalidated)', async () => {
    await agent(app)
      .post('/api/v1/auth/refresh')
      .send({ refreshToken })
      .expect(401);
  });
});

import { INestApplication } from '@nestjs/common';
import { MongoMemoryServer } from 'mongodb-memory-server';
import { OPEN_ANSWERS, agent, closeTestApp, createTestApp } from './app.helper';

describe('Locations (e2e)', () => {
  let app: INestApplication;
  let mongod: MongoMemoryServer;
  let accessToken: string;
  let taskId: string;
  let taskDescription: string;

  beforeAll(async () => {
    ({ app, mongod } = await createTestApp());

    await agent(app)
      .post('/api/v1/auth/register')
      .send({ email: 'e2e-locations@test.com', password: 'Password123', openAnswers: OPEN_ANSWERS });

    const res = await agent(app)
      .post('/api/v1/auth/login')
      .send({ email: 'e2e-locations@test.com', password: 'Password123' });

    accessToken = res.body.accessToken;

    const home = await agent(app)
      .get('/api/v1/users/me/home')
      .set('Authorization', `Bearer ${accessToken}`)
      .expect(200);

    const firstGoal = home.body.coreGoals[0];
    taskId = firstGoal.id;
    taskDescription = firstGoal.description;
  });

  afterAll(async () => {
    await closeTestApp(app, mongod);
  });

  it('POST /locations — records a completion location (401 without token)', async () => {
    await agent(app)
      .post('/api/v1/locations')
      .send({ habitId: taskId, latitude: 32.0853, longitude: 34.7818 })
      .expect(401);

    const res = await agent(app)
      .post('/api/v1/locations')
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ habitId: taskId, taskTitle: taskDescription, latitude: 32.0853, longitude: 34.7818, timestamp: 1720000000000 })
      .expect(200);

    expect(res.body).toEqual({ success: true });
  });

  it('GET /locations/me — returns the user locations joined with the task title', async () => {
    const res = await agent(app)
      .get('/api/v1/locations/me')
      .set('Authorization', `Bearer ${accessToken}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body).toHaveLength(1);

    const location = res.body[0];
    expect(location.habitId).toBe(taskId);
    expect(location.taskTitle).toBe(taskDescription);
    expect(location.latitude).toBe(32.0853);
    expect(location.longitude).toBe(34.7818);
    expect(location.placeName).toBe('Mock Place');
  });

  it('GET /locations/me — requires authentication', async () => {
    await agent(app)
      .get('/api/v1/locations/me')
      .expect(401);
  });
});

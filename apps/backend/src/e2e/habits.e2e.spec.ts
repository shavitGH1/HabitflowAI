import { INestApplication } from '@nestjs/common';
import { MongoMemoryServer } from 'mongodb-memory-server';
import { GOAL, OPEN_ANSWERS, agent, closeTestApp, createTestApp } from './app.helper';

describe('Habits (e2e)', () => {
  let app: INestApplication;
  let mongod: MongoMemoryServer;
  let accessToken: string;
  let habitId: string;

  beforeAll(async () => {
    ({ app, mongod } = await createTestApp());

    await agent(app)
      .post('/api/v1/auth/register')
      .send({ email: 'e2e-habits@test.com', password: 'Password123', goal: GOAL, openAnswers: OPEN_ANSWERS });

    const res = await agent(app)
      .post('/api/v1/auth/login')
      .send({ email: 'e2e-habits@test.com', password: 'Password123' });

    accessToken = res.body.accessToken;
  });

  afterAll(async () => {
    await closeTestApp(app, mongod);
  });

  it('POST /habits — creates a habit and returns 201 with an id', async () => {
    const res = await agent(app)
      .post('/api/v1/habits')
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ title: 'Morning Run', frequency: 'daily' })
      .expect(201);

    expect(res.body.id).toBeDefined();
    expect(res.body.title).toBe('Morning Run');
    habitId = res.body.id;
  });

  it('GET /habits — includes the newly created habit', async () => {
    const res = await agent(app)
      .get('/api/v1/habits')
      .set('Authorization', `Bearer ${accessToken}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.some((h: { id: string }) => h.id === habitId)).toBe(true);
  });

  it('PATCH /habits/:id — updates the description', async () => {
    const res = await agent(app)
      .patch(`/api/v1/habits/${habitId}`)
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ description: 'Run 5 km every morning' })
      .expect(200);

    expect(res.body.description).toBe('Run 5 km every morning');
  });

  it('PATCH /habits/:id/complete — marks habit done and returns streak of 1', async () => {
    const res = await agent(app)
      .patch(`/api/v1/habits/${habitId}/complete`)
      .set('Authorization', `Bearer ${accessToken}`)
      .send({ note: 'Ran 5km along the river this morning' })
      .expect(200);

    expect(res.body.streak).toBe(1);
    expect(res.body.completionHistory).toHaveLength(1);
    expect(res.body.consistencyScore).toBeGreaterThan(0);
    expect(res.body.implementedAt).toBeUndefined();
    expect(res.body.completionNotes).toEqual([
      expect.objectContaining({ note: 'Ran 5km along the river this morning' }),
    ]);
  });

  it('DELETE /habits/:id — soft-deletes the habit', async () => {
    await agent(app)
      .delete(`/api/v1/habits/${habitId}`)
      .set('Authorization', `Bearer ${accessToken}`)
      .expect(200);
  });

  it('GET /habits — archived habit no longer appears in the list', async () => {
    const res = await agent(app)
      .get('/api/v1/habits')
      .set('Authorization', `Bearer ${accessToken}`)
      .expect(200);

    expect(res.body.every((h: { id: string }) => h.id !== habitId)).toBe(true);
  });
});

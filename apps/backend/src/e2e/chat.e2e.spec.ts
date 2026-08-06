import { AddressInfo } from 'net';
import { INestApplication } from '@nestjs/common';
import { MongoMemoryServer } from 'mongodb-memory-server';
import { io, Socket } from 'socket.io-client';
import { OPEN_ANSWERS, agent, closeTestApp, createTestApp } from './app.helper';

describe('Chat (e2e)', () => {
  let app: INestApplication;
  let mongod: MongoMemoryServer;
  let baseUrl: string;

  let userAId: string;
  let userAToken: string;
  let userBId: string;
  let userBToken: string;
  let userCId: string;
  let userCToken: string;
  let userDId: string;
  let chatId: string;

  const openSocket = (token: string): Promise<Socket> =>
    new Promise((resolve, reject) => {
      const socket = io(baseUrl, { auth: { token }, transports: ['websocket'] });
      socket.on('connect', () => resolve(socket));
      socket.on('connect_error', reject);
    });

  beforeAll(async () => {
    ({ app, mongod } = await createTestApp());
    await app.listen(0);
    const { port } = app.getHttpServer().address() as AddressInfo;
    baseUrl = `http://localhost:${port}`;

    const registerA = await agent(app)
      .post('/api/v1/auth/register')
      .send({ email: 'e2e-chat-a@test.com', password: 'Password123', openAnswers: OPEN_ANSWERS });
    userAId = registerA.body.userId;
    const loginA = await agent(app)
      .post('/api/v1/auth/login')
      .send({ email: 'e2e-chat-a@test.com', password: 'Password123' });
    userAToken = loginA.body.accessToken;

    const registerB = await agent(app)
      .post('/api/v1/auth/register')
      .send({ email: 'e2e-chat-b@test.com', password: 'Password123', openAnswers: OPEN_ANSWERS });
    userBId = registerB.body.userId;
    const loginB = await agent(app)
      .post('/api/v1/auth/login')
      .send({ email: 'e2e-chat-b@test.com', password: 'Password123' });
    userBToken = loginB.body.accessToken;

    const registerC = await agent(app)
      .post('/api/v1/auth/register')
      .send({ email: 'e2e-chat-c@test.com', password: 'Password123', openAnswers: OPEN_ANSWERS });
    userCId = registerC.body.userId;
    const loginC = await agent(app)
      .post('/api/v1/auth/login')
      .send({ email: 'e2e-chat-c@test.com', password: 'Password123' });
    userCToken = loginC.body.accessToken;

    const registerD = await agent(app)
      .post('/api/v1/auth/register')
      .send({ email: 'e2e-chat-d@test.com', password: 'Password123', openAnswers: OPEN_ANSWERS });
    userDId = registerD.body.userId;
  });

  afterAll(async () => {
    await closeTestApp(app, mongod);
  });

  it('POST /chats — creates a direct chat between two real users', async () => {
    const res = await agent(app)
      .post('/api/v1/chats')
      .set('Authorization', `Bearer ${userAToken}`)
      .send({ participantIds: [userBId] })
      .expect(201);

    expect(res.body.id).toBeDefined();
    expect(res.body.participantIds.sort()).toEqual([userAId, userBId].sort());
    chatId = res.body.id;
  });

  it('disconnects a WebSocket connection with no auth token', async () => {
    const socket = io(baseUrl, { auth: { token: '' }, transports: ['websocket'] });
    await new Promise<void>((resolve) => socket.on('disconnect', () => resolve()));
    socket.close();
  });

  it('delivers a message sent by one participant to the other in real time, and persists it', async () => {
    const socketA = await openSocket(userAToken);
    const socketB = await openSocket(userBToken);

    socketA.emit('joinChat', { chatId });
    socketB.emit('joinChat', { chatId });
    await new Promise((resolve) => setTimeout(resolve, 100));

    const received = new Promise<{ text: string; senderId: string }>((resolve) => {
      socketB.on('newMessage', resolve);
    });

    socketA.emit('sendMessage', { chatId, text: 'hey, still on for the run?' });

    const message = await received;
    expect(message.text).toBe('hey, still on for the run?');
    expect(message.senderId).toBe(userAId);

    socketA.disconnect();
    socketB.disconnect();

    const history = await agent(app)
      .get(`/api/v1/chats/${chatId}/messages`)
      .set('Authorization', `Bearer ${userAToken}`)
      .expect(200);

    expect(history.body.some((m: { text: string }) => m.text === 'hey, still on for the run?')).toBe(true);
  });

  it('does not let an outsider read the chat history', async () => {
    await agent(app)
      .get(`/api/v1/chats/${chatId}/messages`)
      .set('Authorization', `Bearer ${userCToken}`)
      .expect(403);
  });

  describe('Group chat lifecycle', () => {
    let groupId: string;

    it('POST /chats/group — A creates a group with B and C; A is owner and sole admin', async () => {
      const res = await agent(app)
        .post('/api/v1/chats')
        .set('Authorization', `Bearer ${userAToken}`)
        .send({ participantIds: [userBId, userCId], isGroup: true, name: 'Marathon Crew' })
        .expect(201);

      expect(res.body.owner).toBe(userAId);
      expect(res.body.admins).toEqual([userAId]);
      groupId = res.body.id;
    });

    it('rejects admin-only actions from a non-admin member', async () => {
      await agent(app)
        .patch(`/api/v1/chats/${groupId}/name`)
        .set('Authorization', `Bearer ${userBToken}`)
        .send({ name: 'Not allowed' })
        .expect(403);
    });

    it('lets the owner rename the group', async () => {
      const res = await agent(app)
        .patch(`/api/v1/chats/${groupId}/name`)
        .set('Authorization', `Bearer ${userAToken}`)
        .send({ name: 'Marathon Crew 2026' })
        .expect(200);

      expect(res.body.name).toBe('Marathon Crew 2026');
    });

    it('lets the owner add a new member', async () => {
      const res = await agent(app)
        .post(`/api/v1/chats/${groupId}/members`)
        .set('Authorization', `Bearer ${userAToken}`)
        .send({ userIds: [userDId] })
        .expect(201);

      expect(res.body.participantIds).toContain(userDId);
    });

    it('lets the owner promote B to admin', async () => {
      const res = await agent(app)
        .patch(`/api/v1/chats/${groupId}/admins`)
        .set('Authorization', `Bearer ${userAToken}`)
        .send({ userId: userBId })
        .expect(200);

      expect(res.body.admins).toEqual(expect.arrayContaining([userAId, userBId]));
    });

    it('lets the newly-promoted admin B remove member C', async () => {
      const res = await agent(app)
        .delete(`/api/v1/chats/${groupId}/members`)
        .set('Authorization', `Bearer ${userBToken}`)
        .send({ userId: userCId })
        .expect(200);

      expect(res.body.participantIds).not.toContain(userCId);
    });

    it('refuses to let an admin remove the owner', async () => {
      await agent(app)
        .delete(`/api/v1/chats/${groupId}/members`)
        .set('Authorization', `Bearer ${userBToken}`)
        .send({ userId: userAId })
        .expect(403);
    });

    it('reassigns ownership to the next admin when the owner leaves', async () => {
      await agent(app)
        .post(`/api/v1/chats/${groupId}/leave`)
        .set('Authorization', `Bearer ${userAToken}`)
        .expect(201);

      const res = await agent(app)
        .get('/api/v1/chats')
        .set('Authorization', `Bearer ${userBToken}`)
        .expect(200);

      const group = res.body.find((c: { id: string }) => c.id === groupId);
      expect(group.owner).toBe(userBId);
      expect(group.participantIds).not.toContain(userAId);
    });

    it('lets the new owner delete the group', async () => {
      await agent(app)
        .delete(`/api/v1/chats/${groupId}`)
        .set('Authorization', `Bearer ${userBToken}`)
        .expect(200);

      const res = await agent(app)
        .get('/api/v1/chats')
        .set('Authorization', `Bearer ${userBToken}`)
        .expect(200);

      expect(res.body.some((c: { id: string }) => c.id === groupId)).toBe(false);
    });
  });
});

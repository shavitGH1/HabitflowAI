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
    const registerC = await agent(app)
      .post('/api/v1/auth/register')
      .send({ email: 'e2e-chat-c@test.com', password: 'Password123', openAnswers: OPEN_ANSWERS });
    void registerC;
    const loginC = await agent(app)
      .post('/api/v1/auth/login')
      .send({ email: 'e2e-chat-c@test.com', password: 'Password123' });

    await agent(app)
      .get(`/api/v1/chats/${chatId}/messages`)
      .set('Authorization', `Bearer ${loginC.body.accessToken}`)
      .expect(403);
  });
});

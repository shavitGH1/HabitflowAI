import { Test, TestingModule } from '@nestjs/testing';
import { ConfigService } from '@nestjs/config';
import jwt from 'jsonwebtoken';
import { ChatGateway } from './chat.gateway';
import { ChatService } from './chat.service';

const JWT_SECRET = 'test-secret';
const USER_ID = 'user-123';
const CHAT_ID = 'chat-abc';

const mockChatService = {
  assertParticipant: jest.fn(),
  postMessage: jest.fn(),
};

const mockConfigService = {
  get: jest.fn().mockImplementation((key: string) => (key === 'JWT_SECRET' ? JWT_SECRET : undefined)),
};

const makeSocket = (overrides: Partial<{ auth: Record<string, unknown>; headers: Record<string, unknown> }> = {}) => ({
  id: 'socket-1',
  data: {} as Record<string, unknown>,
  handshake: {
    auth: overrides.auth ?? {},
    headers: overrides.headers ?? {},
  },
  emit: jest.fn(),
  disconnect: jest.fn(),
  join: jest.fn(),
  leave: jest.fn(),
  to: jest.fn().mockReturnValue({ emit: jest.fn() }),
});

describe('ChatGateway', () => {
  let gateway: ChatGateway;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ChatGateway,
        { provide: ChatService, useValue: mockChatService },
        { provide: ConfigService, useValue: mockConfigService },
      ],
    }).compile();

    gateway = module.get<ChatGateway>(ChatGateway);
    gateway.server = { to: jest.fn().mockReturnValue({ emit: jest.fn() }) } as never;
    jest.clearAllMocks();
  });

  describe('handleConnection()', () => {
    it('authenticates and stores userId on the socket when the token is valid', () => {
      const token = jwt.sign({ id: USER_ID }, JWT_SECRET);
      const client = makeSocket({ auth: { token } });

      gateway.handleConnection(client as never);

      expect(client.data.userId).toBe(USER_ID);
      expect(client.disconnect).not.toHaveBeenCalled();
    });

    it('joins the personal per-user room on a successful connection', () => {
      const token = jwt.sign({ id: USER_ID }, JWT_SECRET);
      const client = makeSocket({ auth: { token } });

      gateway.handleConnection(client as never);

      expect(client.join).toHaveBeenCalledWith(`user:${USER_ID}`);
    });

    it('accepts a token from the Authorization header as a fallback', () => {
      const token = jwt.sign({ id: USER_ID }, JWT_SECRET);
      const client = makeSocket({ headers: { authorization: `Bearer ${token}` } });

      gateway.handleConnection(client as never);

      expect(client.data.userId).toBe(USER_ID);
    });

    it('disconnects a client with no token', () => {
      const client = makeSocket();

      gateway.handleConnection(client as never);

      expect(client.emit).toHaveBeenCalledWith('error', expect.objectContaining({ message: 'Unauthorized' }));
      expect(client.disconnect).toHaveBeenCalledWith(true);
    });

    it('disconnects a client with an invalid token', () => {
      const client = makeSocket({ auth: { token: 'not-a-real-token' } });

      gateway.handleConnection(client as never);

      expect(client.disconnect).toHaveBeenCalledWith(true);
    });

    it('disconnects a client whose token was signed with the wrong secret', () => {
      const token = jwt.sign({ id: USER_ID }, 'wrong-secret');
      const client = makeSocket({ auth: { token } });

      gateway.handleConnection(client as never);

      expect(client.disconnect).toHaveBeenCalledWith(true);
    });
  });

  describe('joinChat()', () => {
    it('checks participation before joining the room', async () => {
      const client = makeSocket();
      client.data.userId = USER_ID;
      mockChatService.assertParticipant.mockResolvedValue(undefined);

      await gateway.joinChat(client as never, { chatId: CHAT_ID });

      expect(mockChatService.assertParticipant).toHaveBeenCalledWith(USER_ID, CHAT_ID);
      expect(client.join).toHaveBeenCalledWith(CHAT_ID);
    });

    it('propagates the authorization error and does not join the room', async () => {
      const client = makeSocket();
      client.data.userId = USER_ID;
      mockChatService.assertParticipant.mockRejectedValue(new Error('forbidden'));

      await expect(gateway.joinChat(client as never, { chatId: CHAT_ID })).rejects.toThrow('forbidden');
      expect(client.join).not.toHaveBeenCalled();
    });

    it('broadcasts userJoined to the rest of the room, not back to the joiner', async () => {
      const client = makeSocket();
      client.data.userId = USER_ID;
      mockChatService.assertParticipant.mockResolvedValue(undefined);
      const emit = jest.fn();
      client.to.mockReturnValue({ emit });

      await gateway.joinChat(client as never, { chatId: CHAT_ID });

      expect(client.to).toHaveBeenCalledWith(CHAT_ID);
      expect(emit).toHaveBeenCalledWith('userJoined', { chatId: CHAT_ID, userId: USER_ID });
    });
  });

  describe('leaveChat()', () => {
    it('leaves the room and broadcasts userLeft to the rest of the room', async () => {
      const client = makeSocket();
      client.data.userId = USER_ID;
      const emit = jest.fn();
      client.to.mockReturnValue({ emit });

      await gateway.leaveChat(client as never, { chatId: CHAT_ID });

      expect(client.leave).toHaveBeenCalledWith(CHAT_ID);
      expect(client.to).toHaveBeenCalledWith(CHAT_ID);
      expect(emit).toHaveBeenCalledWith('userLeft', { chatId: CHAT_ID, userId: USER_ID });
    });
  });

  describe('handleTyping()', () => {
    it('broadcasts typing state to the rest of the room, not back to the sender', () => {
      const client = makeSocket();
      client.data.userId = USER_ID;
      const emit = jest.fn();
      client.to.mockReturnValue({ emit });

      gateway.handleTyping(client as never, { chatId: CHAT_ID, isTyping: true });

      expect(client.to).toHaveBeenCalledWith(CHAT_ID);
      expect(emit).toHaveBeenCalledWith('typing', { userId: USER_ID, isTyping: true });
    });
  });

  describe('emitToUser()', () => {
    it('emits to the target user personal room, not a chat room', () => {
      const emit = jest.fn();
      gateway.server = { to: jest.fn().mockReturnValue({ emit }) } as never;

      gateway.emitToUser(USER_ID, 'memberAdded', { chatId: CHAT_ID });

      expect(gateway.server.to).toHaveBeenCalledWith(`user:${USER_ID}`);
      expect(emit).toHaveBeenCalledWith('memberAdded', { chatId: CHAT_ID });
    });
  });

  describe('sendMessage()', () => {
    it('persists the message and broadcasts it to the chat room', async () => {
      const client = makeSocket();
      client.data.userId = USER_ID;
      const savedMessage = { id: 'm1', chatId: CHAT_ID, senderId: USER_ID, text: 'hi', createdAt: 'now' };
      mockChatService.postMessage.mockResolvedValue(savedMessage);
      const emit = jest.fn();
      gateway.server = { to: jest.fn().mockReturnValue({ emit }) } as never;

      await gateway.sendMessage(client as never, { chatId: CHAT_ID, text: 'hi' });

      expect(mockChatService.postMessage).toHaveBeenCalledWith(USER_ID, CHAT_ID, 'hi', undefined);
      expect(gateway.server.to).toHaveBeenCalledWith(CHAT_ID);
      expect(emit).toHaveBeenCalledWith('newMessage', savedMessage);
    });
  });
});

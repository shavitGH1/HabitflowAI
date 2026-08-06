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
  });

  describe('sendMessage()', () => {
    it('persists the message and broadcasts it to the chat room', async () => {
      const client = makeSocket();
      client.data.userId = USER_ID;
      const savedMessage = { id: 'm1', chatId: CHAT_ID, senderId: USER_ID, text: 'hi', sentAt: 'now' };
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

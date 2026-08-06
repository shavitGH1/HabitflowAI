import { BadRequestException, ForbiddenException, NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { ChatData, ChatRepository, MessageData } from './chat.repository';
import { ChatService } from './chat.service';
import { UserRepository } from '../users/user.repository';

const mockChatRepository = {
  createChat: jest.fn(),
  findDirectChatBetween: jest.fn(),
  findById: jest.fn(),
  findByParticipantId: jest.fn(),
  addMessage: jest.fn(),
  findMessagesPaginated: jest.fn(),
};

const mockUserRepository = {
  findUserById: jest.fn(),
};

const USER_ID = 'user-123';
const OTHER_USER_ID = 'user-456';
const OUTSIDER_ID = 'user-999';
const CHAT_ID = 'chat-abc';

const makeChat = (overrides: Partial<ChatData> = {}): ChatData => ({
  id: CHAT_ID,
  participantIds: [USER_ID, OTHER_USER_ID],
  isGroup: false,
  createdAt: new Date().toISOString(),
  ...overrides,
});

describe('ChatService', () => {
  let service: ChatService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ChatService,
        { provide: ChatRepository, useValue: mockChatRepository },
        { provide: UserRepository, useValue: mockUserRepository },
      ],
    }).compile();

    service = module.get<ChatService>(ChatService);
    jest.clearAllMocks();
  });

  describe('createChat()', () => {
    it('rejects a direct chat with more than one other participant', async () => {
      await expect(
        service.createChat(USER_ID, { participantIds: [OTHER_USER_ID, OUTSIDER_ID] }),
      ).rejects.toThrow(BadRequestException);
      expect(mockChatRepository.createChat).not.toHaveBeenCalled();
    });

    it('rejects when a participant id does not correspond to a real user', async () => {
      mockUserRepository.findUserById.mockResolvedValue(null);

      await expect(
        service.createChat(USER_ID, { participantIds: [OTHER_USER_ID] }),
      ).rejects.toThrow(BadRequestException);
      expect(mockChatRepository.createChat).not.toHaveBeenCalled();
    });

    it('returns the existing direct chat instead of creating a duplicate', async () => {
      mockUserRepository.findUserById.mockResolvedValue({ id: OTHER_USER_ID });
      const existing = makeChat();
      mockChatRepository.findDirectChatBetween.mockResolvedValue(existing);

      const result = await service.createChat(USER_ID, { participantIds: [OTHER_USER_ID] });

      expect(result).toEqual(existing);
      expect(mockChatRepository.createChat).not.toHaveBeenCalled();
    });

    it('creates a new direct chat when none exists yet', async () => {
      mockUserRepository.findUserById.mockResolvedValue({ id: OTHER_USER_ID });
      mockChatRepository.findDirectChatBetween.mockResolvedValue(null);
      const created = makeChat();
      mockChatRepository.createChat.mockResolvedValue(created);

      const result = await service.createChat(USER_ID, { participantIds: [OTHER_USER_ID] });

      expect(mockChatRepository.createChat).toHaveBeenCalledWith(
        expect.objectContaining({ participantIds: [USER_ID, OTHER_USER_ID], isGroup: false }),
      );
      expect(result).toEqual(created);
    });

    it('allows a group chat with more than one other participant and skips dedup lookup', async () => {
      mockUserRepository.findUserById.mockResolvedValue({ id: OTHER_USER_ID });
      const created = makeChat({ isGroup: true, participantIds: [USER_ID, OTHER_USER_ID, OUTSIDER_ID] });
      mockChatRepository.createChat.mockResolvedValue(created);

      const result = await service.createChat(USER_ID, {
        participantIds: [OTHER_USER_ID, OUTSIDER_ID],
        isGroup: true,
        name: 'Marathon Crew',
      });

      expect(mockChatRepository.findDirectChatBetween).not.toHaveBeenCalled();
      expect(result).toEqual(created);
    });
  });

  describe('assertParticipant()', () => {
    it('throws NotFoundException when the chat does not exist', async () => {
      mockChatRepository.findById.mockResolvedValue(null);

      await expect(service.assertParticipant(USER_ID, CHAT_ID)).rejects.toThrow(NotFoundException);
    });

    it('throws ForbiddenException when the user is not a participant', async () => {
      mockChatRepository.findById.mockResolvedValue(makeChat());

      await expect(service.assertParticipant(OUTSIDER_ID, CHAT_ID)).rejects.toThrow(ForbiddenException);
    });
  });

  describe('getMessages()', () => {
    it('rejects a non-participant before hitting the repository', async () => {
      mockChatRepository.findById.mockResolvedValue(makeChat());

      await expect(service.getMessages(OUTSIDER_ID, CHAT_ID, 1, 30)).rejects.toThrow(ForbiddenException);
      expect(mockChatRepository.findMessagesPaginated).not.toHaveBeenCalled();
    });

    it('returns paginated messages for a participant', async () => {
      mockChatRepository.findById.mockResolvedValue(makeChat());
      const messages: MessageData[] = [
        { id: 'm1', chatId: CHAT_ID, senderId: USER_ID, text: 'hey', sentAt: new Date().toISOString() },
      ];
      mockChatRepository.findMessagesPaginated.mockResolvedValue(messages);

      const result = await service.getMessages(USER_ID, CHAT_ID, 1, 30);

      expect(mockChatRepository.findMessagesPaginated).toHaveBeenCalledWith(CHAT_ID, 1, 30);
      expect(result).toEqual(messages);
    });
  });

  describe('postMessage()', () => {
    it('rejects a message with neither text nor an image', async () => {
      mockChatRepository.findById.mockResolvedValue(makeChat());

      await expect(service.postMessage(USER_ID, CHAT_ID)).rejects.toThrow(BadRequestException);
      expect(mockChatRepository.addMessage).not.toHaveBeenCalled();
    });

    it('persists a text message from a participant', async () => {
      mockChatRepository.findById.mockResolvedValue(makeChat());
      const saved: MessageData = {
        id: 'm1',
        chatId: CHAT_ID,
        senderId: USER_ID,
        text: 'hello',
        sentAt: new Date().toISOString(),
      };
      mockChatRepository.addMessage.mockResolvedValue(saved);

      const result = await service.postMessage(USER_ID, CHAT_ID, 'hello');

      expect(mockChatRepository.addMessage).toHaveBeenCalledWith({
        chatId: CHAT_ID,
        senderId: USER_ID,
        text: 'hello',
        imageUrl: undefined,
      });
      expect(result).toEqual(saved);
    });
  });
});

import { BadRequestException, ForbiddenException, NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { ChatData, ChatRepository, MessageData } from './chat.repository';
import { ChatService } from './chat.service';
import { UserRepository } from '../users/user.repository';
import { IStorageAdapter, STORAGE_ADAPTER } from '../storage/storage.adapter';

const mockChatRepository = {
  createChat: jest.fn(),
  findDirectChatBetween: jest.fn(),
  findById: jest.fn(),
  findByParticipantId: jest.fn(),
  addMessage: jest.fn(),
  findMessagesPaginated: jest.fn(),
  findMessageById: jest.fn(),
  setMessageLikes: jest.fn(),
  updateChat: jest.fn(),
  deleteChat: jest.fn(),
};

const mockUserRepository = {
  findUserById: jest.fn(),
};

const mockStorage: jest.Mocked<IStorageAdapter> = {
  upload: jest.fn(),
  delete: jest.fn(),
};

const USER_ID = 'user-123';
const OTHER_USER_ID = 'user-456';
const OUTSIDER_ID = 'user-999';
const THIRD_USER_ID = 'user-789';
const CHAT_ID = 'chat-abc';
const GROUP_CHAT_ID = 'chat-group-1';

const makeChat = (overrides: Partial<ChatData> = {}): ChatData => ({
  id: CHAT_ID,
  participantIds: [USER_ID, OTHER_USER_ID],
  isGroup: false,
  isPublic: false,
  admins: [],
  unreadCount: {},
  pinnedBy: [],
  mutedBy: [],
  createdAt: new Date().toISOString(),
  ...overrides,
});

const makeGroupChat = (overrides: Partial<ChatData> = {}): ChatData => ({
  id: GROUP_CHAT_ID,
  participantIds: [USER_ID, OTHER_USER_ID, THIRD_USER_ID],
  isGroup: true,
  isPublic: false,
  name: 'Marathon Crew',
  owner: USER_ID,
  admins: [USER_ID],
  unreadCount: {},
  pinnedBy: [],
  mutedBy: [],
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
        { provide: STORAGE_ADAPTER, useValue: mockStorage },
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

    it('defaults a new group chat to private (isPublic: false) when omitted', async () => {
      mockUserRepository.findUserById.mockResolvedValue({ id: OTHER_USER_ID });
      mockChatRepository.createChat.mockResolvedValue(makeGroupChat());

      await service.createChat(USER_ID, {
        participantIds: [OTHER_USER_ID, THIRD_USER_ID],
        isGroup: true,
        name: 'Marathon Crew',
      });

      expect(mockChatRepository.createChat).toHaveBeenCalledWith(
        expect.objectContaining({ isPublic: false }),
      );
    });

    it('respects an explicit isPublic: true on a new group chat', async () => {
      mockUserRepository.findUserById.mockResolvedValue({ id: OTHER_USER_ID });
      mockChatRepository.createChat.mockResolvedValue(makeGroupChat({ isPublic: true }));

      await service.createChat(USER_ID, {
        participantIds: [OTHER_USER_ID, THIRD_USER_ID],
        isGroup: true,
        name: 'Marathon Crew',
        isPublic: true,
      });

      expect(mockChatRepository.createChat).toHaveBeenCalledWith(
        expect.objectContaining({ isPublic: true }),
      );
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
    it('delegates pagination straight to the repository (authorization is ChatMemberGuard\'s job)', async () => {
      const messages: MessageData[] = [
        { id: 'm1', chatId: CHAT_ID, senderId: USER_ID, text: 'hey', likes: [], createdAt: new Date().toISOString() },
      ];
      mockChatRepository.findMessagesPaginated.mockResolvedValue(messages);

      const result = await service.getMessages(CHAT_ID, 1, 30);

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
        likes: [],
        createdAt: new Date().toISOString(),
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

    it('increments unread count for every other participant, not the sender, and sets lastMessage', async () => {
      const chat = makeGroupChat({ unreadCount: { [OTHER_USER_ID]: 2 } });
      mockChatRepository.findById.mockResolvedValue(chat);
      const saved: MessageData = {
        id: 'm2',
        chatId: GROUP_CHAT_ID,
        senderId: USER_ID,
        text: 'hi all',
        likes: [],
        createdAt: new Date().toISOString(),
      };
      mockChatRepository.addMessage.mockResolvedValue(saved);

      await service.postMessage(USER_ID, GROUP_CHAT_ID, 'hi all');

      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(GROUP_CHAT_ID, {
        lastMessage: 'hi all',
        lastMessageAt: expect.any(Date),
        unreadCount: { [OTHER_USER_ID]: 3, [THIRD_USER_ID]: 1 },
      });
    });

    it('sets a photo placeholder as lastMessage when the message has no text', async () => {
      const chat = makeGroupChat({ unreadCount: {} });
      mockChatRepository.findById.mockResolvedValue(chat);
      const saved: MessageData = {
        id: 'm3',
        chatId: GROUP_CHAT_ID,
        senderId: USER_ID,
        imageUrl: '/uploads/photo.jpg',
        likes: [],
        createdAt: new Date().toISOString(),
      };
      mockChatRepository.addMessage.mockResolvedValue(saved);

      await service.postMessage(USER_ID, GROUP_CHAT_ID, undefined, '/uploads/photo.jpg');

      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(GROUP_CHAT_ID, {
        lastMessage: '📷 Photo',
        lastMessageAt: expect.any(Date),
        unreadCount: { [OTHER_USER_ID]: 1, [THIRD_USER_ID]: 1 },
      });
    });
  });

  describe('markAsRead()', () => {
    it('resets only the caller unread count, leaving other participants untouched', async () => {
      const chat = makeGroupChat({ unreadCount: { [USER_ID]: 5, [OTHER_USER_ID]: 2 } });
      mockChatRepository.updateChat.mockResolvedValue(chat);

      await service.markAsRead(USER_ID, chat);

      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(GROUP_CHAT_ID, {
        unreadCount: { [USER_ID]: 0, [OTHER_USER_ID]: 2 },
      });
    });
  });

  describe('togglePin()', () => {
    it('pins a chat that is not yet pinned', async () => {
      const chat = makeChat({ pinnedBy: [] });
      mockChatRepository.findByParticipantId.mockResolvedValue([chat]);
      mockChatRepository.updateChat.mockResolvedValue(makeChat({ pinnedBy: [USER_ID] }));

      const result = await service.togglePin(USER_ID, chat);

      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(CHAT_ID, { pinnedBy: [USER_ID] });
      expect(result.pinnedBy).toEqual([USER_ID]);
    });

    it('unpins an already-pinned chat without checking the limit', async () => {
      const chat = makeChat({ pinnedBy: [USER_ID] });
      mockChatRepository.updateChat.mockResolvedValue(makeChat({ pinnedBy: [] }));

      await service.togglePin(USER_ID, chat);

      expect(mockChatRepository.findByParticipantId).not.toHaveBeenCalled();
      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(CHAT_ID, { pinnedBy: [] });
    });

    it('rejects pinning a 4th chat once already at the 3-chat limit', async () => {
      const chat = makeChat({ id: 'chat-4', pinnedBy: [] });
      const alreadyPinned = [
        makeChat({ id: 'chat-1', pinnedBy: [USER_ID] }),
        makeChat({ id: 'chat-2', pinnedBy: [USER_ID] }),
        makeChat({ id: 'chat-3', pinnedBy: [USER_ID] }),
      ];
      mockChatRepository.findByParticipantId.mockResolvedValue([...alreadyPinned, chat]);

      await expect(service.togglePin(USER_ID, chat)).rejects.toThrow(BadRequestException);
      expect(mockChatRepository.updateChat).not.toHaveBeenCalled();
    });
  });

  describe('toggleMute()', () => {
    it('mutes a chat that is not yet muted', async () => {
      const chat = makeChat({ mutedBy: [] });
      mockChatRepository.updateChat.mockResolvedValue(makeChat({ mutedBy: [USER_ID] }));

      const result = await service.toggleMute(USER_ID, chat);

      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(CHAT_ID, { mutedBy: [USER_ID] });
      expect(result.mutedBy).toEqual([USER_ID]);
    });

    it('unmutes an already-muted chat', async () => {
      const chat = makeChat({ mutedBy: [USER_ID] });
      mockChatRepository.updateChat.mockResolvedValue(makeChat({ mutedBy: [] }));

      await service.toggleMute(USER_ID, chat);

      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(CHAT_ID, { mutedBy: [] });
    });
  });

  describe('toggleMessageLike()', () => {
    const MESSAGE_ID = 'm1';

    it('throws NotFoundException for a missing message', async () => {
      mockChatRepository.findMessageById.mockResolvedValue(null);

      await expect(service.toggleMessageLike(USER_ID, CHAT_ID, MESSAGE_ID)).rejects.toThrow(NotFoundException);
    });

    it('adds the caller to likes when not already liked', async () => {
      mockChatRepository.findMessageById.mockResolvedValue({
        id: MESSAGE_ID,
        chatId: CHAT_ID,
        senderId: OTHER_USER_ID,
        text: 'hey',
        likes: [],
        createdAt: new Date().toISOString(),
      });

      await service.toggleMessageLike(USER_ID, CHAT_ID, MESSAGE_ID);

      expect(mockChatRepository.setMessageLikes).toHaveBeenCalledWith(MESSAGE_ID, [USER_ID]);
    });

    it('removes the caller from likes when already liked', async () => {
      mockChatRepository.findMessageById.mockResolvedValue({
        id: MESSAGE_ID,
        chatId: CHAT_ID,
        senderId: OTHER_USER_ID,
        text: 'hey',
        likes: [USER_ID, OTHER_USER_ID],
        createdAt: new Date().toISOString(),
      });

      await service.toggleMessageLike(USER_ID, CHAT_ID, MESSAGE_ID);

      expect(mockChatRepository.setMessageLikes).toHaveBeenCalledWith(MESSAGE_ID, [OTHER_USER_ID]);
    });
  });

  describe('group creation', () => {
    it('makes the requester owner and sole initial admin of a new group', async () => {
      mockUserRepository.findUserById.mockResolvedValue({ id: OTHER_USER_ID });
      const created = makeGroupChat();
      mockChatRepository.createChat.mockResolvedValue(created);

      await service.createChat(USER_ID, {
        participantIds: [OTHER_USER_ID, THIRD_USER_ID],
        isGroup: true,
        name: 'Marathon Crew',
      });

      expect(mockChatRepository.createChat).toHaveBeenCalledWith(
        expect.objectContaining({ owner: USER_ID, admins: [USER_ID] }),
      );
    });
  });

  describe('group-only guard (getGroupChat)', () => {
    it('rejects group-only actions on a direct chat', async () => {
      mockChatRepository.findById.mockResolvedValue(makeChat());

      await expect(service.renameGroup(CHAT_ID, 'New name')).rejects.toThrow(BadRequestException);
    });
  });

  describe('addMembers()', () => {
    it('rejects an unknown participant id', async () => {
      mockChatRepository.findById.mockResolvedValue(makeGroupChat());
      mockUserRepository.findUserById.mockResolvedValue(null);

      await expect(service.addMembers(GROUP_CHAT_ID, ['ghost'])).rejects.toThrow(BadRequestException);
      expect(mockChatRepository.updateChat).not.toHaveBeenCalled();
    });

    it('dedupes against existing participants', async () => {
      const chat = makeGroupChat();
      mockChatRepository.findById.mockResolvedValue(chat);
      mockUserRepository.findUserById.mockResolvedValue({ id: OTHER_USER_ID });
      mockChatRepository.updateChat.mockResolvedValue(chat);

      await service.addMembers(GROUP_CHAT_ID, [OTHER_USER_ID]);

      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(GROUP_CHAT_ID, {
        participantIds: chat.participantIds,
      });
    });
  });

  describe('removeMember()', () => {
    it('refuses to remove the group owner', async () => {
      mockChatRepository.findById.mockResolvedValue(makeGroupChat());

      await expect(service.removeMember(GROUP_CHAT_ID, USER_ID)).rejects.toThrow(ForbiddenException);
      expect(mockChatRepository.updateChat).not.toHaveBeenCalled();
    });

    it('removes a non-owner member and keeps them out of admins', async () => {
      const chat = makeGroupChat({ admins: [USER_ID, OTHER_USER_ID] });
      mockChatRepository.findById.mockResolvedValue(chat);
      mockChatRepository.updateChat.mockResolvedValue(chat);

      await service.removeMember(GROUP_CHAT_ID, OTHER_USER_ID);

      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(GROUP_CHAT_ID, {
        participantIds: [USER_ID, THIRD_USER_ID],
        admins: [USER_ID],
        owner: USER_ID,
      });
    });
  });

  describe('leaveGroup()', () => {
    it('rejects a non-participant', async () => {
      mockChatRepository.findById.mockResolvedValue(makeGroupChat());

      await expect(service.leaveGroup(OUTSIDER_ID, GROUP_CHAT_ID)).rejects.toThrow(ForbiddenException);
    });

    it('reassigns owner and admin when the owner leaves', async () => {
      const chat = makeGroupChat();
      mockChatRepository.findById.mockResolvedValue(chat);
      mockChatRepository.updateChat.mockResolvedValue(chat);

      await service.leaveGroup(USER_ID, GROUP_CHAT_ID);

      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(GROUP_CHAT_ID, {
        participantIds: [OTHER_USER_ID, THIRD_USER_ID],
        admins: [OTHER_USER_ID],
        owner: OTHER_USER_ID,
      });
    });

    it('deletes the group when the last member leaves', async () => {
      const chat = makeGroupChat({ participantIds: [USER_ID], admins: [USER_ID], owner: USER_ID });
      mockChatRepository.findById.mockResolvedValue(chat);

      const result = await service.leaveGroup(USER_ID, GROUP_CHAT_ID);

      expect(mockChatRepository.deleteChat).toHaveBeenCalledWith(GROUP_CHAT_ID);
      expect(mockChatRepository.updateChat).not.toHaveBeenCalled();
      expect(result).toBeNull();
    });
  });

  describe('addAdmin() / removeAdmin()', () => {
    it('rejects promoting someone who is not a member', async () => {
      mockChatRepository.findById.mockResolvedValue(makeGroupChat());

      await expect(service.addAdmin(GROUP_CHAT_ID, OUTSIDER_ID)).rejects.toThrow(BadRequestException);
    });

    it('promotes an existing member to admin', async () => {
      const chat = makeGroupChat();
      mockChatRepository.findById.mockResolvedValue(chat);
      mockChatRepository.updateChat.mockResolvedValue(chat);

      await service.addAdmin(GROUP_CHAT_ID, OTHER_USER_ID);

      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(GROUP_CHAT_ID, {
        admins: [USER_ID, OTHER_USER_ID],
      });
    });

    it('refuses to demote the last remaining admin', async () => {
      mockChatRepository.findById.mockResolvedValue(makeGroupChat());

      await expect(service.removeAdmin(GROUP_CHAT_ID, USER_ID)).rejects.toThrow(BadRequestException);
      expect(mockChatRepository.updateChat).not.toHaveBeenCalled();
    });

    it('demotes an admin when at least one other admin remains', async () => {
      const chat = makeGroupChat({ admins: [USER_ID, OTHER_USER_ID] });
      mockChatRepository.findById.mockResolvedValue(chat);
      mockChatRepository.updateChat.mockResolvedValue(chat);

      await service.removeAdmin(GROUP_CHAT_ID, OTHER_USER_ID);

      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(GROUP_CHAT_ID, { admins: [USER_ID] });
    });
  });

  describe('uploadGroupImage()', () => {
    it('uploads via the storage adapter and persists the returned URL', async () => {
      mockChatRepository.findById.mockResolvedValue(makeGroupChat());
      mockStorage.upload.mockResolvedValue('/uploads/group-123.jpg');
      const updated = makeGroupChat({ imageUrl: '/uploads/group-123.jpg' });
      mockChatRepository.updateChat.mockResolvedValue(updated);

      const file = {} as Express.Multer.File;
      const result = await service.uploadGroupImage(GROUP_CHAT_ID, file);

      expect(mockStorage.upload).toHaveBeenCalledWith(file);
      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(GROUP_CHAT_ID, {
        imageUrl: '/uploads/group-123.jpg',
      });
      expect(result.imageUrl).toBe('/uploads/group-123.jpg');
    });
  });

  describe('uploadMessageImage()', () => {
    it('uploads via the storage adapter and returns the URL without persisting anything', async () => {
      mockStorage.upload.mockResolvedValue('/uploads/msg-123.jpg');
      const file = {} as Express.Multer.File;

      const result = await service.uploadMessageImage(file);

      expect(mockStorage.upload).toHaveBeenCalledWith(file);
      expect(mockChatRepository.updateChat).not.toHaveBeenCalled();
      expect(result).toBe('/uploads/msg-123.jpg');
    });
  });

  describe('updateVisibility()', () => {
    it('rejects changing visibility on a direct chat', async () => {
      mockChatRepository.findById.mockResolvedValue(makeChat());

      await expect(service.updateVisibility(CHAT_ID, true)).rejects.toThrow(BadRequestException);
      expect(mockChatRepository.updateChat).not.toHaveBeenCalled();
    });

    it('makes a private group public', async () => {
      mockChatRepository.findById.mockResolvedValue(makeGroupChat({ isPublic: false }));
      mockChatRepository.updateChat.mockResolvedValue(makeGroupChat({ isPublic: true }));

      const result = await service.updateVisibility(GROUP_CHAT_ID, true);

      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(GROUP_CHAT_ID, { isPublic: true });
      expect(result.isPublic).toBe(true);
    });

    it('makes a public group private', async () => {
      mockChatRepository.findById.mockResolvedValue(makeGroupChat({ isPublic: true }));
      mockChatRepository.updateChat.mockResolvedValue(makeGroupChat({ isPublic: false }));

      const result = await service.updateVisibility(GROUP_CHAT_ID, false);

      expect(mockChatRepository.updateChat).toHaveBeenCalledWith(GROUP_CHAT_ID, { isPublic: false });
      expect(result.isPublic).toBe(false);
    });
  });

  describe('deleteGroup()', () => {
    it('rejects deleting a direct chat via the group-delete path', async () => {
      mockChatRepository.findById.mockResolvedValue(makeChat());

      await expect(service.deleteGroup(CHAT_ID)).rejects.toThrow(BadRequestException);
      expect(mockChatRepository.deleteChat).not.toHaveBeenCalled();
    });

    it('deletes a group chat and its messages', async () => {
      mockChatRepository.findById.mockResolvedValue(makeGroupChat());

      await service.deleteGroup(GROUP_CHAT_ID);

      expect(mockChatRepository.deleteChat).toHaveBeenCalledWith(GROUP_CHAT_ID);
    });
  });
});

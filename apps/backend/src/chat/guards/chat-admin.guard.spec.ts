import { ExecutionContext, ForbiddenException, NotFoundException } from '@nestjs/common';
import { ChatData, ChatRepository } from '../chat.repository';
import { ChatAdminGuard } from './chat-admin.guard';

const mockChatRepository = {
  findById: jest.fn(),
};

const USER_ID = 'user-123';
const CHAT_ID = 'chat-abc';

const makeChat = (overrides: Partial<ChatData> = {}): ChatData => ({
  id: CHAT_ID,
  participantIds: [USER_ID],
  isGroup: true,
  admins: [USER_ID],
  createdAt: new Date().toISOString(),
  ...overrides,
});

const makeContext = (userId: string, chatId: string): ExecutionContext =>
  ({
    switchToHttp: () => ({
      getRequest: () => ({ params: { chatId }, user: { id: userId } }),
    }),
  }) as unknown as ExecutionContext;

describe('ChatAdminGuard', () => {
  let guard: ChatAdminGuard;

  beforeEach(() => {
    guard = new ChatAdminGuard(mockChatRepository as unknown as ChatRepository);
    jest.clearAllMocks();
  });

  it('throws NotFoundException when the chat does not exist', async () => {
    mockChatRepository.findById.mockResolvedValue(null);

    await expect(guard.canActivate(makeContext(USER_ID, CHAT_ID))).rejects.toThrow(NotFoundException);
  });

  it('throws ForbiddenException for a direct (non-group) chat', async () => {
    mockChatRepository.findById.mockResolvedValue(makeChat({ isGroup: false }));

    await expect(guard.canActivate(makeContext(USER_ID, CHAT_ID))).rejects.toThrow(ForbiddenException);
  });

  it('throws ForbiddenException for a group member who is not an admin', async () => {
    mockChatRepository.findById.mockResolvedValue(makeChat({ admins: ['someone-else'] }));

    await expect(guard.canActivate(makeContext(USER_ID, CHAT_ID))).rejects.toThrow(ForbiddenException);
  });

  it('allows a group admin through', async () => {
    mockChatRepository.findById.mockResolvedValue(makeChat());

    await expect(guard.canActivate(makeContext(USER_ID, CHAT_ID))).resolves.toBe(true);
  });
});

import { ExecutionContext, ForbiddenException, NotFoundException } from '@nestjs/common';
import { ChatData, ChatRepository } from '../chat.repository';
import { ChatMemberGuard, ChatMemberRequest } from './chat-member.guard';

const mockChatRepository = {
  findById: jest.fn(),
};

const USER_ID = 'user-123';
const OUTSIDER_ID = 'user-999';
const CHAT_ID = 'chat-abc';

const makeChat = (overrides: Partial<ChatData> = {}): ChatData => ({
  id: CHAT_ID,
  participantIds: [USER_ID],
  isGroup: false,
  isPublic: false,
  admins: [],
  unreadCount: {},
  createdAt: new Date().toISOString(),
  ...overrides,
});

const makeContext = (userId: string, chatId: string): { context: ExecutionContext; request: Partial<ChatMemberRequest> } => {
  const request: Partial<ChatMemberRequest> = { params: { chatId }, user: { id: userId } };
  const context = {
    switchToHttp: () => ({ getRequest: () => request }),
  } as unknown as ExecutionContext;
  return { context, request };
};

describe('ChatMemberGuard', () => {
  let guard: ChatMemberGuard;

  beforeEach(() => {
    guard = new ChatMemberGuard(mockChatRepository as unknown as ChatRepository);
    jest.clearAllMocks();
  });

  it('throws NotFoundException when the chat does not exist', async () => {
    mockChatRepository.findById.mockResolvedValue(null);
    const { context } = makeContext(USER_ID, CHAT_ID);

    await expect(guard.canActivate(context)).rejects.toThrow(NotFoundException);
  });

  it('throws ForbiddenException for a non-participant', async () => {
    mockChatRepository.findById.mockResolvedValue(makeChat());
    const { context } = makeContext(OUTSIDER_ID, CHAT_ID);

    await expect(guard.canActivate(context)).rejects.toThrow(ForbiddenException);
  });

  it('allows a participant through and attaches the chat to the request', async () => {
    const chat = makeChat();
    mockChatRepository.findById.mockResolvedValue(chat);
    const { context, request } = makeContext(USER_ID, CHAT_ID);

    await expect(guard.canActivate(context)).resolves.toBe(true);
    expect(request.chat).toEqual(chat);
  });
});

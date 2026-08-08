import { Injectable, NotFoundException } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { AiService } from '../ai/ai.service';
import { PERSONA_TYPES, PersonaType } from '../ai/pillars';
import { ChatGateway } from '../chat/chat.gateway';
import { ChatService } from '../chat/chat.service';
import { MessageData } from '../chat/chat.repository';
import { ChatEvent } from '../chat/enums/chat-event.enum';
import { HabitRepository } from '../habits/habit.repository';
import { logger } from '../logger';
import { PersonasService } from '../personas/personas.service';
import { UserData, UserRepository } from '../users/user.repository';
import {
  CoachStats,
  computeStats,
  isDueForPersonaReview,
  pickBand,
  pickTip,
  toDateKey,
} from './coach.rules';
import {
  BAND_SENTENCES,
  COACH_USER_ID,
  NOTHING_DONE_TODAY,
  PERSONA_LINES,
  TIPS,
  completedTodayLine,
  personaSwitchLine,
} from './coach.templates';

@Injectable()
export class CoachService {
  constructor(
    private readonly chatService: ChatService,
    private readonly chatGateway: ChatGateway,
    private readonly habitRepository: HabitRepository,
    private readonly userRepository: UserRepository,
    private readonly personasService: PersonasService,
    private readonly ai: AiService,
  ) {}

  @Cron('0 20 * * *')
  async runDailyForAllUsers(): Promise<void> {
    await this.runForAllUsers('daily', (userId) => this.postDaily(userId));
  }

  @Cron('0 18 * * 0')
  async runWeeklyForAllUsers(): Promise<void> {
    await this.runForAllUsers('weekly', (userId) => this.postWeekly(userId));
  }

  async postDaily(userId: string, force = false): Promise<MessageData | null> {
    const user = await this.loadUser(userId);
    const chatId = await this.ensureCoachChat(userId);
    if (!force && (await this.alreadyPostedToday(chatId))) return null;

    const stats = await this.loadStats(userId);
    const base = stats.completedToday.length
      ? completedTodayLine(stats.completedToday)
      : NOTHING_DONE_TODAY;

    const text = await this.phrase(user, stats, `${base} ${this.personaLine(user)}`.trim(), 'daily');
    return this.post(chatId, text);
  }

  async postWeekly(userId: string, force = false): Promise<MessageData | null> {
    const user = await this.loadUser(userId);
    const chatId = await this.ensureCoachChat(userId);
    if (!force && (await this.alreadyPostedToday(chatId))) return null;

    const stats = await this.loadStats(userId);
    const tip = pickTip(stats);
    const base = [BAND_SENTENCES[pickBand(stats.completionRate7d)], this.personaLine(user), tip ? TIPS[tip] : '']
      .filter(Boolean)
      .join(' ');

    const phrased = await this.phrase(user, stats, base, 'weekly');
    const suggestion = await this.personaSuggestion(user, stats);

    return this.post(chatId, suggestion ? `${phrased} ${suggestion}` : phrased);
  }

  private async runForAllUsers(label: string, run: (userId: string) => Promise<unknown>): Promise<void> {
    const users = await this.userRepository.findAllUsers();
    logger.info({ count: users.length, job: label }, 'coach job started');

    for (const user of users) {
      if (user.id === COACH_USER_ID) continue;
      try {
        await run(user.id);
      } catch (error) {
        logger.error({ userId: user.id, job: label, err: error }, 'coach job failed for user');
      }
    }
  }

  private async loadUser(userId: string): Promise<UserData> {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');
    return user;
  }

  private async loadStats(userId: string): Promise<CoachStats> {
    return computeStats(await this.habitRepository.findByUserId(userId), new Date());
  }

  private async ensureCoachChat(userId: string): Promise<string> {
    const chat = await this.chatService.createChat(userId, {
      participantIds: [COACH_USER_ID],
      isGroup: false,
    });
    return chat.id;
  }

  private async alreadyPostedToday(chatId: string): Promise<boolean> {
    const [latest] = await this.chatService.getMessages(chatId, 1, 1);
    if (!latest || latest.senderId !== COACH_USER_ID) return false;
    return latest.sentAt.split('T')[0] === toDateKey(new Date());
  }

  private async post(chatId: string, text: string): Promise<MessageData> {
    const message = await this.chatService.postMessage(COACH_USER_ID, chatId, text);
    this.chatGateway.emitToRoom(chatId, ChatEvent.NEW_MESSAGE, { ...message });
    return message;
  }

  private phrase(user: UserData, stats: CoachStats, baseMessage: string, cacheTag: string): Promise<string> {
    const personaType = this.toPersonaType(user.personaType);
    if (!personaType) return Promise.resolve(baseMessage);

    return this.ai.phraseCoachMessage({
      userId: user.id,
      personaType,
      baseMessage,
      completionRate7d: stats.completionRate7d,
      streak: stats.streak,
      cacheTag,
    });
  }

  private async personaSuggestion(user: UserData, stats: CoachStats): Promise<string | null> {
    if (!isDueForPersonaReview(stats)) return null;

    try {
      const drift = await this.personasService.driftCheck(user.id);
      if (!drift.driftDetected || !drift.newSuggestedPersona) return null;
      return personaSwitchLine(drift.newSuggestedPersona);
    } catch (error) {
      logger.warn({ userId: user.id, err: error }, 'coach persona review skipped');
      return null;
    }
  }

  private personaLine(user: UserData): string {
    const personaType = this.toPersonaType(user.personaType);
    return personaType ? PERSONA_LINES[personaType] : '';
  }

  private toPersonaType(value: string): PersonaType | null {
    return (PERSONA_TYPES as readonly string[]).includes(value) ? (value as PersonaType) : null;
  }
}

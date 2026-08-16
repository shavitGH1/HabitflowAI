import { Injectable } from '@nestjs/common';
import { Type } from '@google/genai';
import { z } from 'zod';
import { AgentTool, defineTool, ToolRejectedError } from '../ai/agent/agent-tool';
import { PERSONA_TYPES, PersonaType } from '../ai/pillars';
import { proposedChangeSchema, ProposedChange } from '../ai/schemas/coaching-agent.schema';
import { ArticlesService } from '../articles/articles.service';
import { GoalRepository } from '../goals/goal.repository';
import { HabitData, HabitRepository } from '../habits/habit.repository';
import { logger } from '../logger';
import { PersonasService } from '../personas/personas.service';
import { ResearchChunksService } from '../research-chunks/research-chunks.service';
import { UserRepository } from '../users/user.repository';
import { DriftFinding, ProposalContext, rejectionReason } from './coach.policy';
import { CoachStats, computeStats, isGoalFailing, pickBand, weeklySummary } from './coach.rules';
import { COACH_OFFLINE_REPLY, COACH_UNAVAILABLE } from './coach.templates';

export class ProposalRejectedError extends ToolRejectedError {}

export interface CoachToolSession {
  tools: AgentTool[];
  proposedChange(): ProposedChange | null;
  fallbackReply(): Promise<string>;
}

interface SessionState {
  drift: DriftFinding;
  staged: ProposedChange | null;
}

@Injectable()
export class CoachToolset {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly habitRepository: HabitRepository,
    private readonly goalRepository: GoalRepository,
    private readonly personasService: PersonasService,
    private readonly articlesService: ArticlesService,
    private readonly researchChunksService: ResearchChunksService,
  ) {}

  open(userId: string): CoachToolSession {
    const state: SessionState = {
      drift: { checked: false, detected: false, suggestedPersona: null },
      staged: null,
    };

    return {
      tools: [
        this.progressSummaryTool(userId),
        this.habitListTool(userId),
        this.dailyTasksTool(userId),
        this.personaProfileTool(userId),
        this.activeGoalTool(userId),
        this.driftTool(userId, state),
        this.proposeChangeTool(userId, state),
        this.searchArticlesTool(),
        this.searchResearchTool(),
      ],
      proposedChange: () => state.staged,
      fallbackReply: () => this.fallbackReply(userId),
    };
  }

  private progressSummaryTool(userId: string): AgentTool {
    return defineTool({
      name: 'get_progress_summary',
      description:
        "The user's overall standing over the last 7 days, already scored by the rules engine: completion rate, longest active streak, the band (EXCELLENT / GOOD / SLIPPING / AT_RISK) and the verdict sentence that belongs to that band. Call this first for any question about how the user is doing. The band and the verdict come from fixed thresholds — rephrase them, never recompute or overrule them. For a habit-by-habit breakdown call get_habit_list instead.",
      parameters: { type: Type.OBJECT, properties: {} },
      argsSchema: z.object({}),
      execute: async () => {
        const [stats, personaType] = await Promise.all([
          this.loadStats(userId),
          this.loadPersona(userId),
        ]);
        return {
          completionRate7dPercent: Math.round(stats.completionRate7d * 100),
          longestActiveStreak: stats.streak,
          habitCount: stats.habitCount,
          band: pickBand(stats.completionRate7d),
          verdict: weeklySummary(stats, personaType),
          completedToday: stats.completedToday,
          notesToday: stats.notesToday,
        };
      },
    });
  }

  private habitListTool(userId: string): AgentTool {
    return defineTool({
      name: 'get_habit_list',
      description:
        'Every active habit of the user with its own streak, consistency percentage and the goal it is linked to. Call this when the user asks about a specific habit, or when you need to name habits in your reply. Do not use it to judge overall progress — get_progress_summary already carries that verdict. This only covers habits created in the Habits tab — for the AI-generated goals and daily tasks from onboarding shown on the Home screen, call get_daily_tasks instead.',
      parameters: { type: Type.OBJECT, properties: {} },
      argsSchema: z.object({}),
      execute: async () => {
        const habits = await this.habitRepository.findByUserId(userId);
        return {
          habits: habits.map((habit) => ({
            id: habit.id,
            title: habit.title,
            frequency: habit.frequency,
            streak: habit.streak,
            consistencyPercent: Math.round(habit.consistencyScore * 100),
            linkedGoalId: habit.goalId ?? null,
          })),
        };
      },
    });
  }

  private dailyTasksTool(userId: string): AgentTool {
    return defineTool({
      name: 'get_daily_tasks',
      description:
        "The AI-generated core goals and today's daily task variations from onboarding, shown to the user on the Home screen — separate from get_habit_list, which only covers habits created in the Habits tab. Call this when the user asks about their onboarding goals, Home screen tasks, or points, or when get_habit_list and get_progress_summary come back empty for a user who still completed onboarding.",
      parameters: { type: Type.OBJECT, properties: {} },
      argsSchema: z.object({}),
      execute: async () => {
        const user = await this.userRepository.findUserById(userId);
        if (!user) throw new Error('User not found');
        const toSummary = (tasks: { id: string; description: string; points: number; completed: boolean }[]) =>
          tasks.map((task) => ({
            id: task.id,
            description: task.description,
            points: task.points,
            completed: task.completed,
          }));
        return {
          coreGoals: toSummary(user.coreGoals),
          dailyTasks: toSummary(user.dailyVariations),
        };
      },
    });
  }

  private personaProfileTool(userId: string): AgentTool {
    return defineTool({
      name: 'get_persona_profile',
      description:
        "The coaching persona currently stored for the user, the pillar scores it was derived from, and the goal they stated at onboarding. Call this before tailoring advice to the user's style, or when they ask why the coach talks the way it does. This is the stored label only — whether it still fits is what check_persona_drift answers.",
      parameters: { type: Type.OBJECT, properties: {} },
      argsSchema: z.object({}),
      execute: async () => {
        const user = await this.userRepository.findUserById(userId);
        if (!user) throw new Error('User not found');
        return {
          personaType: user.personaType,
          pillarScores: user.weightedScores ?? user.personaBreakdown ?? {},
          statedGoal: user.goal,
        };
      },
    });
  }

  private activeGoalTool(userId: string): AgentTool {
    return defineTool({
      name: 'get_active_goal',
      description:
        'The single goal the user is currently working towards, with its exact id and target date, or activeGoal: null when there is none. Call this before discussing the goal, and always before proposing to forfeit it — the id must be copied from here verbatim.',
      parameters: { type: Type.OBJECT, properties: {} },
      argsSchema: z.object({}),
      execute: async () => {
        const goal = await this.goalRepository.findActiveByUserId(userId);
        if (!goal) return { activeGoal: null };
        return { activeGoal: { id: goal.id, title: goal.title, targetDate: goal.targetDate } };
      },
    });
  }

  private driftTool(userId: string, state: SessionState): AgentTool {
    return defineTool({
      name: 'check_persona_drift',
      description:
        'Expensive behavioural analysis comparing recent activity against the persona baseline. Reports whether the stored persona still fits and which persona the behaviour now points to. Call it only when the user questions their persona, or when get_progress_summary and get_persona_profile clearly contradict each other. It is required before any personaSwitch proposal. Calling it twice returns the same cached answer.',
      parameters: { type: Type.OBJECT, properties: {} },
      argsSchema: z.object({}),
      execute: async () => {
        if (state.drift.checked) return { ...this.driftPayload(state.drift), cached: true };

        const drift = await this.personasService.driftCheck(userId);
        state.drift = {
          checked: true,
          detected: drift.driftDetected,
          suggestedPersona: drift.driftDetected ? drift.newSuggestedPersona : null,
        };
        return { ...this.driftPayload(state.drift), driftScore: Number(drift.driftScore.toFixed(2)) };
      },
    });
  }

  private proposeChangeTool(userId: string, state: SessionState): AgentTool {
    return defineTool({
      name: 'propose_change',
      description:
        'Stage exactly one change for the user to accept or decline. Nothing is applied here and nothing changes until the user confirms it in the app. The backend re-checks every proposal against the same fixed thresholds the other tools reported and rejects it with the reason when the data does not support it, so read those tools first. At most one staged change per conversation.',
      parameters: {
        type: Type.OBJECT,
        properties: {
          type: {
            type: Type.STRING,
            enum: ['personaSwitch', 'adjustDifficulty', 'forfeitGoal'],
            description:
              'personaSwitch requires a prior check_persona_drift that detected drift. adjustDifficulty is judged against the completion rate from get_progress_summary. forfeitGoal requires the id from get_active_goal.',
          },
          rationale: {
            type: Type.STRING,
            description:
              'One short sentence quoting the number or fact from the tool output that justifies the change.',
          },
          suggestedPersona: {
            type: Type.STRING,
            enum: [...PERSONA_TYPES],
            description:
              'Only for personaSwitch. Must be exactly the persona that check_persona_drift suggested.',
          },
          direction: {
            type: Type.STRING,
            enum: ['increase', 'decrease'],
            description:
              "Only for adjustDifficulty. increase makes today's plan harder and needs a completion rate of at least 80%; decrease makes it easier and is only offered below 50%.",
          },
          goalId: {
            type: Type.STRING,
            description: 'Only for forfeitGoal. Must be the id returned by get_active_goal, verbatim.',
          },
        },
        required: ['type', 'rationale'],
      },
      argsSchema: proposedChangeSchema,
      execute: async (change) => {
        const reason = rejectionReason(change, await this.proposalContext(userId, change, state));
        if (reason) throw new ProposalRejectedError(reason);

        state.staged = change;
        return {
          staged: true,
          awaitingUserConfirmation: true,
          note: 'Tell the user what you proposed and that nothing changes until they confirm it.',
        };
      },
    });
  }

  private searchArticlesTool(): AgentTool {
    return defineTool({
      name: 'search_articles',
      description:
        'Search a small curated set of habit-formation articles (title + link) by meaning, not keyword. Call this when the user wants further reading or a resource to share — a concrete link, not an explanation. For the science/reasoning behind a suggestion, call search_research instead.',
      parameters: {
        type: Type.OBJECT,
        properties: {
          query: {
            type: Type.STRING,
            description: 'What the user wants reading material about, in their own words.',
          },
        },
        required: ['query'],
      },
      argsSchema: z.object({ query: z.string() }),
      execute: async ({ query }) => {
        const results = await this.articlesService.search(query);
        return { articles: results.map(({ title, url }) => ({ title, url })) };
      },
    });
  }

  private searchResearchTool(): AgentTool {
    return defineTool({
      name: 'search_research',
      description:
        'Search chunked behavior-change research (neuroscience, motivation frameworks) by meaning, not keyword. Call this when the user asks "why" a suggestion works, not when they just want a link — for that call search_articles instead. Quote or paraphrase the returned content rather than inventing an explanation.',
      parameters: {
        type: Type.OBJECT,
        properties: {
          query: {
            type: Type.STRING,
            description: 'What the user wants the underlying research/explanation for, in their own words.',
          },
        },
        required: ['query'],
      },
      argsSchema: z.object({ query: z.string() }),
      execute: async ({ query }) => {
        const results = await this.researchChunksService.search(query);
        return {
          chunks: results.map(({ sourceTitle, section, content }) => ({ sourceTitle, section, content })),
        };
      },
    });
  }

  private async proposalContext(
    userId: string,
    change: ProposedChange,
    state: SessionState,
  ): Promise<ProposalContext> {
    const activeGoal =
      change.type === 'forfeitGoal' ? await this.goalRepository.findActiveByUserId(userId) : null;

    return {
      stats: await this.loadStats(userId),
      drift: state.drift,
      activeGoalId: activeGoal?.id ?? null,
      goalIsFailing: activeGoal ? isGoalFailing(await this.goalHabits(userId, activeGoal.id)) : false,
      alreadyStaged: Boolean(state.staged),
    };
  }

  private async goalHabits(userId: string, goalId: string): Promise<HabitData[]> {
    const habits = await this.habitRepository.findByUserId(userId);
    return habits.filter((habit) => habit.goalId === goalId);
  }

  private async fallbackReply(userId: string): Promise<string> {
    try {
      const [stats, personaType] = await Promise.all([
        this.loadStats(userId),
        this.loadPersona(userId),
      ]);
      return `${COACH_UNAVAILABLE} ${weeklySummary(stats, personaType)}`;
    } catch (error) {
      logger.warn({ userId, err: error }, 'coach fallback summary unavailable');
      return COACH_OFFLINE_REPLY;
    }
  }

  private async loadStats(userId: string): Promise<CoachStats> {
    return computeStats(await this.habitRepository.findByUserId(userId), new Date());
  }

  private async loadPersona(userId: string): Promise<PersonaType | null> {
    const user = await this.userRepository.findUserById(userId);
    if (!user) return null;
    return (PERSONA_TYPES as readonly string[]).includes(user.personaType)
      ? (user.personaType as PersonaType)
      : null;
  }

  private driftPayload({ detected, suggestedPersona }: DriftFinding) {
    return { driftDetected: detected, suggestedPersona };
  }
}

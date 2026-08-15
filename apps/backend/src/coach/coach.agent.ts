import { Injectable } from '@nestjs/common';
import { AgentLoop, AgentMessage, AgentStep } from '../ai/agent/agent-loop';
import { buildCoachAgentSystemInstruction } from '../ai/prompts/coach-agent.prompt';
import { ProposedChange } from '../ai/schemas/coaching-agent.schema';
import { logger } from '../logger';
import { CoachToolSession, CoachToolset } from './coach.toolset';

export interface CoachAgentResult {
  reply: string;
  proposedChange: ProposedChange | null;
  toolsUsed: string[];
  fellBack: boolean;
}

const MAX_REPLY_LENGTH = 1000;

@Injectable()
export class CoachAgent {
  constructor(
    private readonly loop: AgentLoop,
    private readonly toolset: CoachToolset,
  ) {}

  async converse(userId: string, message: string, history: AgentMessage[]): Promise<CoachAgentResult> {
    const session = this.toolset.open(userId);

    try {
      const run = await this.loop.run({
        systemInstruction: buildCoachAgentSystemInstruction(),
        history,
        message,
        tools: session.tools,
      });

      const reply = run.text.trim().slice(0, MAX_REPLY_LENGTH);
      if (!reply) {
        logger.warn({ userId }, 'coach agent produced no text, using the deterministic summary');
        return this.fallback(session, run.steps);
      }

      logger.info(
        { userId, tools: this.toolNames(run.steps), stoppedOnStepLimit: run.stoppedOnStepLimit },
        'coach agent run finished',
      );
      return {
        reply,
        proposedChange: session.proposedChange(),
        toolsUsed: this.toolNames(run.steps),
        fellBack: false,
      };
    } catch (error) {
      logger.warn({ userId, err: error }, 'coach agent run failed, using the deterministic summary');
      return this.fallback(session, []);
    }
  }

  // A staged proposal is dropped on fallback: the reply that would explain it never reached the user.
  private async fallback(session: CoachToolSession, steps: AgentStep[]): Promise<CoachAgentResult> {
    return {
      reply: await session.fallbackReply(),
      proposedChange: null,
      toolsUsed: this.toolNames(steps),
      fellBack: true,
    };
  }

  private toolNames(steps: AgentStep[]): string[] {
    return Array.from(new Set(steps.map((step) => step.tool)));
  }
}

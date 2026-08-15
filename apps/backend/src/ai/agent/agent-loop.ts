import { Injectable } from '@nestjs/common';
import { Content, Part } from '@google/genai';
import { logger } from '../../logger';
import { GeminiClient, ToolCallRequest } from '../gemini.client';
import { AgentTool, InvalidToolArgumentsError, ToolRejectedError, toFunctionDeclarations } from './agent-tool';

export interface AgentMessage {
  role: 'user' | 'model';
  text: string;
}

export interface AgentStep {
  tool: string;
  args: Record<string, unknown>;
  ok: boolean;
  result: unknown;
}

export interface AgentRunInput {
  systemInstruction: string;
  history?: AgentMessage[];
  message: string;
  tools: AgentTool[];
  maxSteps?: number;
}

export interface AgentRunResult {
  text: string;
  steps: AgentStep[];
  stoppedOnStepLimit: boolean;
}

const DEFAULT_MAX_STEPS = 5;

@Injectable()
export class AgentLoop {
  constructor(private readonly gemini: GeminiClient) {}

  async run({
    systemInstruction,
    history = [],
    message,
    tools,
    maxSteps = DEFAULT_MAX_STEPS,
  }: AgentRunInput): Promise<AgentRunResult> {
    const contents: Content[] = [
      ...history.map((entry) => ({ role: entry.role, parts: [{ text: entry.text }] })),
      { role: 'user', parts: [{ text: message }] },
    ];
    const functionDeclarations = toFunctionDeclarations(tools);
    const steps: AgentStep[] = [];

    for (let step = 0; step < maxSteps; step++) {
      const turn = await this.gemini.generateWithTools({
        systemInstruction,
        contents,
        functionDeclarations,
        // Force at least one tool call on the very first turn of a brand-new
        // conversation, so the model can't skip grounding itself in the
        // user's real persona/habit data before its first reply.
        forceToolCall: step === 0 && history.length === 0,
      });

      if (!turn.toolCalls.length) {
        return { text: turn.text, steps, stoppedOnStepLimit: false };
      }

      contents.push(this.modelTurn(turn.text, turn.toolCalls));

      const responses: Part[] = [];
      for (const call of turn.toolCalls) {
        const outcome = await this.invoke(tools, call);
        steps.push({ tool: call.name, args: call.args, ok: outcome.ok, result: outcome.response });
        responses.push({ functionResponse: { name: call.name, response: outcome.response } });
      }
      contents.push({ role: 'user', parts: responses });
    }

    logger.warn({ steps: steps.length }, 'agent loop hit the step limit, forcing a final answer');
    const closing = await this.gemini.generateWithTools({
      systemInstruction,
      contents,
      functionDeclarations: [],
    });

    return { text: closing.text, steps, stoppedOnStepLimit: true };
  }

  private modelTurn(text: string, toolCalls: ToolCallRequest[]): Content {
    const parts: Part[] = toolCalls.map((call) => ({
      functionCall: { name: call.name, args: call.args },
      ...(call.thoughtSignature ? { thoughtSignature: call.thoughtSignature } : {}),
    }));
    return { role: 'model', parts: text ? [{ text }, ...parts] : parts };
  }

  private async invoke(
    tools: AgentTool[],
    call: ToolCallRequest,
  ): Promise<{ ok: boolean; response: Record<string, unknown> }> {
    const tool = tools.find((candidate) => candidate.name === call.name);
    if (!tool) {
      return { ok: false, response: { error: `Unknown tool "${call.name}".` } };
    }

    try {
      return { ok: true, response: { output: await tool.run(call.args) } };
    } catch (error) {
      if (error instanceof ToolRejectedError) {
        return { ok: false, response: { rejected: error.message } };
      }
      if (error instanceof InvalidToolArgumentsError) {
        return { ok: false, response: { error: `Invalid arguments: ${error.message}` } };
      }
      logger.warn({ tool: call.name, err: error }, 'agent tool threw');
      const reason = error instanceof Error ? error.message : 'unknown failure';
      return { ok: false, response: { error: `Tool failed: ${reason}` } };
    }
  }
}

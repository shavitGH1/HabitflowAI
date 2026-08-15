import { FunctionDeclaration, Schema } from '@google/genai';
import { ZodType } from 'zod';

export interface AgentTool {
  readonly name: string;
  readonly description: string;
  readonly parameters: Schema;
  run(rawArgs: Record<string, unknown>): Promise<unknown>;
}

export interface ToolDefinition<TArgs> {
  name: string;
  description: string;
  parameters: Schema;
  argsSchema: ZodType<TArgs>;
  execute: (args: TArgs) => Promise<unknown>;
}

export class InvalidToolArgumentsError extends Error {}

/** Thrown when a tool deliberately refuses; the message is shown to the model verbatim. */
export class ToolRejectedError extends Error {}

export const defineTool = <TArgs>({
  name,
  description,
  parameters,
  argsSchema,
  execute,
}: ToolDefinition<TArgs>): AgentTool => ({
  name,
  description,
  parameters,
  run: async (rawArgs) => {
    const parsed = argsSchema.safeParse(rawArgs);
    if (!parsed.success) {
      throw new InvalidToolArgumentsError(parsed.error.issues.map((i) => i.message).join('; '));
    }
    return execute(parsed.data);
  },
});

export const toFunctionDeclarations = (tools: AgentTool[]): FunctionDeclaration[] =>
  tools.map(({ name, description, parameters }) => ({ name, description, parameters }));

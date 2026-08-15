import { Type } from '@google/genai';
import { z } from 'zod';
import { AgentLoop } from './agent-loop';
import { defineTool } from './agent-tool';
import { GeminiClient } from '../gemini.client';

const SYSTEM = 'you are a test agent';

const makeGemini = () => ({ generateWithTools: jest.fn() });

const echoTool = (execute = jest.fn().mockResolvedValue({ value: 42 })) =>
  defineTool({
    name: 'read_value',
    description: 'reads a value',
    parameters: { type: Type.OBJECT, properties: { key: { type: Type.STRING } }, required: ['key'] },
    argsSchema: z.object({ key: z.string() }),
    execute,
  });

describe('AgentLoop', () => {
  it('returns the model text without touching tools when no tool call is made', async () => {
    const gemini = makeGemini();
    gemini.generateWithTools.mockResolvedValue({ text: 'hello', toolCalls: [] });
    const execute = jest.fn();

    const result = await new AgentLoop(gemini as unknown as GeminiClient).run({
      systemInstruction: SYSTEM,
      message: 'hi',
      tools: [echoTool(execute)],
    });

    expect(result).toEqual({ text: 'hello', steps: [], stoppedOnStepLimit: false });
    expect(execute).not.toHaveBeenCalled();
  });

  it('runs a tool, feeds the result back and answers on the next turn', async () => {
    const gemini = makeGemini();
    gemini.generateWithTools
      .mockResolvedValueOnce({ text: '', toolCalls: [{ name: 'read_value', args: { key: 'streak' } }] })
      .mockResolvedValueOnce({ text: 'your streak is 42', toolCalls: [] });
    const execute = jest.fn().mockResolvedValue({ value: 42 });

    const result = await new AgentLoop(gemini as unknown as GeminiClient).run({
      systemInstruction: SYSTEM,
      message: 'what is my streak?',
      tools: [echoTool(execute)],
    });

    expect(execute).toHaveBeenCalledWith({ key: 'streak' });
    expect(result.text).toBe('your streak is 42');
    expect(result.steps).toEqual([
      { tool: 'read_value', args: { key: 'streak' }, ok: true, result: { output: { value: 42 } } },
    ]);

    const secondCall = gemini.generateWithTools.mock.calls[1][0];
    expect(secondCall.contents).toHaveLength(3);
    expect(secondCall.contents[1].parts[0].functionCall).toEqual({
      name: 'read_value',
      args: { key: 'streak' },
    });
    expect(secondCall.contents[2].parts[0].functionResponse).toEqual({
      name: 'read_value',
      response: { output: { value: 42 } },
    });
  });

  it('hands invalid tool arguments back to the model instead of throwing', async () => {
    const gemini = makeGemini();
    gemini.generateWithTools
      .mockResolvedValueOnce({ text: '', toolCalls: [{ name: 'read_value', args: {} }] })
      .mockResolvedValueOnce({ text: 'sorry, retrying', toolCalls: [] });
    const execute = jest.fn();

    const result = await new AgentLoop(gemini as unknown as GeminiClient).run({
      systemInstruction: SYSTEM,
      message: 'go',
      tools: [echoTool(execute)],
    });

    expect(execute).not.toHaveBeenCalled();
    expect(result.steps[0].ok).toBe(false);
    expect(result.steps[0].result).toEqual({ error: expect.stringContaining('Invalid arguments') });
  });

  it('reports an unknown tool back to the model', async () => {
    const gemini = makeGemini();
    gemini.generateWithTools
      .mockResolvedValueOnce({ text: '', toolCalls: [{ name: 'delete_everything', args: {} }] })
      .mockResolvedValueOnce({ text: 'I cannot do that', toolCalls: [] });

    const result = await new AgentLoop(gemini as unknown as GeminiClient).run({
      systemInstruction: SYSTEM,
      message: 'go',
      tools: [echoTool()],
    });

    expect(result.steps[0]).toEqual({
      tool: 'delete_everything',
      args: {},
      ok: false,
      result: { error: 'Unknown tool "delete_everything".' },
    });
  });

  it('converts a failing tool into an error result the model can recover from', async () => {
    const gemini = makeGemini();
    gemini.generateWithTools
      .mockResolvedValueOnce({ text: '', toolCalls: [{ name: 'read_value', args: { key: 'x' } }] })
      .mockResolvedValueOnce({ text: 'could not read that', toolCalls: [] });

    const result = await new AgentLoop(gemini as unknown as GeminiClient).run({
      systemInstruction: SYSTEM,
      message: 'go',
      tools: [echoTool(jest.fn().mockRejectedValue(new Error('db down')))],
    });

    expect(result.steps[0].result).toEqual({ error: 'Tool failed: db down' });
    expect(result.text).toBe('could not read that');
  });

  it('stops at the step limit and forces a final answer with no tools available', async () => {
    const gemini = makeGemini();
    gemini.generateWithTools
      .mockResolvedValueOnce({ text: '', toolCalls: [{ name: 'read_value', args: { key: 'a' } }] })
      .mockResolvedValueOnce({ text: '', toolCalls: [{ name: 'read_value', args: { key: 'b' } }] })
      .mockResolvedValueOnce({ text: 'here is what I found', toolCalls: [] });

    const result = await new AgentLoop(gemini as unknown as GeminiClient).run({
      systemInstruction: SYSTEM,
      message: 'go',
      tools: [echoTool()],
      maxSteps: 2,
    });

    expect(result.stoppedOnStepLimit).toBe(true);
    expect(result.steps).toHaveLength(2);
    expect(gemini.generateWithTools.mock.calls[2][0].functionDeclarations).toEqual([]);
  });

  it('sends prior chat turns as history before the new message', async () => {
    const gemini = makeGemini();
    gemini.generateWithTools.mockResolvedValue({ text: 'ok', toolCalls: [] });

    await new AgentLoop(gemini as unknown as GeminiClient).run({
      systemInstruction: SYSTEM,
      history: [
        { role: 'user', text: 'I missed yesterday' },
        { role: 'model', text: 'that happens' },
      ],
      message: 'what now?',
      tools: [echoTool()],
    });

    expect(gemini.generateWithTools.mock.calls[0][0].contents).toEqual([
      { role: 'user', parts: [{ text: 'I missed yesterday' }] },
      { role: 'model', parts: [{ text: 'that happens' }] },
      { role: 'user', parts: [{ text: 'what now?' }] },
    ]);
  });
});

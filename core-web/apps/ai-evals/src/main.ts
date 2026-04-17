import 'dotenv/config';

import { anthropic } from '@ai-sdk/anthropic';
import { type LanguageModel, generateText, stepCountIs, tool } from 'ai';
import { z } from 'zod/v4';

import { createApiAdapter, createExecutor, getSpec } from '@dotcms/agentic-tools';

// ── Types ─────────────────────────────────────────────────────────────────────

export interface Assertion {
    name: string;
    passed: boolean;
}

export interface TaskTrace {
    prompt: string;
    text: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    steps: any[];
    finishReason: string;
    usage: { inputTokens: number | undefined; outputTokens: number | undefined };
    assertions: Assertion[];
    passed: boolean;
}

export interface RunTaskOptions {
    prompt: string;
    model: LanguageModel;
    assertions: (trace: Omit<TaskTrace, 'assertions' | 'passed'>) => Assertion[];
}

// ── Tools ─────────────────────────────────────────────────────────────────────

const DOTCMS_URL = process.env['DOTCMS_URL'];
const AUTH_TOKEN = process.env['AUTH_TOKEN'];

if (!DOTCMS_URL) throw new Error('DOTCMS_URL is required');
if (!AUTH_TOKEN) throw new Error('AUTH_TOKEN is required');

const searchTool = tool({
    description: `Explore the dotCMS REST API spec. Write JavaScript with the \`spec\` global (spec.paths keyed by path string). Return the data you need.`,
    inputSchema: z.object({
        code: z
            .string()
            .max(100_000)
            .describe('JavaScript async function body. Use the `spec` global. Return what you need.')
    }),
    execute: async ({ code }) => {
        const executor = createExecutor();
        const spec = getSpec();
        const result = await executor.execute(code, {
            variables: { spec },
            sandbox: { timeout: 10000 }
        });
        if (!result.success) return `Error: ${result.error?.name}: ${result.error?.message}`;
        return typeof result.value === 'string'
            ? result.value
            : JSON.stringify(result.value, null, 2);
    }
});

const executeTool = tool({
    description: `Execute authenticated calls against the dotCMS REST API. Use api.request({ method, path, query, body }). Always call search first to find the right endpoint.`,
    inputSchema: z.object({
        code: z
            .string()
            .max(100_000)
            .describe(
                'JavaScript async function body. Use `api.request({ method, path, query, body })`. Return the result.'
            )
    }),
    execute: async ({ code }) => {
        const executor = createExecutor();
        const apiAdapter = createApiAdapter({ dotcmsUrl: DOTCMS_URL!, authToken: AUTH_TOKEN! });
        executor.registerAdapter(apiAdapter);
        const result = await executor.execute(code, {
            sandbox: { timeout: 15000 },
            adapters: ['api']
        });
        if (!result.success) return `Error: ${result.error?.name}: ${result.error?.message}`;
        return typeof result.value === 'string'
            ? result.value
            : JSON.stringify(result.value, null, 2);
    }
});

// ── Harness ───────────────────────────────────────────────────────────────────

export async function runTask({ prompt, model, assertions }: RunTaskOptions): Promise<TaskTrace> {
    const { text, steps, finishReason, usage } = await generateText({
        model,
        prompt,
        tools: { search: searchTool, execute: executeTool },
        stopWhen: stepCountIs(10)
    });

    const base = { prompt, text, steps, finishReason, usage };
    const evaluated = assertions(base);
    const passed = evaluated.every((a) => a.passed);

    return { ...base, assertions: evaluated, passed };
}

function printTrace(trace: TaskTrace): void {
    console.log(`\nPrompt: ${trace.prompt}`);

    for (const [i, step] of trace.steps.entries()) {
        const calls = step.toolCalls?.map((c) => c.toolName).join(', ') ?? '—';
        console.log(`  step ${i + 1}: [${calls}]`);
    }

    console.log(`\nSteps: ${trace.steps.length}, Finish: ${trace.finishReason}`);
    console.log(`Tokens: ${trace.usage.inputTokens} in / ${trace.usage.outputTokens} out`);
    console.log('\nFinal text:\n', trace.text);

    console.log('\n=== Assertions ===');
    for (const { name, passed } of trace.assertions) {
        console.log(`[${passed ? 'PASS' : 'FAIL'}] ${name}`);
    }
}

// ── Smoke test ────────────────────────────────────────────────────────────────

async function main() {
    console.log('=== dotCMS ai-evals smoke test ===');

    const trace = await runTask({
        model: anthropic('claude-sonnet-4-5-20250929'),
        prompt: 'List the content types in this dotCMS instance. Just their names and variable names.',
        assertions: ({ text, steps }) => {
            const allCalls: Array<{ toolName: string }> = steps.flatMap((s) => s.toolCalls ?? []);
            return [
                { name: '≥1 search call', passed: allCalls.some((c) => c.toolName === 'search') },
                {
                    name: '≥1 execute call',
                    passed: allCalls.some((c) => c.toolName === 'execute')
                },
                { name: 'non-empty final text', passed: text.trim().length > 0 }
            ];
        }
    });

    printTrace(trace);

    if (!trace.passed) process.exit(1);
    console.log('\nSmoke test passed.');
}

main().catch((e) => {
    console.error('Fatal:', e);
    process.exit(1);
});

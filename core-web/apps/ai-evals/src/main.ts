import 'dotenv/config';

import { anthropic } from '@ai-sdk/anthropic';
import { generateText, stepCountIs, tool } from 'ai';
import { z } from 'zod/v4';

import { createApiAdapter, createExecutor, getSpec } from '@dotcms/agentic-tools';

const DOTCMS_URL = process.env['DOTCMS_URL'];
const AUTH_TOKEN = process.env['AUTH_TOKEN'];

if (!DOTCMS_URL) throw new Error('DOTCMS_URL is required');
if (!AUTH_TOKEN) throw new Error('AUTH_TOKEN is required');

const searchTool = tool({
    description: `Explore the dotCMS REST API spec. Write JavaScript with the \`spec\` global (spec.paths keyed by path string). Return the data you need.`,
    inputSchema: z.object({
        code: z.string().max(100_000).describe('JavaScript async function body. Use the `spec` global. Return what you need.')
    }),
    execute: async ({ code }) => {
        const executor = createExecutor();
        const spec = getSpec();
        const result = await executor.execute(code, { variables: { spec }, sandbox: { timeout: 10000 } });
        if (!result.success) return `Error: ${result.error?.name}: ${result.error?.message}`;
        return typeof result.value === 'string' ? result.value : JSON.stringify(result.value, null, 2);
    }
});

const executeTool = tool({
    description: `Execute authenticated calls against the dotCMS REST API. Use api.request({ method, path, query, body }). Always call search first to find the right endpoint.`,
    inputSchema: z.object({
        code: z.string().max(100_000).describe('JavaScript async function body. Use `api.request({ method, path, query, body })`. Return the result.')
    }),
    execute: async ({ code }) => {
        const executor = createExecutor();
        const apiAdapter = createApiAdapter({ dotcmsUrl: DOTCMS_URL!, authToken: AUTH_TOKEN! });
        executor.registerAdapter(apiAdapter);
        const result = await executor.execute(code, { sandbox: { timeout: 15000 }, adapters: ['api'] });
        if (!result.success) return `Error: ${result.error?.name}: ${result.error?.message}`;
        return typeof result.value === 'string' ? result.value : JSON.stringify(result.value, null, 2);
    }
});

async function main() {
    console.log('=== dotCMS ai-evals smoke test ===\n');

    const { text, steps, finishReason, usage } = await generateText({
        model: anthropic('claude-sonnet-4-5-20250929'),
        prompt: 'List the content types in this dotCMS instance. Just their names and variable names.',
        tools: { search: searchTool, execute: executeTool },
        stopWhen: stepCountIs(10)
    });

    console.log(`Steps: ${steps.length}, Finish: ${finishReason}`);
    console.log(`Tokens: ${usage.inputTokens} in / ${usage.outputTokens} out`);
    console.log('\nFinal text:\n', text);

    const allCalls = steps.flatMap((s) => s.toolCalls ?? []);
    const assertions = [
        { name: '≥1 search call', passed: allCalls.some((c) => c.toolName === 'search') },
        { name: '≥1 execute call', passed: allCalls.some((c) => c.toolName === 'execute') },
        { name: 'non-empty final text', passed: text.trim().length > 0 }
    ];

    console.log('\n=== Assertions ===');
    let failed = false;
    for (const { name, passed } of assertions) {
        console.log(`[${passed ? 'PASS' : 'FAIL'}] ${name}`);
        if (!passed) failed = true;
    }

    if (failed) process.exit(1);
    console.log('\nSmoke test passed.');
}

main().catch((e) => {
    console.error('Fatal:', e);
    process.exit(1);
});

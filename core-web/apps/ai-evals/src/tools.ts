import { tool } from 'ai';
import { z } from 'zod/v4';

import { createApiAdapter, createExecutor, getSpec } from '@dotcms/agentic-tools';

function sandboxResult(
    result: Awaited<ReturnType<ReturnType<typeof createExecutor>['execute']>>
): string {
    if (!result.success) return `Error: ${result.error?.name}: ${result.error?.message}`;
    return typeof result.value === 'string' ? result.value : JSON.stringify(result.value, null, 2);
}

export function makeTools(dotcmsUrl: string, authToken: string) {
    // nosemgrep: detect-vercelai -- internal LLM eval harness (ai-evals), not shipped runtime code; Vercel AI SDK usage is intentional
    const searchTool = tool({
        description: `Explore the dotCMS REST API spec. Write JavaScript with the \`spec\` global (spec.paths keyed by path string). Return the data you need.`,
        inputSchema: z.object({
            code: z
                .string()
                .max(100_000)
                .describe(
                    'JavaScript async function body. Use the `spec` global. Return what you need.'
                )
        }),
        execute: async ({ code }) => {
            const executor = createExecutor();
            const spec = getSpec();
            const result = await executor.execute(code, {
                variables: { spec },
                sandbox: { timeout: 10000 }
            });
            return sandboxResult(result);
        }
    });

    // nosemgrep: detect-vercelai -- internal LLM eval harness (ai-evals), not shipped runtime code; Vercel AI SDK usage is intentional
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
            const apiAdapter = createApiAdapter({ dotcmsUrl, authToken });
            executor.registerAdapter(apiAdapter);
            const result = await executor.execute(code, {
                sandbox: { timeout: 15000 },
                adapters: ['api']
            });
            return sandboxResult(result);
        }
    });

    return { search: searchTool, execute: executeTool };
}

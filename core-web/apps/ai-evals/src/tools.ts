import { tool } from 'ai';
import { z } from 'zod/v4';

import { createApiAdapter } from '@dotcms/ai/adapter';
import { createExecutor, formatSandboxResult } from '@dotcms/ai/sandbox';
import { getSpec } from '@dotcms/ai/spec';
import {
    createContentTypeTool,
    dotcmsConnection,
    executeTool,
    lookupTool,
    pageCreateTool,
    pagePlaceContentTool,
    pageVerifyTool,
    searchTool,
    updateContentTypeTool,
    upsertContentTool
} from '@dotcms/ai/tools';

export function makeTools(dotcmsUrl: string, authToken: string) {
    // nosemgrep: detect-vercelai -- internal LLM eval harness (ai-evals), not shipped runtime code; Vercel AI SDK usage is intentional
    const searchTool = tool({
        description: `Explore the dotCMS REST API spec. Write JavaScript with the \`spec\` global (\`spec.paths\` + \`spec.components.schemas\`, \`$ref\`-based). Schemas in requestBody/responses are usually \`$ref\`s — call \`resolveRef(schemaOrName, depth)\` to expand them. Return the data you need.`,
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
            return formatSandboxResult(result);
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
            return formatSandboxResult(result);
        }
    });

    return { search: searchTool, execute: executeTool };
}

/**
 * Which tools a run gives the model.
 *  - `code`: `search` + `execute` only. The model finds and calls the REST endpoints itself.
 *  - `tools`: the `@dotcms/ai/tools` set the MCP server exposes (purpose-built tools next to
 *    `search` + `execute`). Asset tools are left out: they read and write the local disk.
 *
 * Running the same task with both measures what the purpose-built tools change.
 */
export type Toolset = 'code' | 'tools';

export const TOOLSETS: readonly Toolset[] = ['code', 'tools'];

export function makeToolset(toolset: Toolset, dotcmsUrl: string, authToken: string) {
    if (toolset === 'code') return makeTools(dotcmsUrl, authToken);

    const dotcms = dotcmsConnection({ url: dotcmsUrl, token: authToken });
    return {
        search: searchTool(dotcms),
        execute: executeTool(dotcms),
        page_create: pageCreateTool(dotcms),
        page_place_content: pagePlaceContentTool(dotcms),
        page_verify: pageVerifyTool(dotcms),
        lookup: lookupTool(dotcms),
        upsert_content: upsertContentTool(dotcms),
        create_content_type: createContentTypeTool(dotcms),
        update_content_type: updateContentTypeTool(dotcms)
    };
}

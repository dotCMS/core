import { type ToolMetadata } from 'xmcp';

import type { DotCMSTool, ExecuteToolOptions } from '@dotcms/ai/tools';

/** Cap on a context-load error echoed to stderr — a dotCMS 5xx body is a full HTML page. */
const MAX_LOGGED_ERROR_CHARS = 2_000;

/**
 * The options every tool on this server is built with.
 *
 * No `url` or `token`: the tools fall back to `DOTCMS_URL` / `AUTH_TOKEN` themselves, reading
 * them on each call — so a server started without credentials still boots, lists its tools,
 * and answers each call with a readable CONFIGURATION failure.
 */
const SERVER_OPTIONS: ExecuteToolOptions = {
    timeout: Number(process.env.SANDBOX_TIMEOUT) || undefined,
    onContextError: (label, error) => {
        const message = error instanceof Error ? error.message : String(error);
        console.error(
            `[context] failed to load ${label}: ${message.slice(0, MAX_LOGGED_ERROR_CHARS)}`
        );
    }
};

/**
 * Adapt one `@dotcms/ai/tools` factory to the three exports xmcp discovers in `src/tools/`:
 * `schema` (a raw Zod shape), `metadata`, and the default-exported handler.
 */
export function xmcpTool(factory: (options: ExecuteToolOptions) => DotCMSTool) {
    const tool = factory(SERVER_OPTIONS);

    const metadata: ToolMetadata = {
        name: tool.name,
        description: tool.description,
        annotations: { title: tool.title, ...tool.annotations }
    };

    return {
        schema: tool.inputSchema.shape,
        metadata,
        // MCP hands the model text: `execute`/`search` already return it, and a manifest or a
        // failure is sent as the same pretty-printed JSON this server has always returned.
        handler: async (args: unknown): Promise<string> => {
            const result = await tool.execute(args);

            return typeof result === 'string' ? result : JSON.stringify(result, null, 2);
        }
    };
}

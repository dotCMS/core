import { type ToolMetadata } from 'xmcp';

import {
    dotcmsConnection,
    toolResultText,
    type DotCMSConnection,
    type DotCMSTool,
    type ExecuteToolOptions,
    type RequestToolOptions
} from '@dotcms/ai/tools';

/** Every tool option this server may pass; each factory reads only its own. */
type ServerToolOptions = ExecuteToolOptions & RequestToolOptions;

/** Cap on a context-load error echoed to stderr — a dotCMS 5xx body is a full HTML page. */
const MAX_LOGGED_ERROR_CHARS = 2_000;

/**
 * The connection every tool on this server uses — the ONE place that reads `DOTCMS_URL` and
 * `AUTH_TOKEN`, the variables this server's MCP client config has always set.
 *
 * Resolvers rather than values: the tools read them on each call, so a server started without
 * credentials still boots, lists its tools, and answers each call with a readable
 * CONFIGURATION failure instead of crashing.
 */
const DOTCMS: DotCMSConnection = dotcmsConnection({
    url: () => process.env.DOTCMS_URL,
    token: () => process.env.AUTH_TOKEN,
    onContextError: (label, error) => {
        const message = error instanceof Error ? error.message : String(error);
        console.error(
            `[context] failed to load ${label}: ${message.slice(0, MAX_LOGGED_ERROR_CHARS)}`
        );
    }
});

/** Tool options this server sets. Only `execute` reads `timeout`; the others ignore it. */
const SERVER_OPTIONS: ServerToolOptions = {
    timeout: Number(process.env.SANDBOX_TIMEOUT) || undefined
};

/**
 * Adapt one `@dotcms/ai/tools` factory to the three exports xmcp discovers in `src/tools/`:
 * `schema` (a raw Zod shape), `metadata`, and the default-exported handler.
 */
export function xmcpTool(
    factory: (connection: DotCMSConnection, options: ServerToolOptions) => DotCMSTool
) {
    const tool = factory(DOTCMS, SERVER_OPTIONS);

    const metadata: ToolMetadata = {
        name: tool.name,
        description: tool.description,
        annotations: { title: tool.title, ...tool.annotations }
    };

    return {
        schema: tool.inputSchema.shape,
        metadata,
        // MCP hands the model text: a code tool's output as-is, and a manifest or a failure as
        // the same pretty-printed JSON this server has always returned.
        handler: async (args: unknown): Promise<string> => toolResultText(await tool.execute(args))
    };
}

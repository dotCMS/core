import { type ToolMetadata } from 'xmcp';

import {
    dotcmsConnection,
    type AssetToolOptions,
    type DotCMSConnection,
    type DotCMSTool,
    type ExecuteToolOptions
} from '@dotcms/ai/tools';

/** Every tool option this server may pass; each factory reads only its own. */
type ServerToolOptions = ExecuteToolOptions & AssetToolOptions;

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

/** Tool options this server sets; each factory reads only its own. */
const SERVER_OPTIONS: ServerToolOptions = {
    // `execute`'s sandbox timeout.
    timeout: Number(process.env.SANDBOX_TIMEOUT) || undefined,
    // The asset tools' filesystem boundary. `/` — the whole disk — deliberately: this is a
    // local stdio server, the model acts as the user who started it, and moving a theme in or
    // out of any directory they name is the feature. A hosted server would set a workspace.
    root: '/'
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
        handler: async (args: unknown): Promise<string> => tool.toText(await tool.execute(args))
    };
}

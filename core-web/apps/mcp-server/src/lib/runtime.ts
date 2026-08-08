import { createRuntime } from '@dotcms/ai/runtime';

type DotCMSRuntime = ReturnType<typeof createRuntime>;

/**
 * Build a runtime from the MCP server's environment. One place owns the `DOTCMS_URL` /
 * `AUTH_TOKEN` reading, the default session id, and the standard context-error logging — so
 * every tool (`execute`, `search`, `download_assets`, `upload_assets`) constructs the runtime
 * the same way instead of re-deriving it (and silently drifting on which options they set).
 */
export function runtimeFromEnv(
    sessionId?: string,
    opts?: { timeout?: number; includeSpec?: boolean }
): DotCMSRuntime {
    return createRuntime({
        url: process.env.DOTCMS_URL ?? '',
        token: process.env.AUTH_TOKEN ?? '',
        sessionId: sessionId ?? '__default__',
        timeout: opts?.timeout,
        includeSpec: opts?.includeSpec,
        onContextError: (label, error) => {
            console.error(`[context] failed to load ${label}: ${errorMessage(error)}`);
        }
    });
}

/** Normalize any thrown value to a message string. */
export function errorMessage(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
}
